import 'package:flutter/material.dart';
import 'package:surface_fix/surface_fix.dart';

void main() {
  runApp(const MyApp());
}

enum RenderMode {
  surfaceView('SurfaceView', Colors.red),
  textureView('TextureView', Colors.green),
  surfaceProducer('SurfaceProducer', Colors.blue);

  const RenderMode(this.label, this.color);
  final String label;
  final Color color;
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  RenderMode _mode = RenderMode.surfaceView;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(title: const Text('Surface Bug Repro')),
        body: Column(
          children: [
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              child: SegmentedButton<RenderMode>(
                segments: RenderMode.values
                    .map(
                      (m) => ButtonSegment<RenderMode>(
                        value: m,
                        label: Text(m.label),
                      ),
                    )
                    .toList(),
                selected: {_mode},
                onSelectionChanged: (selected) {
                  setState(() => _mode = selected.first);
                },
              ),
            ),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: Text(
                _descriptionFor(_mode),
                textAlign: TextAlign.center,
                style: const TextStyle(fontSize: 13),
              ),
            ),
            const SizedBox(height: 8),
            Expanded(child: _buildView(_mode)),
          ],
        ),
      ),
    );
  }

  Widget _buildView(RenderMode mode) {
    switch (mode) {
      case RenderMode.surfaceView:
        return const TriangleSurfaceWidget(key: ValueKey('surfaceView'));
      case RenderMode.textureView:
        return const TriangleTextureViewWidget(key: ValueKey('textureView'));
      case RenderMode.surfaceProducer:
        return const TriangleSurfaceProducerWidget(
          key: ValueKey('surfaceProducer'),
        );
    }
  }

  String _descriptionFor(RenderMode mode) {
    switch (mode) {
      case RenderMode.surfaceView:
        return 'Android SurfaceView (PlatformView)\n'
            'Red triangle. Skia OK / Impeller artifacts.';
      case RenderMode.textureView:
        return 'Android TextureView (PlatformView)\n'
            'Green triangle.';
      case RenderMode.surfaceProducer:
        return 'TextureRegistry.SurfaceProducer\n'
            'Blue triangle rendered via Texture widget.';
    }
  }
}
