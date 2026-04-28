package com.example.surface_fix

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.SurfaceTexture
import android.view.TextureView
import android.view.View
import io.flutter.plugin.platform.PlatformView

class TriangleTextureView(context: Context) : TextureView(context), TextureView.SurfaceTextureListener, PlatformView {

    private val trianglePaint = Paint().apply {
        color = Color.GREEN
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
        surfaceTextureListener = this
    }

    override fun getView(): View = this

    override fun dispose() {
        surfaceTextureListener = null
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        drawTriangle(width, height)
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        drawTriangle(width, height)
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}

    private fun drawTriangle(width: Int, height: Int) {
        val canvas: Canvas = lockCanvas() ?: return
        val w = width.toFloat()
        val h = height.toFloat()

        canvas.drawRect(0f, 0f, w, h, bgPaint)

        val path = Path().apply {
            moveTo(w / 2f, h * 0.2f)
            lineTo(w * 0.15f, h * 0.8f)
            lineTo(w * 0.85f, h * 0.8f)
            close()
        }
        canvas.drawPath(path, trianglePaint)

        // draw Android icon at top-left
        canvas.drawBitmap(iconBitmap, 16f, 16f, null)

        unlockCanvasAndPost(canvas)
    }
}
