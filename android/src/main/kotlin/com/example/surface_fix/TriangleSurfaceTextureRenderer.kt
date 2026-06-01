package com.example.surface_fix

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.Surface
import android.util.Log
import io.flutter.view.TextureRegistry

/**
 * Canvas rendering via TextureRegistry.SurfaceTextureEntry.
 * Creates a SurfaceTexture through TextureRegistry.createSurfaceTexture(),
 * wraps it with android.graphics.Surface, then uses lockCanvas/unlockCanvasAndPost
 * to draw a yellow triangle + app icon.
 */
class TriangleSurfaceTextureRenderer(
    private val context: Context,
    private val textureRegistry: TextureRegistry
) {
    companion object {
        private const val TAG = "TriangleSurfaceTexture"
    }

    private var surfaceTextureEntry: TextureRegistry.SurfaceTextureEntry? = null
    private var surface: Surface? = null

    private val trianglePaint = Paint().apply {
        color = Color.parseColor("#FFC107") // amber / yellow
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val bgPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val iconBitmap: Bitmap = run {
        var bmp = BitmapFactory.decodeResource(context.resources, context.applicationInfo.icon)
            ?: Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
        if (bmp.config != Bitmap.Config.ARGB_8888) bmp = bmp.copy(Bitmap.Config.ARGB_8888, false)
        bmp
    }

    fun create(width: Int, height: Int): Long {
        return try {
            val entry = textureRegistry.createSurfaceTexture()
            entry.surfaceTexture().setDefaultBufferSize(width, height)
            surfaceTextureEntry = entry

            val sf = Surface(entry.surfaceTexture())
            surface = sf

            drawTriangle(sf, width, height)

            entry.id()
        } catch (e: Exception) {
            Log.e(TAG, "create failed", e)
            -1L
        }
    }

    fun dispose() {
        surface?.release()
        surface = null
        surfaceTextureEntry?.release()
        surfaceTextureEntry = null
    }

    private fun drawTriangle(sf: Surface, width: Int, height: Int) {
        val canvas: Canvas = sf.lockCanvas(null) ?: return
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

        canvas.drawBitmap(iconBitmap, 16f, 16f, null)

        sf.unlockCanvasAndPost(canvas)
    }
}
