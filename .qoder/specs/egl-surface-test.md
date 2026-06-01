# EGL Surface Test — 实现规划

## Context

现有工程已有三种 Canvas 渲染方式（SurfaceView / TextureView / SurfaceProducer）用于复现 Impeller 花屏 bug。
现在需要新增三种对应的 **EGL 渲染方式**（纯 Kotlin，使用 `android.opengl.EGL14 + GLES20`），
通过 OpenGL ES 绘制相同的三角形 + 图标，验证 EGL 路径在三种 Surface 来源下的渲染行为差异。
UI 上新增第二个底部 NavigationBar Tab（"EGL"），与现有 "Canvas" Tab 并列。

---

## 文件清单

### 新增文件（Android Kotlin）
```
android/src/main/kotlin/com/example/surface_fix/
  ├── EglHelper.kt                   ← 公共 EGL 抽象基类
  ├── EglSurfaceViewRenderer.kt      ← SurfaceView + EGL（橙色三角形）
  ├── EglSurfaceViewFactory.kt       ← SurfaceView PlatformView 工厂
  ├── EglTextureViewRenderer.kt      ← TextureView + EGL（紫色三角形）
  ├── EglTextureViewFactory.kt       ← TextureView PlatformView 工厂
  └── EglSurfaceProducerRenderer.kt  ← SurfaceProducer + EGL（青色三角形）
```

### 修改文件
```
android/.../SurfaceFixPlugin.kt   ← 注册 2 个新 viewType + 2 个新 MethodChannel 方法
lib/surface_fix.dart              ← 新增 3 个 EGL Widget
example/lib/main.dart             ← NavigationBar + EGL Tab
```

---

## 实现细节

### 1. EglHelper.kt — 公共抽象基类

**EGL 初始化流程（在 HandlerThread 渲染线程上执行）：**
1. `EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)`
2. `EGL14.eglInitialize(display, major, minor)`
3. `EGL14.eglChooseConfig`：要求 `EGL_WINDOW_BIT`、`EGL_OPENGL_ES2_BIT`、RGBA 各 8 位
4. `EGL14.eglCreateContext` 带 `EGL_CONTEXT_CLIENT_VERSION=2`
5. 调用抽象方法 `createEglWindowSurface(display, config)` → 返回 EGLSurface
6. `EGL14.eglMakeCurrent`
7. 编译两个 GLSL Program（纯色 + 纹理），上传图标纹理

**抽象成员（子类实现）：**
- `abstract fun createEglWindowSurface(display: EGLDisplay, config: EGLConfig): EGLSurface`
- `abstract val triangleColor: FloatArray`  // RGBA float[4]

**GLSL 着色器：两个独立 Program**
- Program A（三角形纯色）：
  - 顶点：`attribute vec4 aPosition; void main() { gl_Position = aPosition; }`
  - 片段：`uniform vec4 uColor; void main() { gl_FragColor = uColor; }`
- Program B（图标纹理四边形）：
  - 顶点：`attribute vec4 aPosition; attribute vec2 aTexCoord; varying vec2 vTexCoord; void main() { gl_Position = aPosition; vTexCoord = aTexCoord; }`
  - 片段：`uniform sampler2D uTexture; varying vec2 vTexCoord; void main() { gl_FragColor = texture2D(uTexture, vTexCoord); }`

**drawFrame(width, height) 流程：**
1. `glViewport(0, 0, width, height)`
2. `glClearColor(1,1,1,1)` + `glClear(COLOR_BUFFER_BIT)`
3. 激活 Program A → 传 `uColor` → 绑定三角形顶点 VBO → `glDrawArrays(TRIANGLES, 0, 3)`
4. 激活 Program B → 绑定纹理 → 绑定图标四边形 VBO（位置+纹理坐标，左上角 48×48 px 换算为 NDC） → `glDrawArrays(TRIANGLE_STRIP, 0, 4)`
5. `EGL14.eglSwapBuffers(display, eglSurface)`

**NDC 坐标（三角形，固定）：**
- 顶部中心：`(0.0, 0.6)`
- 左下：`(-0.7, -0.6)`
- 右下：`(0.7, -0.6)`

**图标四边形：** 在 `drawFrame` 中根据 width/height 动态计算 48px 对应的 NDC 偏移量：
```kotlin
val nx = 48f / width * 2f   // 48px → NDC 宽度
val ny = 48f / height * 2f  // 48px → NDC 高度
// 左上角：NDC (-1 + margin, 1 - margin)
// 顶点顺序：TRIANGLE_STRIP，注意 OpenGL Y 轴翻转纹理坐标
```

**图标纹理加载（EGL 初始化后）：**
```kotlin
var bmp = BitmapFactory.decodeResource(context.resources, context.applicationInfo.icon)
    ?: Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
if (bmp.config != Bitmap.Config.ARGB_8888)
    bmp = bmp.copy(Bitmap.Config.ARGB_8888, false)
GLES20.glGenTextures(1, texIds, 0)
GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texIds[0])
GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)
GLES20.glTexParameteri(..., GL_TEXTURE_MIN_FILTER, GL_LINEAR)
GLES20.glTexParameteri(..., GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
bmp.recycle()
```

**渲染线程：** `HandlerThread("EglRenderThread")`，暴露 `startRender()` / `stopRender()` / `release()`。
⚠️ 所有 EGL 调用必须 `post` 到此线程，禁止在主线程直接调用。

**release() 顺序（在渲染线程执行）：**
1. `glDeleteBuffers` / `glDeleteTextures`
2. `eglMakeCurrent(..., NO_SURFACE, NO_SURFACE, NO_CONTEXT)`
3. `eglDestroyContext` + `eglDestroySurface` + `eglTerminate`
4. quit HandlerThread

---

### 2. EglSurfaceViewRenderer.kt

- 继承 `EglHelper`，实现 `PlatformView` + `SurfaceHolder.Callback`
- 持有一个 `SurfaceView` 实例（不是继承），`getView()` 返回它
- `triangleColor = floatArrayOf(1.0f, 0.5f, 0.0f, 1.0f)` // 橙色
- `createEglWindowSurface` → `EGL14.eglCreateWindowSurface(display, config, holder.surface, intArrayOf(EGL14.EGL_NONE), 0)`
- 生命周期：`surfaceCreated` → `startRender()`；`surfaceChanged` → 更新 size；`surfaceDestroyed` → renderHandler.post { stopRender() }
- `dispose()` → `renderHandler.post { release() }`

**EglSurfaceViewFactory.kt：** 标准 `PlatformViewFactory`，`create()` 返回 `EglSurfaceViewRenderer(context)`

---

### 3. EglTextureViewRenderer.kt

- 继承 `EglHelper`，实现 `PlatformView` + `TextureView.SurfaceTextureListener`
- 持有一个 `TextureView` 实例，`getView()` 返回它
- `triangleColor = floatArrayOf(0.6f, 0.0f, 0.8f, 1.0f)` // 紫色
- `createEglWindowSurface` → `EGL14.eglCreateWindowSurface(display, config, surfaceTexture, intArrayOf(EGL14.EGL_NONE), 0)`
- 生命周期：`onSurfaceTextureAvailable` → 保存 SurfaceTexture → `startRender()`；`onSurfaceTextureSizeChanged` → 重建；`onSurfaceTextureDestroyed` → renderHandler.post { stopRender() }，返回 `true`
- `dispose()` → `renderHandler.post { release() }`

**EglTextureViewFactory.kt：** 同上模式

---

### 4. EglSurfaceProducerRenderer.kt

- 继承 `EglHelper`（不是 PlatformView）
- `triangleColor = floatArrayOf(0.0f, 0.8f, 0.8f, 1.0f)` // 青色
- `create(width, height): Long`：
  1. `textureRegistry.createSurfaceProducer()` → `producer.setSize(w, h)` → 保存 surface
  2. `startRender()`（渲染线程执行 EGL init 并 drawFrame）
  3. 返回 `producer.id()`
- `createEglWindowSurface` → `EGL14.eglCreateWindowSurface(display, config, producer!!.surface, intArrayOf(EGL14.EGL_NONE), 0)`
- `dispose()` → `renderHandler.post { release() }` → `producer.release()`

---

### 5. SurfaceFixPlugin.kt 修改

在 `onAttachedToEngine` 中（工厂注册 try-catch 之外）追加：
```kotlin
// 新增两个 EGL PlatformView 工厂注册
platformViewRegistry.registerViewFactory("surface_fix/egl_surface_view", EglSurfaceViewFactory())
platformViewRegistry.registerViewFactory("surface_fix/egl_texture_view", EglTextureViewFactory())

// 新增 EGL SurfaceProducer Renderer
eglSurfaceProducerRenderer = EglSurfaceProducerRenderer(applicationContext, textureRegistry)
```

在 `onMethodCall` when 块追加：
```kotlin
"createEglSurfaceProducer" -> {
    val w = call.argument<Int>("width") ?: 640
    val h = call.argument<Int>("height") ?: 480
    result.success(eglSurfaceProducerRenderer?.create(w, h) ?: -1L)
}
"disposeEglSurfaceProducer" -> {
    eglSurfaceProducerRenderer?.dispose()
    result.success(null)
}
```

---

### 6. lib/surface_fix.dart 修改

`SurfaceFix` 类新增两个静态方法：
```dart
static Future<int> createEglSurfaceProducer(int width, int height) async { ... }
static Future<void> disposeEglSurfaceProducer() async { ... }
```

新增三个 Widget（模式与现有三个完全对应）：
- `EglSurfaceViewWidget`：viewType `'surface_fix/egl_surface_view'`，`initSurfaceAndroidView`
- `EglTextureViewWidget`：viewType `'surface_fix/egl_texture_view'`，`initSurfaceAndroidView`
- `EglSurfaceProducerWidget`：StatefulWidget，调用 `createEglSurfaceProducer` + `Texture` widget

---

### 7. example/lib/main.dart 修改

新增 `EglRenderMode` enum（orange/purple/cyan 颜色对应三角形）：
```dart
enum EglRenderMode {
  surfaceView('EGL SurfaceView', Colors.orange),
  textureView('EGL TextureView', Colors.purple),
  surfaceProducer('EGL Producer', Colors.cyan);
  ...
}
```

`_MyAppState` 新增：
- `int _tabIndex = 0`
- `EglRenderMode _eglMode = EglRenderMode.surfaceView`

Scaffold 结构：
- `body`：`IndexedStack(index: _tabIndex, children: [_buildCanvasTab(), _buildEglTab()])`
- `bottomNavigationBar`：`NavigationBar` 两个目标（Canvas / EGL）

`_buildCanvasTab()`：现有 Column 逻辑不变
`_buildEglTab()`：与 Canvas Tab 结构镜像，使用 `EglRenderMode` enum 和 `_buildEglView()`

---

## 关键注意事项

1. **EGL 线程亲和性**：所有 EGL/GLES 调用必须在 `HandlerThread` 上执行，Surface 回调（主线程）通过 `renderHandler.post {}` 切换
2. **attribs 结尾**：`eglCreateWindowSurface` 的 attribList 必须以 `EGL14.EGL_NONE` 结尾：`intArrayOf(EGL14.EGL_NONE)`
3. **Bitmap 格式**：上传前确保 `ARGB_8888`，否则 `GLUtils.texImage2D` 行为未定义
4. **图标四边形 NDC**：根据运行时 `width/height` 动态计算，不硬编码
5. **SurfaceProducer 错误处理**：`eglCreateWindowSurface` 失败时向 Flutter 返回 `-1L`

---

## 验证方案

1. 设备连接后运行 `flutter run` 进入 example 应用
2. 点击底部 "EGL" Tab
3. 切换三种 EGL 模式（SurfaceView / TextureView / SurfaceProducer）
4. 观察各模式下三角形 + 图标是否正常渲染
5. 切换 Skia / Impeller 模式对比（`--no-enable-impeller` vs 默认）：
   - Impeller 模式下 EGL TextureView 是否同样花屏
6. 切换回 "Canvas" Tab 确认原有功能不受影响
