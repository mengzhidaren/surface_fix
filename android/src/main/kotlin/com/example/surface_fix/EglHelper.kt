package com.example.surface_fix

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.opengl.GLUtils
import android.os.Handler
import android.os.HandlerThread
import android.util.Log

abstract class EglHelper(protected val context: Context) {

    companion object {
        private const val TAG = "EglHelper"

        private const val VERT_TRIANGLE = """
attribute vec4 aPosition;
void main() {
    gl_Position = aPosition;
}
"""
        private const val FRAG_TRIANGLE = """
precision mediump float;
uniform vec4 uColor;
void main() {
    gl_FragColor = uColor;
}
"""
        private const val VERT_ICON = """
attribute vec4 aPosition;
attribute vec2 aTexCoord;
varying vec2 vTexCoord;
void main() {
    gl_Position = aPosition;
    vTexCoord = aTexCoord;
}
"""
        private const val FRAG_ICON = """
precision mediump float;
uniform sampler2D uTexture;
varying vec2 vTexCoord;
void main() {
    gl_FragColor = texture2D(uTexture, vTexCoord);
}
"""
    }

    // Subclass must provide the EGLSurface from its specific surface source
    protected abstract fun createEglWindowSurface(display: EGLDisplay, config: EGLConfig): EGLSurface
    protected abstract val triangleColor: FloatArray

    // EGL objects
    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
    private var eglConfig: EGLConfig? = null

    // GL programs
    private var triProgram = 0
    private var iconProgram = 0
    private var iconTexId = 0

    // Render thread
    private var renderThread: HandlerThread? = null
    protected var renderHandler: Handler? = null

    protected var surfaceWidth = 0
    protected var surfaceHeight = 0

    // -------------------------------------------------------------------
    // Render thread lifecycle
    // -------------------------------------------------------------------

    fun startRender(width: Int, height: Int) {
        surfaceWidth = width
        surfaceHeight = height
        renderThread = HandlerThread("EglRenderThread").also { it.start() }
        renderHandler = Handler(renderThread!!.looper)
        renderHandler!!.post { initEgl() }
    }

    fun updateSize(width: Int, height: Int) {
        surfaceWidth = width
        surfaceHeight = height
        renderHandler?.post { drawFrame() }
    }

    fun stopRender() {
        renderHandler?.post {
            destroyEgl()
            renderThread?.quit()
            renderThread = null
            renderHandler = null
        }
    }

    fun release() {
        stopRender()
    }

    // -------------------------------------------------------------------
    // EGL init / destroy (must run on render thread)
    // -------------------------------------------------------------------

    private fun initEgl() {
        // 1. Display
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
            Log.e(TAG, "eglGetDisplay failed")
            return
        }
        val version = IntArray(2)
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
            Log.e(TAG, "eglInitialize failed")
            return
        }

        // 2. Config
        val attribList = intArrayOf(
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_DEPTH_SIZE, 16,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        EGL14.eglChooseConfig(eglDisplay, attribList, 0, configs, 0, 1, numConfigs, 0)
        if (numConfigs[0] == 0) {
            Log.e(TAG, "eglChooseConfig failed")
            return
        }
        eglConfig = configs[0]!!

        // 3. Context
        val ctxAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
        eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT, ctxAttribs, 0)
        if (eglContext == EGL14.EGL_NO_CONTEXT) {
            Log.e(TAG, "eglCreateContext failed")
            return
        }

        // 4. Window surface (provided by subclass)
        eglSurface = createEglWindowSurface(eglDisplay, eglConfig!!)
        if (eglSurface == EGL14.EGL_NO_SURFACE) {
            Log.e(TAG, "createEglWindowSurface failed: error=${EGL14.eglGetError()}")
            return
        }

        // 5. Make current
        if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            Log.e(TAG, "eglMakeCurrent failed")
            return
        }

        // 6. Build GL programs
        triProgram = buildProgram(VERT_TRIANGLE.trimIndent(), FRAG_TRIANGLE.trimIndent())
        iconProgram = buildProgram(VERT_ICON.trimIndent(), FRAG_ICON.trimIndent())

        // 7. Upload icon texture
        iconTexId = uploadIconTexture()

        // 8. Draw first frame
        drawFrame()
    }

    private fun destroyEgl() {
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) return

        EGL14.eglMakeCurrent(
            eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT
        )

        if (triProgram != 0) { GLES20.glDeleteProgram(triProgram); triProgram = 0 }
        if (iconProgram != 0) { GLES20.glDeleteProgram(iconProgram); iconProgram = 0 }
        if (iconTexId != 0) {
            GLES20.glDeleteTextures(1, intArrayOf(iconTexId), 0)
            iconTexId = 0
        }

        if (eglSurface != EGL14.EGL_NO_SURFACE) {
            EGL14.eglDestroySurface(eglDisplay, eglSurface)
            eglSurface = EGL14.EGL_NO_SURFACE
        }
        if (eglContext != EGL14.EGL_NO_CONTEXT) {
            EGL14.eglDestroyContext(eglDisplay, eglContext)
            eglContext = EGL14.EGL_NO_CONTEXT
        }
        EGL14.eglTerminate(eglDisplay)
        eglDisplay = EGL14.EGL_NO_DISPLAY
    }

    // -------------------------------------------------------------------
    // Draw (must run on render thread)
    // -------------------------------------------------------------------

    protected fun drawFrame() {
        if (eglDisplay == EGL14.EGL_NO_DISPLAY || eglSurface == EGL14.EGL_NO_SURFACE) return
        if (surfaceWidth <= 0 || surfaceHeight <= 0) return

        GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight)
        GLES20.glClearColor(1f, 1f, 1f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        drawTriangle()
        drawIcon()

        EGL14.eglSwapBuffers(eglDisplay, eglSurface)
    }

    private fun drawTriangle() {
        // NDC vertices: top-center, bottom-left, bottom-right
        val verts = floatArrayOf(
            0.0f,  0.6f,   // top center
           -0.7f, -0.6f,   // bottom left
            0.7f, -0.6f    // bottom right
        )
        val vBuf = java.nio.ByteBuffer.allocateDirect(verts.size * 4)
            .order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer()
            .apply { put(verts); position(0) }

        GLES20.glUseProgram(triProgram)

        val aPos = GLES20.glGetAttribLocation(triProgram, "aPosition")
        GLES20.glEnableVertexAttribArray(aPos)
        GLES20.glVertexAttribPointer(aPos, 2, GLES20.GL_FLOAT, false, 8, vBuf)

        val uColor = GLES20.glGetUniformLocation(triProgram, "uColor")
        GLES20.glUniform4fv(uColor, 1, triangleColor, 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3)
        GLES20.glDisableVertexAttribArray(aPos)
    }

    private fun drawIcon() {
        if (iconTexId == 0) return
        val w = surfaceWidth.toFloat()
        val h = surfaceHeight.toFloat()
        val margin = 16f
        val size = 48f

        // Convert pixel coords to NDC
        // NDC x = (px / w) * 2 - 1
        // NDC y = 1 - (py / h) * 2   (flip Y)
        val x0 = (margin / w) * 2f - 1f
        val y0 = 1f - (margin / h) * 2f          // top
        val x1 = ((margin + size) / w) * 2f - 1f
        val y1 = 1f - ((margin + size) / h) * 2f // bottom

        // TRIANGLE_STRIP: TL, BL, TR, BR
        // Tex coords: OpenGL Y is flipped relative to Bitmap, so tex (0,0)=bottom-left
        val verts = floatArrayOf(
            x0, y0,   0f, 0f,   // TL  pos  tex(0,1 in bitmap = 0,0 in GL flipped... use 0,1)
            x0, y1,   0f, 1f,   // BL
            x1, y0,   1f, 0f,   // TR
            x1, y1,   1f, 1f    // BR
        )
        val vBuf = java.nio.ByteBuffer.allocateDirect(verts.size * 4)
            .order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer()
            .apply { put(verts); position(0) }

        GLES20.glUseProgram(iconProgram)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        val stride = 4 * 4 // 4 floats per vertex * 4 bytes
        val aPos = GLES20.glGetAttribLocation(iconProgram, "aPosition")
        val aTex = GLES20.glGetAttribLocation(iconProgram, "aTexCoord")

        GLES20.glEnableVertexAttribArray(aPos)
        GLES20.glEnableVertexAttribArray(aTex)

        vBuf.position(0)
        GLES20.glVertexAttribPointer(aPos, 2, GLES20.GL_FLOAT, false, stride, vBuf)
        vBuf.position(2)
        GLES20.glVertexAttribPointer(aTex, 2, GLES20.GL_FLOAT, false, stride, vBuf)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, iconTexId)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(iconProgram, "uTexture"), 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(aPos)
        GLES20.glDisableVertexAttribArray(aTex)
        GLES20.glDisable(GLES20.GL_BLEND)
    }

    // -------------------------------------------------------------------
    // GL helpers
    // -------------------------------------------------------------------

    private fun buildProgram(vertSrc: String, fragSrc: String): Int {
        val vert = compileShader(GLES20.GL_VERTEX_SHADER, vertSrc)
        val frag = compileShader(GLES20.GL_FRAGMENT_SHADER, fragSrc)
        return GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vert)
            GLES20.glAttachShader(it, frag)
            GLES20.glLinkProgram(it)
            GLES20.glDeleteShader(vert)
            GLES20.glDeleteShader(frag)
        }
    }

    private fun compileShader(type: Int, src: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, src)
            GLES20.glCompileShader(shader)
            val status = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
            if (status[0] == 0) {
                Log.e(TAG, "Shader compile error: ${GLES20.glGetShaderInfoLog(shader)}")
            }
        }
    }

    private fun uploadIconTexture(): Int {
        var bmp = BitmapFactory.decodeResource(context.resources, context.applicationInfo.icon)
            ?: Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
        if (bmp.config != Bitmap.Config.ARGB_8888) {
            bmp = bmp.copy(Bitmap.Config.ARGB_8888, false)
        }

        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, ids[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)
        bmp.recycle()
        return ids[0]
    }
}
