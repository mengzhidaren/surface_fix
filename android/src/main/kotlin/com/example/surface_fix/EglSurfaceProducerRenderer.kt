package com.example.surface_fix

import android.content.Context
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.util.Log
import io.flutter.view.TextureRegistry

class EglSurfaceProducerRenderer(
    context: Context,
    private val textureRegistry: TextureRegistry
) : EglHelper(context) {

    companion object {
        private const val TAG = "EglSurfaceProducer"
    }

    private var surfaceProducer: TextureRegistry.SurfaceProducer? = null

    override val triangleColor = floatArrayOf(0.0f, 0.8f, 0.8f, 1.0f) // cyan

    fun create(width: Int, height: Int): Long {
        return try {
            val producer = textureRegistry.createSurfaceProducer()
            producer.setSize(width, height)
            surfaceProducer = producer
            startRender(width, height)
            producer.id()
        } catch (e: Exception) {
            Log.e(TAG, "create failed", e)
            -1L
        }
    }

    fun dispose() {
        release()
        surfaceProducer?.release()
        surfaceProducer = null
    }

    override fun createEglWindowSurface(display: EGLDisplay, config: EGLConfig): EGLSurface {
        val attribs = intArrayOf(EGL14.EGL_NONE)
        return EGL14.eglCreateWindowSurface(
            display, config, surfaceProducer!!.surface, attribs, 0
        )
    }
}
