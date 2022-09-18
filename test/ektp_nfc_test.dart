import 'package:flutter_test/flutter_test.dart';
import 'package:ektp_nfc/ektp_nfc.dart';
import 'package:ektp_nfc/ektp_nfc_platform_interface.dart';
import 'package:ektp_nfc/ektp_nfc_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockEktpNfcPlatform 
    with MockPlatformInterfaceMixin
    implements EktpNfcPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final EktpNfcPlatform initialPlatform = EktpNfcPlatform.instance;

  test('$MethodChannelEktpNfc is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelEktpNfc>());
  });

  test('getPlatformVersion', () async {
    EktpNfc ektpNfcPlugin = EktpNfc();
    MockEktpNfcPlatform fakePlatform = MockEktpNfcPlatform();
    EktpNfcPlatform.instance = fakePlatform;
  
    expect(await ektpNfcPlugin.getPlatformVersion(), '42');
  });
}
