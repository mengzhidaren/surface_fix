package com.example.surface_fix

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import io.flutter.plugin.platform.PlatformView

class TriangleSurfaceView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, PlatformView {

    private val trianglePaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val bgPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val iconBitmap: Bitmap = BitmapFactory.decodeResource(
        context.resources,
        context.applicationInfo.icon
    ) ?: Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)

    init {
        holder.addCallback(this)
    }

    override fun getView(): View = this

    override fun dispose() {
        holder.removeCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        drawTriangle()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        drawTriangle()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {}

    private fun drawTriangle() {
        val canvas: Canvas? = holder.lockCanvas()
        canvas?.let {
            val w = it.width.toFloat()
            val h = it.height.toFloat()

            // draw white background
            it.drawRect(0f, 0f, w, h, bgPaint)

            // draw a centered triangle
            val path = Path().apply {
                moveTo(w / 2f, h * 0.2f)       // top center
                lineTo(w * 0.15f, h * 0.8f)    // bottom left
                lineTo(w * 0.85f, h * 0.8f)    // bottom right
                close()
            }
            it.drawPath(path, trianglePaint)

            // draw Android icon at top-left
            it.drawBitmap(iconBitmap, 16f, 16f, null)

            holder.unlockCanvasAndPost(it)
        }
    }
}
