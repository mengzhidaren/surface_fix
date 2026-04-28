package com.example.surface_fix

import android.util.Log
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

/** SurfaceFixPlugin */
class SurfaceFixPlugin :
    FlutterPlugin,
    MethodCallHandler {

    private lateinit var channel: MethodChannel
    private var surfaceProducerRenderer: TriangleSurfaceProducerRenderer? = null

    companion object {
        private const val TAG = "SurfaceFixPlugin"
    }

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "surface_fix")
        channel.setMethodCallHandler(this)

        flutterPluginBinding
            .platformViewRegistry
            .registerViewFactory(
                "surface_fix/triangle_surface",
                TriangleSurfaceViewFactory()
            )

        flutterPluginBinding
            .platformViewRegistry
            .registerViewFactory(
                "surface_fix/triangle_texture_view",
                TriangleTextureViewFactory()
            )

        try {
            surfaceProducerRenderer = TriangleSurfaceProducerRenderer(
                flutterPluginBinding.applicationContext,
                flutterPluginBinding.textureRegistry
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init SurfaceProducerRenderer", e)
        }
    }

    override fun onMethodCall(
        call: MethodCall,
        result: Result
    ) {
        when (call.method) {
            "getPlatformVersion" -> {
                result.success("Android ${android.os.Build.VERSION.RELEASE}")
            }
            "createSurfaceProducer" -> {
                val width = call.argument<Int>("width") ?: 640
                val height = call.argument<Int>("height") ?: 480
                val textureId = surfaceProducerRenderer?.create(width, height) ?: -1L
                result.success(textureId)
            }
            "disposeSurfaceProducer" -> {
                surfaceProducerRenderer?.dispose()
                result.success(null)
            }
            else -> result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        surfaceProducerRenderer?.dispose()
        surfaceProducerRenderer = null
    }
}
