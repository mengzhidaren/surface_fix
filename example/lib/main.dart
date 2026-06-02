import 'package:flutter/material.dart';
import 'package:surface_fix/surface_fix.dart';

void main() {
  runApp(const MyApp());
}

// ── App ───────────────────────────────────────────────────────
class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  int _tabIndex = 0;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: Text(
            _tabIndex == 0 ? 'Canvas Surface Test' : 'EGL Surface Test',
          ),
        ),
        body: IndexedStack(
          index: _tabIndex,
          children: [_buildCanvasTab(), _buildEglTab()],
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

  // ── Canvas Tab — 2x2 grid ─────────────────────────────────────

  Widget _buildCanvasTab() {
    return GridView.count(
      crossAxisCount: 2,
      padding: const EdgeInsets.all(4),
      mainAxisSpacing: 4,
      crossAxisSpacing: 4,
      children: [
        _GridCell(
          label: 'SurfaceView',
          color: Colors.red,
          child: const TriangleSurfaceWidget(key: ValueKey('canvasSurface')),
        ),
        _GridCell(
          label: 'TextureView',
          color: Colors.green,
          child: const TriangleTextureViewWidget(
            key: ValueKey('canvasTexture'),
          ),
        ),
        _GridCell(
          label: 'SurfaceProducer',
          color: Colors.blue,
          child: const TriangleSurfaceProducerWidget(
            key: ValueKey('canvasProducer'),
          ),
        ),
        _GridCell(
          label: 'SurfaceTextureEntry',
          color: Colors.amber,
          child: const TriangleSurfaceTextureWidget(
            key: ValueKey('canvasSurfaceTextureEntry'),
          ),
        ),
      ],
    );
  }

  // ── EGL Tab — 2x2 grid ────────────────────────────────────────

  Widget _buildEglTab() {
    return GridView.count(
      crossAxisCount: 2,
      padding: const EdgeInsets.all(4),
      mainAxisSpacing: 4,
      crossAxisSpacing: 4,
      children: [
        _GridCell(
          label: 'EGL SurfaceView',
          color: Colors.orange,
          child: const EglSurfaceViewWidget(key: ValueKey('eglSurface')),
        ),
        _GridCell(
          label: 'EGL TextureView',
          color: Colors.purple,
          child: const EglTextureViewWidget(key: ValueKey('eglTexture')),
        ),
        _GridCell(
          label: 'EGL SurfaceProducer',
          color: Colors.cyan,
          child: const EglSurfaceProducerWidget(
            key: ValueKey('eglSurfaceProducer'),
          ),
        ),
        _GridCell(
          label: 'EGL SurfaceTextureEntry',
          color: Colors.lightGreen,
          child: const EglSurfaceTextureWidget(
            key: ValueKey('eglSurfaceTextureEntry'),
          ),
        ),
      ],
    );
  }
}

// ── Grid cell with label overlay ──────────────────────────────
class _GridCell extends StatelessWidget {
  const _GridCell({
    required this.label,
    required this.color,
    required this.child,
  });

  final String label;
  final Color color;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Card(
      clipBehavior: Clip.antiAlias,
      margin: EdgeInsets.zero,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(8),
        side: BorderSide(color: color, width: 2),
      ),
      child: Column(
        children: [
          Container(
            width: double.infinity,
            color: color.withValues(alpha: 0.15),
            padding: const EdgeInsets.symmetric(vertical: 4),
            child: Text(
              label,
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.bold,
                color: color,
              ),
            ),
          ),
          Expanded(child: child),
        ],
      ),
    );
  }
}
