package suzaku.szk.ektp_reader.ektp_nfc_example

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugins.GeneratedPluginRegistrant
import suzaku.szk.ektp_reader.ektp_nfc.EktpNfcPlugin


class MainActivity: FlutterActivity() {
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        GeneratedPluginRegistrant.registerWith(flutterEngine)
        val channel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "ektp_nfc")
        channel.setMethodCallHandler(EktpNfcPlugin())
    }
}
