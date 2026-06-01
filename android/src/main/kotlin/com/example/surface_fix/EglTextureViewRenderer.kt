package com.example.surface_fix

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.view.TextureView
import android.view.View
import io.flutter.plugin.platform.PlatformView

class EglTextureViewRenderer(context: Context) :
    EglHelper(context), PlatformView, TextureView.SurfaceTextureListener {

    private val textureView = TextureView(context)
    private var surfaceTexture: SurfaceTexture? = null

    override val triangleColor = floatArrayOf(0.6f, 0.0f, 0.8f, 1.0f) // purple

    init {
        textureView.surfaceTextureListener = this
    }

    override fun getView(): View = textureView

    override fun dispose() {
        textureView.surfaceTextureListener = null
        renderHandler?.post { release() } ?: release()
    }

    // TextureView.SurfaceTextureListener
    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        surfaceTexture = surface
        startRender(width, height)
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        updateSize(width, height)
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        renderHandler?.post { stopRender() } ?: stopRender()
        surfaceTexture = null
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}

    override fun createEglWindowSurface(display: EGLDisplay, config: EGLConfig): EGLSurface {
        val attribs = intArrayOf(EGL14.EGL_NONE)
        return EGL14.eglCreateWindowSurface(display, config, surfaceTexture, attribs, 0)
    }
}
