import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'surface_fix_method_channel.dart';

abstract class SurfaceFixPlatform extends PlatformInterface {
  /// Constructs a SurfaceFixPlatform.
  SurfaceFixPlatform() : super(token: _token);

  static final Object _token = Object();

  static SurfaceFixPlatform _instance = MethodChannelSurfaceFix();

  /// The default instance of [SurfaceFixPlatform] to use.
  ///
  /// Defaults to [MethodChannelSurfaceFix].
  static SurfaceFixPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [SurfaceFixPlatform] when
  /// they register themselves.
  static set instance(SurfaceFixPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
