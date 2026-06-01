package com.example.surface_fix

import android.content.Context
import android.view.Surface
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.util.Log
import io.flutter.view.TextureRegistry

/**
 * EGL rendering via TextureRegistry.SurfaceTextureEntry.
 * Creates a SurfaceTexture through TextureRegistry.createSurfaceTexture(),
 * wraps it with android.graphics.Surface, then uses that Surface as the
 * EGL window surface for OpenGL ES rendering (lime/yellow-green triangle).
 */
class EglSurfaceTextureRenderer(
    context: Context,
    private val textureRegistry: TextureRegistry
) : EglHelper(context) {

    companion object {
        private const val TAG = "EglSurfaceTexture"
    }

    private var surfaceTextureEntry: TextureRegistry.SurfaceTextureEntry? = null
    // Wrap SurfaceTexture → android.graphics.Surface for EGL
    private var nativeSurface: Surface? = null

    override val triangleColor = floatArrayOf(0.5f, 0.9f, 0.0f, 1.0f) // lime green

    fun create(width: Int, height: Int): Long {
        return try {
            val entry = textureRegistry.createSurfaceTexture()
            entry.surfaceTexture().setDefaultBufferSize(width, height)
            surfaceTextureEntry = entry
            nativeSurface = Surface(entry.surfaceTexture())

            startRender(width, height)
            entry.id()
        } catch (e: Exception) {
            Log.e(TAG, "create failed", e)
            -1L
        }
    }

    fun dispose() {
        release()
        nativeSurface?.release()
        nativeSurface = null
        surfaceTextureEntry?.release()
        surfaceTextureEntry = null
    }

    override fun createEglWindowSurface(display: EGLDisplay, config: EGLConfig): EGLSurface {
        val attribs = intArrayOf(EGL14.EGL_NONE)
        // Pass the android.graphics.Surface wrapping the SurfaceTexture
        return EGL14.eglCreateWindowSurface(display, config, nativeSurface!!, attribs, 0)
    }
}
