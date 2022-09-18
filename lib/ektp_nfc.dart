import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:flutter/services.dart';
import 'package:path_provider/path_provider.dart';

typedef ConvertByteDataToBase64 = void Function(String base64);
typedef GetFilePath = void Function(String path);

class EktpNfc {
  static const MethodChannel _channel = MethodChannel('ektp_nfc');

  static Future<dynamic> getEktpPhoto(ConvertByteDataToBase64 callback) async {
    String base64;
    ServicesBinding.instance.defaultBinaryMessenger
        .setMessageHandler('getEKTPPhoto', (ByteData? message) {
      final ByteBuffer buffer = message!.buffer;
      base64 = base64Encode(buffer.asUint8List(message.offsetInBytes, message.lengthInBytes));
      callback(base64);
      return;
    });

    return await _channel.invokeMethod("getEKTPPhoto");
  }

  static Future<dynamic> getFilePathEKTPPhoto(GetFilePath callback,
      {String filename = ".filename01.jpg"}) async {
    ServicesBinding.instance.defaultBinaryMessenger
        .setMessageHandler('getEKTPPhoto', (ByteData? message) async {
      final ByteBuffer buffer = message!.buffer;
      if (buffer != null) {
        String dir = (await getExternalStorageDirectories())![0].path;
        File file = File("$dir/" + filename ?? ".file01.jpg");
        await file.writeAsBytes(
            buffer.asUint8List(message.offsetInBytes, message.lengthInBytes));
        callback(file.path);
      } else {
        callback("error");
      }
      return;
    });

    return await _channel.invokeMethod("getEKTPPhoto");
  }
}