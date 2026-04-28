import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'surface_fix_platform_interface.dart';

/// An implementation of [SurfaceFixPlatform] that uses method channels.
class MethodChannelSurfaceFix extends SurfaceFixPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('surface_fix');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>(
      'getPlatformVersion',
    );
    return version;
  }
}
