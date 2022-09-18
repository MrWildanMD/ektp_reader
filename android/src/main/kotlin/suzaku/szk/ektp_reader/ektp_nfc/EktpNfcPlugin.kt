package suzaku.szk.ektp_reader.ektp_nfc

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.AsyncTask
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.*


/** EktpNfcPlugin */
class EktpNfcPlugin: FlutterPlugin, MethodCallHandler, ActivityAware {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var channel : MethodChannel
  private lateinit var nfc : NfcAdapter
  private lateinit var messenger: BinaryMessenger
  private lateinit var activity : Activity
  private lateinit var mDecodeThreadPool: ThreadPoolExecutor

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    val mDecodeWorkQueue: BlockingQueue<Runnable> = LinkedBlockingQueue<Runnable>()

    val KEEP_ALIVE_TIME = 1L
    val KEEP_ALIVE_TIMEUNIT = TimeUnit.SECONDS

    mDecodeThreadPool = ThreadPoolExecutor(
      1,
      1,
      KEEP_ALIVE_TIME,
      KEEP_ALIVE_TIMEUNIT,
      mDecodeWorkQueue,
    )

    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "ektp_nfc")
    channel.setMethodCallHandler(this)
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    nfc = NfcAdapter.getDefaultAdapter(activity)

    if (nfc == null) {
      result.error("404", "This device didn't support NFC!", null)
      return
    }

    if (!nfc.isEnabled) {
      result.error("401", "NFC is not enabled!", null)
      return
    }

    if (call.method == "getEKTPPhoto") {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        nfc.enableReaderMode(activity, NfcAdapter.ReaderCallback {
          val task = GetEKTPPhoto(messenger, it)
          task.executeOnExecutor(mDecodeThreadPool)
        }, 1, null)
      }
      result.success(true)
    } else {
      result.notImplemented()
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    activity = binding.activity
  }

  override fun onDetachedFromActivityForConfigChanges() {

  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {

  }

  override fun onDetachedFromActivity() {

  }

  class GetEKTPPhoto(var messenger: BinaryMessenger, var tag: Tag) :
    AsyncTask<String?, Void?, Void?>() {
    var buffer: ByteBuffer? = null

    fun hexString2ByteArray(str: String): ByteArray? {
      //convert hexString to ByteArray
      val length = str.length
      val bArr = ByteArray(length / 2)
      var i = 0
      while (i < length) {
        bArr[i / 2] = ((Character.digit(str[i], 16) shl 4) + Character.digit(str[i + 1], 16)).toByte()
        i += 2
      }
      return bArr
    }

    fun byte2Hex(bArr: ByteArray?, str: String): String? {
      if (bArr == null || bArr.size < 1) {
        return ""
      }
      val sb = StringBuilder()
      for (valueOf in bArr) {
        sb.append(String.format("%02X$str", *arrayOf<Any>(java.lang.Byte.valueOf(valueOf))))
      }
      return sb.toString().trim { it <= ' ' }
    }

    fun int2Hex(i: Int, i2: Int, str: String): String? {
      val wrap = ByteBuffer.wrap(ByteArray(4))
      wrap.putInt(i)
      return byte2Hex(Arrays.copyOfRange(wrap.array(), 4 - i2 / 2, 4), str)
    }

    override fun doInBackground(vararg strings: String?): Void? {
      try {
        var res = ""
        var bytesArray: ByteArray? = null
        var hexStringToByteArray: ByteArray?
        val isoDep = IsoDep.get(tag)
        isoDep.connect()
        //transcieve to get Photo in EKTP
        isoDep.transceive(hexString2ByteArray("00A40000027F0A00"))
        isoDep.transceive(hexString2ByteArray("00A40000026FF2"))
        val transceive = isoDep.transceive(hexString2ByteArray("00B0000008"))
        val parseInt: Int = byte2Hex(Arrays.copyOfRange(transceive, 0, 2), "")!!.toInt(16) + 2
        val byteArrayOutputStream = ByteArrayOutputStream()
        byteArrayOutputStream.write(transceive, 2, transceive.size - 2)
        var i = 8
        while (i < parseInt) {
          val i2 = i + 112
          if (i2 > parseInt) {
            hexStringToByteArray =
              hexString2ByteArray("00B0" + int2Hex(i, 4, "") + int2Hex(parseInt - i, 2, ""))
          } else {
            hexStringToByteArray =
              hexString2ByteArray("00B0" + int2Hex(i, 4, "") + int2Hex(112, 2, ""))
          }
          val transceive2 = isoDep.transceive(hexStringToByteArray)
          byteArrayOutputStream.write(transceive2, 0, transceive2.size - 2)
          i = i2
        }

        // create base64 string of ektp photo
        res = Base64.encodeToString(byteArrayOutputStream.toByteArray(), 0).replace("\n", "")
          .replace("\r", "")

        //decode from base64 ,save it to bitmap and get bytesArray
        val decodedString = Base64.decode(res, Base64.DEFAULT)
        val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
        val bitmapStream = ByteArrayOutputStream()
        decodedByte.compress(Bitmap.CompressFormat.JPEG, 100, bitmapStream)
        bytesArray = bitmapStream.toByteArray()
        buffer = ByteBuffer.allocateDirect(bytesArray.size)
        if (bytesArray != null) {
          buffer!!.put(bytesArray)
        }
        isoDep.close()
      } catch (ex: Exception) {
        Log.d("error", ex.cause.toString())
      }
      return null
    }

    override fun onPostExecute(aVoid: Void?) {
      try {
        //send buffer of image to flutter
        messenger.send("getEKTPPhoto", buffer)
      } catch (ex: Exception) {
        Log.d("error", ex.cause.toString())
      }
      super.onPostExecute(aVoid)
    }
  }
}
