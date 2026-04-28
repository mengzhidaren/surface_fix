
import 'surface_fix_platform_interface.dart';

class SurfaceFix {
  Future<String?> getPlatformVersion() {
    return SurfaceFixPlatform.instance.getPlatformVersion();
  }
}
