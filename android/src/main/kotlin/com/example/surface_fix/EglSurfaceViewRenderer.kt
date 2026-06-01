package com.example.surface_fix

import android.content.Context
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import io.flutter.plugin.platform.PlatformView

class EglSurfaceViewRenderer(context: Context) :
    EglHelper(context), PlatformView, SurfaceHolder.Callback {

    private val surfaceView = SurfaceView(context)
    private var surfaceHolder: SurfaceHolder? = null

    override val triangleColor = floatArrayOf(1.0f, 0.5f, 0.0f, 1.0f) // orange

    init {
        surfaceView.holder.addCallback(this)
    }

    override fun getView(): View = surfaceView

    override fun dispose() {
        surfaceView.holder.removeCallback(this)
        renderHandler?.post { release() }
    }

    // SurfaceHolder.Callback
    override fun surfaceCreated(holder: SurfaceHolder) {
        surfaceHolder = holder
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        surfaceHolder = holder
        startRender(width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        renderHandler?.post { stopRender() } ?: stopRender()
        surfaceHolder = null
    }

    override fun createEglWindowSurface(display: EGLDisplay, config: EGLConfig): EGLSurface {
        val attribs = intArrayOf(EGL14.EGL_NONE)
        return EGL14.eglCreateWindowSurface(display, config, surfaceHolder!!.surface, attribs, 0)
    }
}
