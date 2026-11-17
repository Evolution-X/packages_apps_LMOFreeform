package com.libremobileos.freeform.server.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.os.Handler
import android.provider.Settings
import android.util.Slog
import android.view.Display
import android.view.DisplayInfo
import android.graphics.Matrix
import android.view.InputDevice
import android.view.GestureDetector
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.IRotationWatcher
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.android.server.LocalServices
import com.android.server.wm.WindowManagerInternal
import com.libremobileos.freeform.ILMOFreeformDisplayCallback
import com.libremobileos.freeform.server.Debug.dlog
import com.libremobileos.freeform.server.LMOFreeformServiceHolder
import com.libremobileos.freeform.server.SystemServiceHolder
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign

class FreeformWindow(
    val handler: Handler,
    val context: Context,
    private val appConfig: AppConfig,
    val freeformConfig: FreeformConfig
): TextureView.SurfaceTextureListener, ILMOFreeformDisplayCallback.Stub(), View.OnTouchListener,
    WindowManagerInternal.DisplaySecureContentListener {

    var freeformTaskStackListener: FreeformTaskStackListener? = null
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val windowManagerInt = LocalServices.getService(WindowManagerInternal::class.java)
    val windowParams = WindowManager.LayoutParams()
    private val resourceHolder = RemoteResourceHolder(context, FREEFORM_PACKAGE)
    var freeformLayout: ViewGroup? = null
    var freeformWindowView: ViewGroup? = null
    var fullscreenPreview: FreeformFullscreenPreview? = null
    var freeformRootView: ViewGroup? = null
    var freeformView: TextureView? = null
    private var topBarView: View? = null
    private var bottomBarView: View? = null
    private var appIconView: ImageView? = null
    private var packageNameView: TextView? = null
    var veilView: ViewGroup? = null
    private var displayId = Display.INVALID_DISPLAY
    var defaultDisplayWidth = context.resources.displayMetrics.widthPixels
    var defaultDisplayHeight = context.resources.displayMetrics.heightPixels
    private val hangUpGestureListener = HangUpGestureListener(this)
    private val defaultDisplayInfo = DisplayInfo()
    private val destroyRunnable = Runnable { destroy("destroyRunnable", true) }

    private var appPackageName: String = ""
    private var appIcon: Drawable? = null
    private var isInitialized = false

    private val rotationWatcher = object : IRotationWatcher.Stub() {
        override fun onRotationChanged(rotation: Int) {
            dlog(TAG, "onRotationChanged($rotation)")
            defaultDisplayWidth = context.resources.displayMetrics.widthPixels
            defaultDisplayHeight = context.resources.displayMetrics.heightPixels
            measureSize()
            handler.post {
                val layout = freeformWindowView ?: freeformLayout ?: return@post
                changeOrientation()
                if (freeformConfig.isHangUp) {
                    toHangUp()
                } else {
                    setSidebarAwarePosition()
                    runCatching { windowManager.updateViewLayout(layout, windowParams) }
                    makeSureFreeformInScreen()
                }
            }
            measureScale()
            LMOFreeformServiceHolder.resizeFreeform(
                this@FreeformWindow,
                freeformConfig.freeformWidth,
                freeformConfig.freeformHeight,
                freeformConfig.densityDpi
            )
            freeformView?.surfaceTexture?.setDefaultBufferSize(
                freeformConfig.freeformWidth,
                freeformConfig.freeformHeight
            )
        }
    }

    companion object {
        private const val TAG = "LMOFreeform/FreeformWindow"
        private const val FREEFORM_PACKAGE = "com.libremobileos.freeform"
        private const val FREEFORM_LAYOUT = "view_freeform"
        private const val WINDOW_DESTROY_WAIT_MS = 10000L
        private const val DRAWABLE_CLOSE = "ic_minimize"
        private const val DRAWABLE_OPEN_FULL_SCREEN = "ic_maximize"
        private const val DRAWABLE_PIN = "ic_pin"
        const val TITLE_CLOSE = "Close"
        const val TITLE_OPEN_FULL_SCREEN = "Open full screen"
        const val TITLE_PIN = "Pin app"
        // M3 Expressive default spatial curve (damping 0.8, stiffness 380, overshoot y1 = 1.21)
        val EXPRESSIVE_SPATIAL_INTERPOLATOR = PathInterpolator(0.38f, 1.21f, 0.22f, 1.00f)
        // M3 Expressive default effects curve (damping 1.0, stiffness 1600, no overshoot)
        val EXPRESSIVE_EFFECTS_INTERPOLATOR = PathInterpolator(0.34f, 0.80f, 0.34f, 1.00f)
        private const val M3E_DURATION_ENTER = 200L
        private const val M3E_DURATION_EXIT = 150L
        private const val SHIFT_DP = 6f
        private const val CONTROL_MODE_SETTING = "freeform_control_mode"
        private const val CONTROL_MODE_BUTTON = "Button"
        private const val CONTROL_MODE_GESTURE = "Gesture"
        private const val LEGACY_BUTTON_MODE = "0"
        private const val LEGACY_GESTURE_MODE = "1"
    }

    init {
        if (LMOFreeformServiceHolder.ping()) {
            Slog.i(TAG, "FreeformWindow init")
            extractPackageInfo()
            populateFreeformConfig()
            handler.post {
                if (addFreeformView()) {
                    isInitialized = true
                } else {
                    destroy("init:addFreeform failed")
                }
            }
        } else {
            Slog.e(TAG, "FreeformWindow init failed: service not running")
            destroy("init:service not running")
        }
    }

    override fun onDisplayPaused() {
        //NOT USED
    }

    override fun onDisplayResumed() {
        //NOT USED
    }

    override fun onDisplayStopped() {
        //NOT USED
    }

    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        dlog(TAG, "onSurfaceTextureAvailable width:$width height:$height")
        if (displayId < 0) {
            LMOFreeformServiceHolder.createDisplay(freeformConfig, appConfig, Surface(surfaceTexture), this)
        }
        surfaceTexture.setDefaultBufferSize(freeformConfig.freeformWidth, freeformConfig.freeformHeight)
    }

    override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        surfaceTexture.setDefaultBufferSize(freeformConfig.freeformWidth, freeformConfig.freeformHeight)
    }

    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
        return true
    }

    override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
        //NOT USED
    }

    override fun onDisplayAdd(displayId: Int) {
        Slog.i(TAG, "onDisplayAdd displayId=$displayId, $appConfig")
        handler.post {
            this.displayId = displayId
            freeformTaskStackListener = FreeformTaskStackListener(displayId, this)
            SystemServiceHolder.activityTaskManager.registerTaskStackListener(freeformTaskStackListener)
            if (appConfig.taskId != -1) {
                dlog(TAG, "moving taskId=${appConfig.taskId} to freeform display")
                freeformTaskStackListener!!.taskId = appConfig.taskId
                runCatching {
                    // TODO: find a new way for this since getTaskDescription was removed in fwb commit a7cae90a991e
                    // if (SystemServiceHolder.activityTaskManager.getTaskDescription(appConfig.taskId) == null) {
                    //     throw Exception("stale task")
                    // }
                    SystemServiceHolder.activityTaskManager.moveRootTaskToDisplay(appConfig.taskId, displayId)
                }
                .onFailure { e ->
                    Slog.e(TAG, "failed to move task ${appConfig.taskId}: $e, fallback to startApp")
                    startApp()
                }
            } else if (appConfig.userId == -100) {
                if (appConfig.pendingIntent == null) destroy("onDisplayAdd:userId=-100, but pendingIntent is null")
                else {
                    LMOFreeformServiceHolder.startPendingIntent(appConfig.pendingIntent, displayId)
                }
            } else {
                startApp()
            }

            val layout = freeformLayout
            if (layout == null) {
                Slog.e(TAG, "freeformLayout is null")
                destroy("onDisplayAdd:freeformLayout is null")
                return@post
            }
            if (!isGestureMode) {
                resourceHolder.getLayoutChildViewByTag<View>(layout, "arrowBack")
                    ?.setOnClickListener(RightViewClickListener(displayId))
            }
        }
    }

    private fun startApp() {
        if (displayId == Display.INVALID_DISPLAY) {
            Slog.e(TAG, "cannot startApp: displayId not yet set!")
            return
        }
        if (LMOFreeformServiceHolder.startApp(context, appConfig, displayId).not())
            destroy("startApp failed")
    }

    override fun onDisplayHasSecureWindowOnScreenChanged(displayId: Int, hasSecureWindowOnScreen: Boolean) {
        if (displayId != this.displayId) return;
        dlog(TAG, "onDisplayHasSecureWindowOnScreenChanged: $hasSecureWindowOnScreen")
        windowParams.apply {
            flags = if (hasSecureWindowOnScreen) {
                flags or WindowManager.LayoutParams.FLAG_SECURE
            } else {
                flags xor WindowManager.LayoutParams.FLAG_SECURE
            }
        }
        handler.post {
            val layout = freeformWindowView ?: freeformLayout ?: return@post
            runCatching { windowManager.updateViewLayout(layout, windowParams) }
                .onFailure { Slog.e(TAG, "updateViewLayout failed: $it") }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View, event: MotionEvent): Boolean {
        val newEvent = MotionEvent.obtain(event)
        val scaleMatrix = Matrix().apply {
            setScale(freeformConfig.scale, freeformConfig.scale)
        }
        newEvent.transform(scaleMatrix)
        newEvent.setSource(InputDevice.SOURCE_TOUCHSCREEN)
        LMOFreeformServiceHolder.touch(newEvent, displayId)
        newEvent.recycle()
        return true
    }

    /**
     * get freeform screen dimen / freeform view dimen
     */
    private fun populateFreeformConfig() {
        measureSize()
        measureScale()
        context.display.getDisplayInfo(defaultDisplayInfo)
        val maxRefreshRate = context.display.supportedModes
            .maxOfOrNull { it.refreshRate } ?: defaultDisplayInfo.refreshRate
        freeformConfig.apply {
            refreshRate = maxRefreshRate
            presentationDeadlineNanos = if (maxRefreshRate > 0f) {
                (1_000_000_000L / maxRefreshRate).toLong()
            } else {
                defaultDisplayInfo.presentationDeadlineNanos
            }
            dlog(TAG, "populateFreeformConfig: $this")
        }
    }

    fun measureSize() {
        val isPortrait = defaultDisplayWidth < defaultDisplayHeight
        val density = context.resources.displayMetrics.density
        freeformConfig.apply {
            if (isPortrait) {
                height = (defaultDisplayHeight * 0.4).roundToInt()
                width = (defaultDisplayWidth * 0.7).roundToInt()
            } else {
                val chromeH = (104 * density).roundToInt()
                val safeContentH = defaultDisplayHeight - chromeH - (40 * density).roundToInt()
                height = min(safeContentH, (defaultDisplayHeight * 0.62).roundToInt())
                val minW = (200 * density).roundToInt()
                val maxW = (defaultDisplayWidth * 0.45).roundToInt()
                width = max(minW, (height * 0.72).roundToInt()).coerceAtMost(maxW)
            }
            dlog(TAG, "measureSize: isPortrait=$isPortrait width=$width height=$height")
        }
    }

    fun measureScale() {
        freeformConfig.apply {
            val widthScale = min(defaultDisplayWidth, defaultDisplayHeight) * 1.0f / min(width, height)
            val heightScale = max(defaultDisplayWidth, defaultDisplayHeight) * 1.0f / max(width, height)
            scale = min(widthScale, heightScale)
            freeformWidth = (width * scale).roundToInt()
            freeformHeight = (height * scale).roundToInt()
            dlog(TAG, "measureScale: $scale freeformWidth=$freeformWidth freeformHeight=$freeformHeight")
        }
    }

    /**
     * Called in system handler
     */
    @SuppressLint("WrongConstant")
    private fun addFreeformView(): Boolean {
        dlog(TAG, "addFreeformView")
        val tmpFreeformLayout = resourceHolder.getLayout(FREEFORM_LAYOUT)!! ?: return false
        freeformLayout = tmpFreeformLayout
        val pad = (context.resources.displayMetrics.density * 16).roundToInt()
        freeformWindowView = FrameLayout(context).apply {
            clipChildren = false
            clipToPadding = false
            setPadding(pad, pad, pad, pad)
            addView(
                tmpFreeformLayout,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
            )
        }
        freeformRootView = resourceHolder.getLayoutChildViewByTag<FrameLayout>(tmpFreeformLayout, "freeform_root") ?: return false
        veilView = resourceHolder.getLayoutChildViewByTag<FrameLayout>(tmpFreeformLayout, "veilView") ?: return false
        topBarView = resourceHolder.getLayoutChildViewByTag(tmpFreeformLayout, "topBarView") ?: return false
        bottomBarView = resourceHolder.getLayoutChildViewByTag(tmpFreeformLayout, "bottomBarView") ?: return false
        val moveTouchListener = MoveTouchListener(this)
        topBarView?.setOnTouchListener(moveTouchListener)
        bottomBarView?.setOnTouchListener(moveTouchListener)
        val appIconView = resourceHolder.getLayoutChildViewByTag<ImageView>(tmpFreeformLayout, "appIcon")
        val packageNameView = resourceHolder.getLayoutChildViewByTag<TextView>(tmpFreeformLayout, "packageName")
        val pinView = resourceHolder.getLayoutChildViewByTag<View>(tmpFreeformLayout, "pinView")
        val maximizeView = resourceHolder.getLayoutChildViewByTag<View>(tmpFreeformLayout, "maximizeView")
        val minimizeView = resourceHolder.getLayoutChildViewByTag<View>(tmpFreeformLayout, "minimizeView")
        val leftScaleView = resourceHolder.getLayoutChildViewByTag<View>(tmpFreeformLayout, "leftScaleView")
        val rightScaleView = resourceHolder.getLayoutChildViewByTag<View>(tmpFreeformLayout, "rightScaleView")
        val gesturePillTouchView = resourceHolder.getLayoutChildViewByTag<View>(tmpFreeformLayout, "gesturePillTouchView")
        val gesturePillView = resourceHolder.getLayoutChildViewByTag<View>(tmpFreeformLayout, "gesturePill")
        val gesturePillPlateView = resourceHolder.getLayoutChildViewByTag<View>(tmpFreeformLayout, "gesturePillPlate")
        val arrowBackView = resourceHolder.getLayoutChildViewByTag<View>(tmpFreeformLayout, "arrowBack")
        val veilAppIconView = resourceHolder.getLayoutChildViewByTag<ImageView>(tmpFreeformLayout, "veilAppIcon")
        if (null == leftScaleView || null == rightScaleView || null == pinView
                || null == appIconView || null == packageNameView || null == veilAppIconView
                || null == maximizeView || null == minimizeView
                || null == gesturePillTouchView || null == gesturePillView || null == gesturePillPlateView
                || null == arrowBackView) {
            Slog.e(TAG, "freeform chrome view is null")
            destroy("addFreeformView:freeform chrome view is null")
            return false
        }
        this.appIconView = appIconView
        this.packageNameView = packageNameView
        veilAppIconView.setImageDrawable(appIcon)
        appIconView.setImageDrawable(appIcon)
        packageNameView.text = appPackageName
        val actionPillContainer = resourceHolder.getLayoutChildViewByTag<View>(tmpFreeformLayout, "actionPillContainer")
        if (isGestureMode) {
            actionPillContainer?.visibility = View.GONE
            pinView.visibility = View.GONE
            maximizeView.visibility = View.GONE
            minimizeView.visibility = View.GONE
            gesturePillTouchView.visibility = View.VISIBLE
            arrowBackView.visibility = View.GONE
            gesturePillTouchView.setOnTouchListener(
                PillGestureController(this, gesturePillView, gesturePillPlateView)
            )
        } else {
            actionPillContainer?.visibility = View.VISIBLE
            pinView.visibility = View.VISIBLE
            maximizeView.visibility = View.VISIBLE
            minimizeView.visibility = View.VISIBLE
            gesturePillTouchView.visibility = View.GONE
            arrowBackView.visibility = View.VISIBLE
            minimizeView.setOnClickListener(LeftViewClickListener(this))
            maximizeView.setOnClickListener(MaximizeClickListener(this))
            pinView.setOnClickListener(PinClickListener(this))
        }
        leftScaleView.setOnTouchListener(ScaleTouchListener(this, false))
        rightScaleView.setOnTouchListener(ScaleTouchListener(this))

        freeformView = FreeformTextureView(context).apply {
            setOnTouchListener(this@FreeformWindow)
            surfaceTextureListener = this@FreeformWindow
        }
        val rootView = freeformRootView ?: return false
        val view = freeformView ?: return false
        rootView.layoutParams = rootView.layoutParams.apply {
            width = freeformConfig.width
            height = freeformConfig.height
        }
        rootView.addView(view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        windowParams.apply {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            flags = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                    WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
            format = PixelFormat.RGBA_8888
            windowAnimations = android.R.style.Animation_Dialog
        }
        setSidebarAwarePosition()

        val windowView = freeformWindowView ?: freeformLayout ?: return false
        runCatching {
            fullscreenPreview = FreeformFullscreenPreview(context, windowManager, resourceHolder)
            fullscreenPreview?.attach()
            windowManager.addView(windowView, windowParams)
            SystemServiceHolder.windowManager.watchRotation(rotationWatcher, Display.DEFAULT_DISPLAY)
            windowManagerInt.registerDisplaySecureContentListener(this)
        }.onFailure {
            Slog.e(TAG, "addView failed: $it")
            return false
        }
        return true
    }

    /**
     * Called in system handler
     */
    @SuppressLint("ClickableViewAccessibility")
    fun handleHangUp() {
        val rootView = freeformRootView ?: return
        val layout = freeformWindowView ?: freeformLayout ?: return
        val topBar = topBarView ?: return
        val bottomBar = bottomBarView ?: return
        val view = freeformView ?: return

        topBar.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)

        if (freeformConfig.isHangUp) {
            FreeformAnimation.cancelMoveAnimators()
            val pad = (context.resources.displayMetrics.density * 16).roundToInt()
            freeformWindowView?.setPadding(pad, pad, pad, pad)
            windowParams.apply {
                x = 0
                y = 0
                flags = flags or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
            }
            freeformConfig.notInHangUpX = 0
            freeformConfig.notInHangUpY = 0
            rootView.layoutParams.apply {
                width = freeformConfig.width
                height = freeformConfig.height
            }
            windowManager.updateViewLayout(layout, windowParams)
            topBar.visibility = View.VISIBLE
            bottomBar.visibility = View.VISIBLE
            freeformConfig.isHangUp = false
            view.setOnTouchListener(this)
            layout.animate().cancel()
            layout.translationX = 0f
            layout.translationY = 0f
            layout.alpha = 0f
            layout.scaleX = 0.95f
            layout.scaleY = 0.95f
            layout.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(M3E_DURATION_ENTER)
                .setInterpolator(EXPRESSIVE_EFFECTS_INTERPOLATOR)
                .start()
        } else {
            FreeformAnimation.cancelMoveAnimators()
            freeformConfig.notInHangUpX = windowParams.x
            freeformConfig.notInHangUpY = windowParams.y
            toHangUp()
            topBar.visibility = View.GONE
            bottomBar.visibility = View.GONE
            freeformConfig.isHangUp = true
            val gestureDetector = GestureDetector(context, hangUpGestureListener)
            view.setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
                if (event.action == MotionEvent.ACTION_UP && hangUpGestureListener.isDragging) {
                    hangUpGestureListener.isDragging = false
                    makeSureFreeformInScreen()
                }
                true
            }
        }
    }

    /**
     * Called in system handler
     */
    fun toHangUp() {
        val rootView = freeformRootView ?: return
        val layout = freeformWindowView ?: freeformLayout ?: return
        val density = context.resources.displayMetrics.density
        val safeTopMargin = (28 * density).roundToInt()

        freeformWindowView?.setPadding(0, 0, 0, 0)
        windowParams.apply {
            x = (defaultDisplayWidth / 2 - freeformConfig.hangUpWidth / 2)
            y = -(defaultDisplayHeight / 2 - freeformConfig.hangUpHeight / 2) + safeTopMargin
            flags = flags xor WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
        }
        rootView.layoutParams = rootView.layoutParams.apply {
            width = freeformConfig.hangUpWidth
            height = freeformConfig.hangUpHeight
        }
        runCatching { windowManager.updateViewLayout(layout, windowParams) }.onFailure { Slog.e(TAG, "$it") }
    }

    /**
     * Called in uiHandler
     */
    fun makeSureFreeformInScreen() {
        val rootView = freeformRootView ?: return

        val density = context.resources.displayMetrics.density
        val chromeH = (104 * density).roundToInt()
        val safeTopMargin = (28 * density).roundToInt()
        val safeBottomMargin = (24 * density).roundToInt()
        val maxContentHeight = max((150 * density).roundToInt(), defaultDisplayHeight - chromeH - safeTopMargin - safeBottomMargin)
        val maxContentWidth = max((150 * density).roundToInt(), (defaultDisplayWidth * 0.9).roundToInt() - (32 * density).roundToInt())

        if (!freeformConfig.isHangUp) {
            if (rootView.layoutParams.width > maxContentWidth || rootView.layoutParams.height > maxContentHeight) {
                rootView.layoutParams = rootView.layoutParams.apply {
                    width = min(rootView.layoutParams.width, maxContentWidth)
                    height = min(rootView.layoutParams.height, maxContentHeight)
                }
                freeformConfig.width = rootView.layoutParams.width
                freeformConfig.height = rootView.layoutParams.height
            }
        }
        val totalWinWidth = if (freeformConfig.isHangUp) {
            freeformConfig.hangUpWidth
        } else {
            (freeformLayout?.width?.takeIf { it > 0 } ?: freeformConfig.width) + (32 * density).roundToInt()
        }
        val totalWinHeight = if (freeformConfig.isHangUp) {
            freeformConfig.hangUpHeight
        } else {
            (freeformLayout?.height?.takeIf { it > 0 } ?: (freeformConfig.height + (72 * density).roundToInt())) + (32 * density).roundToInt()
        }

        val maxX = max(0, (defaultDisplayWidth - totalWinWidth) / 2)
        val minY = safeTopMargin - (defaultDisplayHeight - totalWinHeight) / 2
        val maxY = (defaultDisplayHeight - totalWinHeight) / 2 - safeBottomMargin

        val targetX = windowParams.x.coerceIn(-maxX, maxX)
        val targetY = if (minY <= maxY) windowParams.y.coerceIn(minY, maxY) else minY

        if (windowParams.x != targetX) {
            FreeformAnimation.moveInScreenAnimator(windowParams.x, targetX, 350L, true, this)
        }
        if (windowParams.y != targetY) {
            FreeformAnimation.moveInScreenAnimator(windowParams.y, targetY, 350L, false, this)
        }
    }

    /**
     * Change freeform orientation
     * Called in system handler
     */
    fun changeOrientation() {
        val rootView = freeformRootView ?: return

        rootView.layoutParams = rootView.layoutParams.apply {
            width = if (freeformConfig.isHangUp) freeformConfig.hangUpWidth else freeformConfig.width
            height = if (freeformConfig.isHangUp) freeformConfig.hangUpHeight else freeformConfig.height
        }
        measureScale()
        LMOFreeformServiceHolder.resizeFreeform(
            this,
            freeformConfig.freeformWidth,
            freeformConfig.freeformHeight,
            freeformConfig.densityDpi
        )
        freeformView?.surfaceTexture?.setDefaultBufferSize(
            freeformConfig.freeformWidth,
            freeformConfig.freeformHeight
        )
    }

    fun getFreeformId(): String {
        return "${appConfig.packageName},${appConfig.activityName},${appConfig.userId}"
    }

    fun goBack() {
        if (displayId == Display.INVALID_DISPLAY) return
        LMOFreeformServiceHolder.back(displayId)
    }

    val closeIcon: Drawable?
        get() = resourceHolder.getDrawable(DRAWABLE_CLOSE)

    val openFullScreenIcon: Drawable?
        get() = resourceHolder.getDrawable(DRAWABLE_OPEN_FULL_SCREEN)

    val pinIcon: Drawable?
        get() = resourceHolder.getDrawable(DRAWABLE_PIN)

    fun updateTitle(
        title: CharSequence?,
        icon: Drawable? = null,
        directionY: Float = 0f,
        animated: Boolean = true
    ) {
        val text = title ?: return
        if (!handler.looper.isCurrentThread) {
            handler.post { updateTitle(text, icon, directionY, animated) }
            return
        }

        val nameView = packageNameView ?: return
        val iconView = appIconView

        if (nameView.text == text) {
            if (nameView.alpha == 1f && nameView.translationY == 0f) return
            nameView.animate().cancel()
            iconView?.animate()?.cancel()
            nameView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(M3E_DURATION_ENTER)
                .setInterpolator(EXPRESSIVE_SPATIAL_INTERPOLATOR)
                .start()
            iconView?.animate()
                ?.alpha(1f)
                ?.translationY(0f)
                ?.setDuration(M3E_DURATION_ENTER)
                ?.setInterpolator(EXPRESSIVE_SPATIAL_INTERPOLATOR)
                ?.start()
            return
        }

        if (!animated) {
            nameView.animate().cancel()
            iconView?.animate()?.cancel()
            nameView.text = text
            nameView.alpha = 1f
            nameView.translationY = 0f
            icon?.let { iconView?.setImageDrawable(it) }
            iconView?.alpha = 1f
            iconView?.translationY = 0f
            return
        }

        val shiftPx = SHIFT_DP * context.resources.displayMetrics.density * (if (directionY != 0f) sign(directionY) else -1f)

        nameView.animate().cancel()
        iconView?.animate()?.cancel()

        iconView?.animate()
            ?.alpha(0f)
            ?.translationY(shiftPx)
            ?.setDuration(M3E_DURATION_EXIT)
            ?.setInterpolator(EXPRESSIVE_EFFECTS_INTERPOLATOR)
            ?.start()

        nameView.animate()
            .alpha(0f)
            .translationY(shiftPx)
            .setDuration(M3E_DURATION_EXIT)
            .setInterpolator(EXPRESSIVE_EFFECTS_INTERPOLATOR)
            .withEndAction {
                nameView.text = text
                nameView.translationY = -shiftPx
                nameView.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(M3E_DURATION_ENTER)
                    .setInterpolator(EXPRESSIVE_SPATIAL_INTERPOLATOR)
                    .start()

                iconView?.let { iv ->
                    icon?.let { iv.setImageDrawable(it) }
                    iv.translationY = -shiftPx
                    iv.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(M3E_DURATION_ENTER)
                        .setInterpolator(EXPRESSIVE_SPATIAL_INTERPOLATOR)
                        .start()
                }
            }
            .start()
    }

    fun resetTitle(directionY: Float = 0f, animated: Boolean = true) {
        updateTitle(appPackageName, appIcon, directionY, animated)
    }

    fun getWindowBoundsOnScreen(targetIdle: Boolean = false): Rect {
        val layout = freeformLayout
        if (layout != null && layout.width > 0 && layout.height > 0) {
            val loc = IntArray(2)
            layout.getLocationOnScreen(loc)
            if (targetIdle) {
                val cx = loc[0] + (layout.width * layout.scaleX) / 2f
                val cy = loc[1] - layout.translationY + (layout.height * layout.scaleY) / 2f
                val w = layout.width.toFloat()
                val h = layout.height.toFloat()
                return Rect(
                    (cx - w / 2f).roundToInt(),
                    (cy - h / 2f).roundToInt(),
                    (cx + w / 2f).roundToInt(),
                    (cy + h / 2f).roundToInt()
                )
            }
            val w = (layout.width * layout.scaleX).roundToInt()
            val h = (layout.height * layout.scaleY).roundToInt()
            return Rect(loc[0], loc[1], loc[0] + w, loc[1] + h)
        }
        val w = freeformConfig.width
        val h = freeformConfig.height
        val left = (defaultDisplayWidth - w) / 2 + windowParams.x
        val top = (defaultDisplayHeight - h) / 2 + windowParams.y
        return Rect(left, top, left + w, top + h)
    }

    fun enterFullscreen() {
        topBarView?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        updateTitle(TITLE_OPEN_FULL_SCREEN, openFullScreenIcon, directionY = 1f)
        val listener = freeformTaskStackListener
        if (listener == null || listener.taskId == -1) {
            Slog.e(TAG, "taskId is -1, can`t move")
            return
        }
        runCatching {
            SystemServiceHolder.activityTaskManager.moveRootTaskToDisplay(listener.taskId, Display.DEFAULT_DISPLAY)
        }.onFailure {
            Slog.e(TAG, "enterFullscreen failed", it)
        }
    }

    fun close() {
        dlog(TAG, "close()")
        topBarView?.performHapticFeedback(HapticFeedbackConstants.REJECT)
        updateTitle(TITLE_CLOSE, closeIcon, directionY = -1f)
        runCatching {
            SystemServiceHolder.activityTaskManager.removeTask(freeformTaskStackListener!!.taskId)
            removeView()
        }.onFailure { exception ->
            Slog.e(TAG, "removeTask failed: ", exception)
            destroy("window.close() fallback")
        }
    }

    fun removeView(runDestroy: Boolean = true) {
        dlog(TAG, "removeView($runDestroy)")
        handler.removeCallbacks(destroyRunnable)

        fullscreenPreview?.detach()
        fullscreenPreview = null

        (freeformWindowView ?: freeformLayout)?.let { layout ->
            handler.post {
                runCatching {
                    windowManager.removeViewImmediate(layout)
                    dlog(TAG, "removeView success")
                }.onFailure { exception ->
                    Slog.e(TAG, "removeView failed $exception")
                }
            }
        }

        // wait for onTaskRemoved(), but take it into our own hands in case its never triggered.
        if (runDestroy)
            handler.postDelayed(destroyRunnable, WINDOW_DESTROY_WAIT_MS)
    }

    fun destroy(callReason: String, shouldRemoveTask: Boolean = false) {
        Slog.i(TAG, "destroy ${getFreeformId()}, displayId=$displayId callReason: $callReason")

        if (!isInitialized) {
            Slog.w(TAG, "destroy called on uninitialized window")
            return
        }

        removeView(false)
        fullscreenPreview?.detach()
        fullscreenPreview = null
        handler.removeCallbacks(destroyRunnable)

        freeformTaskStackListener?.let {
            SystemServiceHolder.activityTaskManager.unregisterTaskStackListener(it)
        }

        runCatching {
            SystemServiceHolder.windowManager.removeRotationWatcher(rotationWatcher)
        }.onFailure {
            Slog.e(TAG, "Failed to remove rotation watcher", it)
        }

        LMOFreeformServiceHolder.releaseFreeform(this)
        FreeformWindowManager.removeWindow(getFreeformId())
        windowManagerInt.unregisterDisplaySecureContentListener(this)

        freeformTaskStackListener?.taskId?.let {
            if (it != -1 && shouldRemoveTask) {
                Slog.i(TAG, "destroy: remove taskId $it again")
                runCatching { SystemServiceHolder.activityTaskManager.removeTask(it) }
            }
        }

        packageNameView?.animate()?.cancel()
        packageNameView?.alpha = 1f
        packageNameView?.translationY = 0f
        appIconView?.animate()?.cancel()
        appIconView?.alpha = 1f
        appIconView?.translationY = 0f
        (freeformWindowView ?: freeformLayout)?.animate()?.cancel()
        FreeformAnimation.cancelMoveAnimators()
        packageNameView = null
        appIconView = null
        isInitialized = false
    }

    private fun extractPackageInfo() {
        try {
            val pm = context.packageManager
            val ai = pm.getApplicationInfo(appConfig.packageName, 0)
            appPackageName = pm.getApplicationLabel(ai).toString()
            appIcon = pm.getApplicationIcon(ai)
        } catch (e: Exception) {
            Slog.e(TAG, "Failed to retrieve app info: ${e.message}")
            appPackageName = ""
            appIcon = null
        }
    }

    private fun setSidebarAwarePosition() {
        val isLandscape = defaultDisplayWidth > defaultDisplayHeight
        val windowWidth = freeformConfig.width
        val margin = 80

        windowParams.apply {
            if (isLandscape) {
                x = -(defaultDisplayWidth / 2) + (windowWidth / 2) + margin
                y = 0
            } else {
                x = 0
                y = 0
            }
        }

        val mode = if (isLandscape) "landscape (left side)" else "portrait (center)"
        dlog(TAG, "Orientation-aware positioning: $mode, windowX=${windowParams.x}, windowY=${windowParams.y}, screenSize=${defaultDisplayWidth}x${defaultDisplayHeight}")
    }

    private val isGestureMode: Boolean
        get() {
            val mode = Settings.System.getString(context.contentResolver, CONTROL_MODE_SETTING)
            return when {
                mode == null -> true
                mode.equals(CONTROL_MODE_BUTTON, ignoreCase = true) ||
                        mode == LEGACY_BUTTON_MODE -> false
                mode.equals(CONTROL_MODE_GESTURE, ignoreCase = true) ||
                        mode == LEGACY_GESTURE_MODE -> true
                else -> true
            }
        }
}
