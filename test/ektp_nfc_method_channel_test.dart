import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:ektp_nfc/ektp_nfc_method_channel.dart';

void main() {
  MethodChannelEktpNfc platform = MethodChannelEktpNfc();
  const MethodChannel channel = MethodChannel('ektp_nfc');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    expect(await platform.getPlatformVersion(), '42');
  });
}
