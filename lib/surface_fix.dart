import 'package:flutter/foundation.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';

import 'surface_fix_platform_interface.dart';

class SurfaceFix {
  static const MethodChannel _channel = MethodChannel('surface_fix');

  Future<String?> getPlatformVersion() {
    return SurfaceFixPlatform.instance.getPlatformVersion();
  }

  static Future<int> createSurfaceProducer(int width, int height) async {
    final textureId = await _channel.invokeMethod<int>(
      'createSurfaceProducer',
      {'width': width, 'height': height},
    );
    return textureId ?? -1;
  }

  static Future<void> disposeSurfaceProducer() async {
    await _channel.invokeMethod<void>('disposeSurfaceProducer');
  }
}

/// Mode 1: SurfaceView (PlatformView with Surface)
class TriangleSurfaceWidget extends StatelessWidget {
  const TriangleSurfaceWidget({super.key});

  static const String viewType = 'surface_fix/triangle_surface';

  @override
  Widget build(BuildContext context) {
    if (defaultTargetPlatform == TargetPlatform.android) {
      return PlatformViewLink(
        viewType: viewType,
        surfaceFactory: (context, controller) {
          return AndroidViewSurface(
            controller: controller as AndroidViewController,
            gestureRecognizers: const <Factory<OneSequenceGestureRecognizer>>{},
            hitTestBehavior: PlatformViewHitTestBehavior.opaque,
          );
        },
        onCreatePlatformView: (params) {
          return PlatformViewsService.initSurfaceAndroidView(
              id: params.id,
              viewType: viewType,
              layoutDirection: TextDirection.ltr,
            )
            ..addOnPlatformViewCreatedListener(params.onPlatformViewCreated)
            ..create();
        },
      );
    }
    return const Center(child: Text('Platform not supported'));
  }
}

/// Mode 2: TextureView (PlatformView with TextureView)
class TriangleTextureViewWidget extends StatelessWidget {
  const TriangleTextureViewWidget({super.key});

  static const String viewType = 'surface_fix/triangle_texture_view';

  @override
  Widget build(BuildContext context) {
    if (defaultTargetPlatform == TargetPlatform.android) {
      return PlatformViewLink(
        viewType: viewType,
        surfaceFactory: (context, controller) {
          return AndroidViewSurface(
            controller: controller as AndroidViewController,
            gestureRecognizers: const <Factory<OneSequenceGestureRecognizer>>{},
            hitTestBehavior: PlatformViewHitTestBehavior.opaque,
          );
        },
        onCreatePlatformView: (params) {
          return PlatformViewsService.initExpensiveAndroidView(
              id: params.id,
              viewType: viewType,
              layoutDirection: TextDirection.ltr,
            )
            ..addOnPlatformViewCreatedListener(params.onPlatformViewCreated)
            ..create();
        },
      );
    }
    return const Center(child: Text('Platform not supported'));
  }
}

/// Mode 3: SurfaceProducer (TextureRegistry.SurfaceProducer rendered via Texture widget)
class TriangleSurfaceProducerWidget extends StatefulWidget {
  const TriangleSurfaceProducerWidget({super.key});

  @override
  State<TriangleSurfaceProducerWidget> createState() =>
      _TriangleSurfaceProducerWidgetState();
}

class _TriangleSurfaceProducerWidgetState
    extends State<TriangleSurfaceProducerWidget> {
  int? _textureId;

  @override
  void initState() {
    super.initState();
    _initTexture();
  }

  Future<void> _initTexture() async {
    final id = await SurfaceFix.createSurfaceProducer(640, 480);
    if (mounted) {
      setState(() {
        _textureId = id;
      });
    }
  }

  @override
  void dispose() {
    SurfaceFix.disposeSurfaceProducer();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (_textureId == null || _textureId == -1) {
      return const Center(child: Text('Creating SurfaceProducer...'));
    }
    return Texture(textureId: _textureId!);
  }
}
