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

  static Future<int> createEglSurfaceProducer(int width, int height) async {
    final textureId = await _channel.invokeMethod<int>(
      'createEglSurfaceProducer',
      {'width': width, 'height': height},
    );
    return textureId ?? -1;
  }

  static Future<void> disposeEglSurfaceProducer() async {
    await _channel.invokeMethod<void>('disposeEglSurfaceProducer');
  }

  static Future<int> createSurfaceTexture(int width, int height) async {
    final textureId = await _channel.invokeMethod<int>('createSurfaceTexture', {
      'width': width,
      'height': height,
    });
    return textureId ?? -1;
  }

  static Future<void> disposeSurfaceTexture() async {
    await _channel.invokeMethod<void>('disposeSurfaceTexture');
  }

  static Future<int> createEglSurfaceTexture(int width, int height) async {
    final textureId = await _channel.invokeMethod<int>(
      'createEglSurfaceTexture',
      {'width': width, 'height': height},
    );
    return textureId ?? -1;
  }

  static Future<void> disposeEglSurfaceTexture() async {
    await _channel.invokeMethod<void>('disposeEglSurfaceTexture');
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
// ────────────────────────────────────────────────────────────────
// Mode 4: SurfaceTextureEntry Widgets
// ────────────────────────────────────────────────────────────────

/// Canvas SurfaceTextureEntry — yellow/amber triangle
class TriangleSurfaceTextureWidget extends StatefulWidget {
  const TriangleSurfaceTextureWidget({super.key});

  @override
  State<TriangleSurfaceTextureWidget> createState() =>
      _TriangleSurfaceTextureWidgetState();
}

class _TriangleSurfaceTextureWidgetState
    extends State<TriangleSurfaceTextureWidget> {
  int? _textureId;

  @override
  void initState() {
    super.initState();
    _initTexture();
  }

  Future<void> _initTexture() async {
    final id = await SurfaceFix.createSurfaceTexture(640, 480);
    if (mounted) setState(() => _textureId = id);
  }

  @override
  void dispose() {
    SurfaceFix.disposeSurfaceTexture();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (_textureId == null || _textureId == -1) {
      return const Center(child: Text('Creating SurfaceTexture...'));
    }
    return Texture(textureId: _textureId!);
  }
}

// ────────────────────────────────────────────────────────────────
// EGL Widgets
// ────────────────────────────────────────────────────────────────

/// EGL Mode 1: SurfaceView + EGL (orange triangle)
class EglSurfaceViewWidget extends StatelessWidget {
  const EglSurfaceViewWidget({super.key});

  static const String viewType = 'surface_fix/egl_surface_view';

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

/// EGL Mode 2: TextureView + EGL (purple triangle)
class EglTextureViewWidget extends StatelessWidget {
  const EglTextureViewWidget({super.key});

  static const String viewType = 'surface_fix/egl_texture_view';

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

/// EGL Mode 3: SurfaceProducer + EGL (cyan triangle)
class EglSurfaceProducerWidget extends StatefulWidget {
  const EglSurfaceProducerWidget({super.key});

  @override
  State<EglSurfaceProducerWidget> createState() =>
      _EglSurfaceProducerWidgetState();
}

class _EglSurfaceProducerWidgetState extends State<EglSurfaceProducerWidget> {
  int? _textureId;

  @override
  void initState() {
    super.initState();
    _initTexture();
  }

  Future<void> _initTexture() async {
    final id = await SurfaceFix.createEglSurfaceProducer(640, 480);
    if (mounted) {
      setState(() {
        _textureId = id;
      });
    }
  }

  @override
  void dispose() {
    SurfaceFix.disposeEglSurfaceProducer();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (_textureId == null || _textureId == -1) {
      return const Center(child: Text('Creating EGL SurfaceProducer...'));
    }
    return Texture(textureId: _textureId!);
  }
}

/// EGL SurfaceTextureEntry — lime green triangle
class EglSurfaceTextureWidget extends StatefulWidget {
  const EglSurfaceTextureWidget({super.key});

  @override
  State<EglSurfaceTextureWidget> createState() =>
      _EglSurfaceTextureWidgetState();
}

class _EglSurfaceTextureWidgetState extends State<EglSurfaceTextureWidget> {
  int? _textureId;

  @override
  void initState() {
    super.initState();
    _initTexture();
  }

  Future<void> _initTexture() async {
    final id = await SurfaceFix.createEglSurfaceTexture(640, 480);
    if (mounted) setState(() => _textureId = id);
  }

  @override
  void dispose() {
    SurfaceFix.disposeEglSurfaceTexture();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (_textureId == null || _textureId == -1) {
      return const Center(child: Text('Creating EGL SurfaceTexture...'));
    }
    return Texture(textureId: _textureId!);
  }
}
