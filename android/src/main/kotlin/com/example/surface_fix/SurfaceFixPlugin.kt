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
    private var eglSurfaceProducerRenderer: EglSurfaceProducerRenderer? = null
    private var surfaceTextureRenderer: TriangleSurfaceTextureRenderer? = null
    private var eglSurfaceTextureRenderer: EglSurfaceTextureRenderer? = null

    companion object {
        private const val TAG = "SurfaceFixPlugin"
    }

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "surface_fix")
        channel.setMethodCallHandler(this)

        // Canvas PlatformView factories
        flutterPluginBinding
            .platformViewRegistry
            .registerViewFactory("surface_fix/triangle_surface", TriangleSurfaceViewFactory())

        flutterPluginBinding
            .platformViewRegistry
            .registerViewFactory("surface_fix/triangle_texture_view", TriangleTextureViewFactory())

        // EGL PlatformView factories
        flutterPluginBinding
            .platformViewRegistry
            .registerViewFactory("surface_fix/egl_surface_view", EglSurfaceViewFactory())

        flutterPluginBinding
            .platformViewRegistry
            .registerViewFactory("surface_fix/egl_texture_view", EglTextureViewFactory())

        val appContext = flutterPluginBinding.applicationContext
        val texReg = flutterPluginBinding.textureRegistry

        // Canvas SurfaceProducer
        try {
            surfaceProducerRenderer = TriangleSurfaceProducerRenderer(appContext, texReg)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init Canvas SurfaceProducerRenderer", e)
        }

        // EGL SurfaceProducer
        try {
            eglSurfaceProducerRenderer = EglSurfaceProducerRenderer(appContext, texReg)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init EGL SurfaceProducerRenderer", e)
        }

        // Canvas SurfaceTextureEntry
        try {
            surfaceTextureRenderer = TriangleSurfaceTextureRenderer(appContext, texReg)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init Canvas SurfaceTextureRenderer", e)
        }

        // EGL SurfaceTextureEntry
        try {
            eglSurfaceTextureRenderer = EglSurfaceTextureRenderer(appContext, texReg)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init EGL SurfaceTextureRenderer", e)
        }
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "getPlatformVersion" -> {
                result.success("Android ${android.os.Build.VERSION.RELEASE}")
            }
            // Canvas SurfaceProducer
            "createSurfaceProducer" -> {
                val width = call.argument<Int>("width") ?: 640
                val height = call.argument<Int>("height") ?: 480
                result.success(surfaceProducerRenderer?.create(width, height) ?: -1L)
            }
            "disposeSurfaceProducer" -> {
                surfaceProducerRenderer?.dispose()
                result.success(null)
            }
            // EGL SurfaceProducer
            "createEglSurfaceProducer" -> {
                val width = call.argument<Int>("width") ?: 640
                val height = call.argument<Int>("height") ?: 480
                result.success(eglSurfaceProducerRenderer?.create(width, height) ?: -1L)
            }
            "disposeEglSurfaceProducer" -> {
                eglSurfaceProducerRenderer?.dispose()
                result.success(null)
            }
            // Canvas SurfaceTextureEntry
            "createSurfaceTexture" -> {
                val width = call.argument<Int>("width") ?: 640
                val height = call.argument<Int>("height") ?: 480
                result.success(surfaceTextureRenderer?.create(width, height) ?: -1L)
            }
            "disposeSurfaceTexture" -> {
                surfaceTextureRenderer?.dispose()
                result.success(null)
            }
            // EGL SurfaceTextureEntry
            "createEglSurfaceTexture" -> {
                val width = call.argument<Int>("width") ?: 640
                val height = call.argument<Int>("height") ?: 480
                result.success(eglSurfaceTextureRenderer?.create(width, height) ?: -1L)
            }
            "disposeEglSurfaceTexture" -> {
                eglSurfaceTextureRenderer?.dispose()
                result.success(null)
            }
            else -> result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        surfaceProducerRenderer?.dispose()
        surfaceProducerRenderer = null
        eglSurfaceProducerRenderer?.dispose()
        eglSurfaceProducerRenderer = null
        surfaceTextureRenderer?.dispose()
        surfaceTextureRenderer = null
        eglSurfaceTextureRenderer?.dispose()
        eglSurfaceTextureRenderer = null
    }
}
