import 'package:flutter_test/flutter_test.dart';
import 'package:surface_fix/surface_fix.dart';
import 'package:surface_fix/surface_fix_platform_interface.dart';
import 'package:surface_fix/surface_fix_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockSurfaceFixPlatform
    with MockPlatformInterfaceMixin
    implements SurfaceFixPlatform {
  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final SurfaceFixPlatform initialPlatform = SurfaceFixPlatform.instance;

  test('$MethodChannelSurfaceFix is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelSurfaceFix>());
  });

  test('getPlatformVersion', () async {
    SurfaceFix surfaceFixPlugin = SurfaceFix();
    MockSurfaceFixPlatform fakePlatform = MockSurfaceFixPlatform();
    SurfaceFixPlatform.instance = fakePlatform;

    expect(await surfaceFixPlugin.getPlatformVersion(), '42');
  });
}
