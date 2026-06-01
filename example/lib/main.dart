import 'package:flutter/material.dart';
import 'package:surface_fix/surface_fix.dart';

void main() {
  runApp(const MyApp());
}

// ── Canvas modes ──────────────────────────────────────────────
enum RenderMode {
  surfaceView('SurfaceView', Colors.red),
  textureView('TextureView', Colors.green),
  surfaceProducer('SurfaceProducer', Colors.blue),
  surfaceTexture('SurfaceTexture', Colors.amber);

  const RenderMode(this.label, this.color);
  final String label;
  final Color color;
}

// ── EGL modes ─────────────────────────────────────────────────
enum EglRenderMode {
  surfaceView('EGL SurfaceView', Colors.orange),
  textureView('EGL TextureView', Colors.purple),
  surfaceProducer('EGL Producer', Colors.cyan),
  surfaceTexture('EGL SurfTex', Colors.lightGreen);

  const EglRenderMode(this.label, this.color);
  final String label;
  final Color color;
}

// ── App ───────────────────────────────────────────────────────
class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  int _tabIndex = 0;
  RenderMode _canvasMode = RenderMode.surfaceView;
  EglRenderMode _eglMode = EglRenderMode.surfaceView;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: Text(_tabIndex == 0 ? 'Canvas Surface Test' : 'EGL Surface Test'),
        ),
        body: IndexedStack(
          index: _tabIndex,
          children: [
            _buildCanvasTab(),
            _buildEglTab(),
          ],
        ),
        bottomNavigationBar: NavigationBar(
          selectedIndex: _tabIndex,
          onDestinationSelected: (i) => setState(() => _tabIndex = i),
          destinations: const [
            NavigationDestination(
              icon: Icon(Icons.brush_outlined),
              selectedIcon: Icon(Icons.brush),
              label: 'Canvas',
            ),
            NavigationDestination(
              icon: Icon(Icons.view_in_ar_outlined),
              selectedIcon: Icon(Icons.view_in_ar),
              label: 'EGL',
            ),
          ],
        ),
      ),
    );
  }

  // ── Canvas Tab ───────────────────────────────────────────────

  Widget _buildCanvasTab() {
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
          child: SegmentedButton<RenderMode>(
            segments: RenderMode.values
                .map((m) => ButtonSegment<RenderMode>(
                      value: m,
                      label: Text(m.label),
                    ))
                .toList(),
            selected: {_canvasMode},
            onSelectionChanged: (s) => setState(() => _canvasMode = s.first),
          ),
        ),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16),
          child: Text(
            _canvasDescription(_canvasMode),
            textAlign: TextAlign.center,
            style: const TextStyle(fontSize: 13),
          ),
        ),
        const SizedBox(height: 8),
        Expanded(child: _buildCanvasView(_canvasMode)),
      ],
    );
  }

  Widget _buildCanvasView(RenderMode mode) {
    switch (mode) {
      case RenderMode.surfaceView:
        return const TriangleSurfaceWidget(key: ValueKey('canvasSurface'));
      case RenderMode.textureView:
        return const TriangleTextureViewWidget(key: ValueKey('canvasTexture'));
      case RenderMode.surfaceProducer:
        return const TriangleSurfaceProducerWidget(key: ValueKey('canvasProducer'));
      case RenderMode.surfaceTexture:
        return const TriangleSurfaceTextureWidget(key: ValueKey('canvasSurfaceTex'));
    }
  }

  String _canvasDescription(RenderMode mode) {
    switch (mode) {
      case RenderMode.surfaceView:
        return 'Android SurfaceView — Canvas API\nRed triangle. Skia OK / Impeller artifacts.';
      case RenderMode.textureView:
        return 'Android TextureView — Canvas API\nGreen triangle.';
      case RenderMode.surfaceProducer:
        return 'TextureRegistry.SurfaceProducer — Canvas API\nBlue triangle via Texture widget.';
      case RenderMode.surfaceTexture:
        return 'TextureRegistry.SurfaceTextureEntry — Canvas API\nAmber triangle via Texture widget.';
    }
  }

  // ── EGL Tab ──────────────────────────────────────────────────

  Widget _buildEglTab() {
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
          child: SegmentedButton<EglRenderMode>(
            segments: EglRenderMode.values
                .map((m) => ButtonSegment<EglRenderMode>(
                      value: m,
                      label: Text(m.label),
                    ))
                .toList(),
            selected: {_eglMode},
            onSelectionChanged: (s) => setState(() => _eglMode = s.first),
          ),
        ),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16),
          child: Text(
            _eglDescription(_eglMode),
            textAlign: TextAlign.center,
            style: const TextStyle(fontSize: 13),
          ),
        ),
        const SizedBox(height: 8),
        Expanded(child: _buildEglView(_eglMode)),
      ],
    );
  }

  Widget _buildEglView(EglRenderMode mode) {
    switch (mode) {
      case EglRenderMode.surfaceView:
        return const EglSurfaceViewWidget(key: ValueKey('eglSurface'));
      case EglRenderMode.textureView:
        return const EglTextureViewWidget(key: ValueKey('eglTexture'));
      case EglRenderMode.surfaceProducer:
        return const EglSurfaceProducerWidget(key: ValueKey('eglProducer'));
      case EglRenderMode.surfaceTexture:
        return const EglSurfaceTextureWidget(key: ValueKey('eglSurfaceTex'));
    }
  }

  String _eglDescription(EglRenderMode mode) {
    switch (mode) {
      case EglRenderMode.surfaceView:
        return 'Android SurfaceView — EGL14 + GLES20\nOrange triangle.';
      case EglRenderMode.textureView:
        return 'Android TextureView — EGL14 + GLES20\nPurple triangle.';
      case EglRenderMode.surfaceProducer:
        return 'TextureRegistry.SurfaceProducer — EGL14 + GLES20\nCyan triangle via Texture widget.';
      case EglRenderMode.surfaceTexture:
        return 'TextureRegistry.SurfaceTextureEntry — EGL14 + GLES20\nLime green triangle via Texture widget.';
    }
  }
}
