package com.example.surface_fix

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.Surface
import io.flutter.view.TextureRegistry

class TriangleSurfaceProducerRenderer(
    private val context: Context,
    private val textureRegistry: TextureRegistry
) {
    private var surfaceProducer: TextureRegistry.SurfaceProducer? = null

    private val trianglePaint = Paint().apply {
        color = Color.BLUE
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

    fun create(width: Int, height: Int): Long {
        val producer = textureRegistry.createSurfaceProducer()
        producer.setSize(width, height)
        surfaceProducer = producer

        drawTriangle(producer.surface, width, height)

        return producer.id()
    }

    fun dispose() {
        surfaceProducer?.release()
        surfaceProducer = null
    }

    private fun drawTriangle(surface: Surface, width: Int, height: Int) {
        val canvas: Canvas = surface.lockCanvas(null) ?: return
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

        surface.unlockCanvasAndPost(canvas)
    }
}
