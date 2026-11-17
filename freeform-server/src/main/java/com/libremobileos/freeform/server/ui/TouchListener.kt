package com.libremobileos.freeform.server.ui

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import com.libremobileos.freeform.server.LMOFreeformServiceHolder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

class MoveTouchListener(
    private val window: FreeformWindow
) : View.OnTouchListener{
    private var startX = 0.0f
    private var startY = 0.0f
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        val layout = window.freeformWindowView ?: window.freeformLayout ?: return true
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.rawX
                startY = event.rawY
            }
            MotionEvent.ACTION_MOVE -> {
                window.windowManager.updateViewLayout(layout, window.windowParams.apply {
                    x = (x + event.rawX - startX).roundToInt()
                    y = (y + event.rawY - startY).roundToInt()
                })
                startX = event.rawX
                startY = event.rawY
            }
            MotionEvent.ACTION_UP -> {
                window.makeSureFreeformInScreen()
            }
        }
        return true
    }
}

class LeftViewClickListener(private val window: FreeformWindow) : View.OnClickListener {
    override fun onClick(v: View) {
        v.performHapticFeedback(HapticFeedbackConstants.REJECT)
        window.close()
    }
}

/**
 * maximize freeform screen
 */
class MaximizeClickListener(private val window: FreeformWindow): View.OnClickListener {
    override fun onClick(v: View) {
        v.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        window.enterFullscreen()
    }
}


sealed interface PillAction {
    object CloseWindow : PillAction
    object EnterFullscreen : PillAction
    object Back : PillAction
}

data class PillAppearance(
    val pillColor: Int,
    val outlineColor: Int,
    val plateColor: Int,
    val showPlate: Boolean
)

object PillAppearanceResolver {
    fun resolve(isDarkTheme: Boolean): PillAppearance {
        val pillColor = if (isDarkTheme) {
            setAlpha(Color.WHITE, 245)
        } else {
            setAlpha(Color.BLACK, 217)
        }
        val outlineColor = if (isDarkTheme) {
            setAlpha(Color.BLACK, 54)
        } else {
            setAlpha(Color.WHITE, 54)
        }
        return PillAppearance(
            pillColor = pillColor,
            outlineColor = outlineColor,
            plateColor = Color.TRANSPARENT,
            showPlate = false
        )
    }

    private fun setAlpha(color: Int, alpha: Int): Int = Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
}

class PillGestureController(
    private val window: FreeformWindow,
    private val pillView: View,
    private val plateView: View
) : View.OnTouchListener {
    companion object {
        private const val MIN_SWIPE_DP = 40
        private const val FLING_VELOCITY_DP = 600
        private const val PILL_IDLE_WIDTH_DP = 64
        private const val PILL_ACTIVE_WIDTH_DP = 84
    }

    private val touchSlop = ViewConfiguration.get(pillView.context).scaledTouchSlop
    private val swipeThreshold = max(dp(MIN_SWIPE_DP), touchSlop)
    private var startX = 0f
    private var startY = 0f
    private var activeAction: PillAction? = null
    private var baseWindowHeight = 0
    private var velocityTracker: VelocityTracker? = null

    init {
        updateAppearance()
        pillView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updateAppearance() }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.rawX
                startY = event.rawY
                activeAction = null
                window.resetTitle(animated = false)
                captureBaseWindowSize()
                window.freeformLayout?.animate()?.cancel()
                pillView.animate()?.cancel()
                window.fullscreenPreview?.cancel(immediate = true)
                velocityTracker = VelocityTracker.obtain().also { it.addMovement(event) }
                updateAppearance()
                animatePressed(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                velocityTracker?.addMovement(event)
                updateInteractiveGesture(event.rawX - startX, event.rawY - startY)
                return true
            }
            MotionEvent.ACTION_UP -> {
                velocityTracker?.addMovement(event)
                velocityTracker?.computeCurrentVelocity(1000)
                val velocityY = velocityTracker?.yVelocity ?: 0f
                val deltaX = event.rawX - startX
                val deltaY = event.rawY - startY
                recycleVelocityTracker()
                animatePressed(false)
                dispatchGesture(deltaX, deltaY, velocityY)
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                activeAction = null
                recycleVelocityTracker()
                animatePressed(false)
                animateBackToIdle()
                return true
            }
        }
        return true
    }

    private fun updateInteractiveGesture(deltaX: Float, deltaY: Float) {
        val layout = window.freeformLayout ?: return
        val scaleDistance = max(baseWindowHeight.takeIf { it > 0 } ?: layout.height, dp(180)).toFloat()

        if (deltaY < 0) {
            val scale = (1f + deltaY / scaleDistance).coerceIn(0.01f, 1f)
            layout.pivotX = layout.width / 2f
            layout.pivotY = layout.height.toFloat()
            layout.scaleX = scale
            layout.scaleY = scale
            layout.translationY = deltaY * 0.12f
            layout.alpha = scale.coerceIn(0f, 1f)
            pillView.translationY = 0f
        } else {
            val maxDragDistance = dp(160).toFloat()
            val dampedDeltaY = if (deltaY <= maxDragDistance) deltaY else maxDragDistance + (deltaY - maxDragDistance) * 0.15f
            val expansionProgress = (dampedDeltaY / maxDragDistance).coerceIn(0f, 1f)

            val scale = 1f + (expansionProgress * 0.035f)
            layout.pivotX = layout.width / 2f
            layout.pivotY = layout.height / 2f
            layout.scaleX = scale
            layout.scaleY = scale
            layout.translationY = (dampedDeltaY * 0.06f).coerceIn(0f, dp(8).toFloat())
            layout.alpha = 1f

            val progress = (deltaY / swipeThreshold).coerceIn(0f, 1f)
            pillView.translationY = (dampedDeltaY * 0.20f).coerceIn(0f, dp(8).toFloat())
            val targetPillWidth = dp(PILL_IDLE_WIDTH_DP + ((PILL_ACTIVE_WIDTH_DP - PILL_IDLE_WIDTH_DP) * progress).roundToInt())
            if (pillView.layoutParams.width != targetPillWidth) {
                pillView.layoutParams = pillView.layoutParams.apply { width = targetPillWidth }
            }
        }

        val isHorizontal = abs(deltaY) <= abs(deltaX)
        val thresholdAction = when {
            isHorizontal -> null
            deltaY < -swipeThreshold -> PillAction.CloseWindow
            deltaY > swipeThreshold -> PillAction.EnterFullscreen
            else -> null
        }
        if (thresholdAction != activeAction) {
            if (thresholdAction != null) {
                pillView.performHapticFeedback(HapticFeedbackConstants.SEGMENT_TICK)
            }
            when (thresholdAction) {
                PillAction.CloseWindow -> {
                    val windowBounds = window.getWindowBoundsOnScreen()
                    window.fullscreenPreview?.cancel(windowBounds)
                    window.updateTitle(FreeformWindow.TITLE_CLOSE, window.closeIcon, directionY = -1f)
                }
                PillAction.EnterFullscreen -> {
                    window.updateTitle(FreeformWindow.TITLE_OPEN_FULL_SCREEN, window.openFullScreenIcon, directionY = 1f)
                    val windowBounds = window.getWindowBoundsOnScreen()
                    window.fullscreenPreview?.triggerFullscreen(windowBounds, dp(28).toFloat())
                }
                else -> {
                    val returnDirection = if (activeAction == PillAction.CloseWindow) 1f else -1f
                    window.resetTitle(directionY = returnDirection)
                    val windowBounds = window.getWindowBoundsOnScreen()
                    window.fullscreenPreview?.cancel(windowBounds)
                }
            }
            activeAction = thresholdAction
        }
    }

    private fun captureBaseWindowSize() {
        val layout = window.freeformLayout ?: return
        baseWindowHeight = layout.height
    }

    private fun resetWindowHostBounds() {
        val host = window.freeformWindowView ?: return
        window.windowParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
        window.windowParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        runCatching { window.windowManager.updateViewLayout(host, window.windowParams) }
    }

    private fun dispatchGesture(deltaX: Float, deltaY: Float, velocityY: Float) {
        val flingVelocityPx = dp(FLING_VELOCITY_DP).toFloat()
        val isFlingDown = velocityY > flingVelocityPx && deltaY > touchSlop
        val isFlingUp = velocityY < -flingVelocityPx && deltaY < -touchSlop

        if (abs(deltaX) < touchSlop && abs(deltaY) < touchSlop) {
            dispatch(PillAction.Back)
            return
        }

        val triggeredAction = activeAction
        when {
            triggeredAction == PillAction.EnterFullscreen || isFlingDown -> {
                dispatch(PillAction.EnterFullscreen)
            }
            triggeredAction == PillAction.CloseWindow || isFlingUp -> {
                dispatch(PillAction.CloseWindow)
            }
            deltaY > swipeThreshold && abs(deltaY) > abs(deltaX) -> {
                dispatch(PillAction.EnterFullscreen)
            }
            deltaY < -swipeThreshold && abs(deltaY) > abs(deltaX) -> {
                dispatch(PillAction.CloseWindow)
            }
            else -> animateBackToIdle()
        }
    }

    private fun dispatch(action: PillAction) {
        activeAction = null
        when (action) {
            PillAction.CloseWindow -> {
                pillView.performHapticFeedback(HapticFeedbackConstants.REJECT)
                window.updateTitle(FreeformWindow.TITLE_CLOSE, window.closeIcon, directionY = -1f)
                animateCloseThenRun { window.close() }
            }
            PillAction.EnterFullscreen -> {
                pillView.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                window.updateTitle(FreeformWindow.TITLE_OPEN_FULL_SCREEN, window.openFullScreenIcon, directionY = 1f)
                animateFullscreenThenRun { window.enterFullscreen() }
            }
            PillAction.Back -> {
                pillView.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                animateBackToIdle()
                window.goBack()
            }
        }
    }

    private fun animateCloseThenRun(endAction: () -> Unit) {
        val layout = window.freeformLayout ?: return endAction()
        window.fullscreenPreview?.cancel(immediate = true)
        layout.animate()
            .translationYBy(-dp(24).toFloat())
            .alpha(0f)
            .scaleX(0.01f)
            .scaleY(0.01f)
            .setInterpolator(FreeformWindow.EXPRESSIVE_EFFECTS_INTERPOLATOR)
            .setDuration(150L)
            .withEndAction {
                resetWindowHostBounds()
                endAction()
            }
            .start()
    }

    private fun animateFullscreenThenRun(endAction: () -> Unit) {
        val layout = window.freeformLayout ?: return endAction()
        pillView.animate().translationY(0f).setDuration(120L).start()
        window.fullscreenPreview?.commitFullscreen()
        endAction()
        layout.animate()
            .scaleX(1.06f)
            .scaleY(1.06f)
            .alpha(0f)
            .setInterpolator(FreeformWindow.EXPRESSIVE_EFFECTS_INTERPOLATOR)
            .setDuration(160L)
            .withEndAction {
                layout.alpha = 1f
                layout.scaleX = 1f
                layout.scaleY = 1f
                layout.translationY = 0f
                resetWindowHostBounds()
            }
            .start()
    }

    private fun animateBackToIdle() {
        val layout = window.freeformLayout ?: return
        val wasAction = activeAction
        activeAction = null
        val returnDirection = if (wasAction == PillAction.CloseWindow) 1f else -1f
        window.resetTitle(directionY = returnDirection)

        val windowBounds = window.getWindowBoundsOnScreen(targetIdle = true)
        window.fullscreenPreview?.cancel(windowBounds)

        pillView.animate()
            .translationY(0f)
            .setInterpolator(FreeformWindow.EXPRESSIVE_SPATIAL_INTERPOLATOR)
            .setDuration(180L)
            .start()

        val idleWidth = dp(PILL_IDLE_WIDTH_DP)
        if (pillView.layoutParams.width != idleWidth) {
            ValueAnimator.ofInt(pillView.layoutParams.width, idleWidth).apply {
                duration = 180L
                interpolator = FreeformWindow.EXPRESSIVE_SPATIAL_INTERPOLATOR
                addUpdateListener { animator ->
                    pillView.layoutParams = pillView.layoutParams.apply { width = animator.animatedValue as Int }
                }
                start()
            }
        }

        layout.animate()
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setInterpolator(FreeformWindow.EXPRESSIVE_SPATIAL_INTERPOLATOR)
            .setDuration(220L)
            .withEndAction { resetWindowHostBounds() }
            .start()
    }

    private fun updateAppearance() {
        val isDarkTheme = (pillView.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val appearance = PillAppearanceResolver.resolve(isDarkTheme)
        pillView.background = capsuleDrawable(appearance.pillColor, dp(2), appearance.outlineColor)
        plateView.background = capsuleDrawable(appearance.plateColor, 0, Color.TRANSPARENT)
        plateView.animate().alpha(if (appearance.showPlate) 1f else 0f).setDuration(120L).start()
    }

    private fun animatePressed(pressed: Boolean) {
        val targetWidth = dp(if (pressed) PILL_ACTIVE_WIDTH_DP else PILL_IDLE_WIDTH_DP)
        ValueAnimator.ofInt(pillView.layoutParams.width, targetWidth).apply {
            duration = 150L
            interpolator = FreeformWindow.EXPRESSIVE_SPATIAL_INTERPOLATOR
            addUpdateListener { animator ->
                pillView.layoutParams = pillView.layoutParams.apply { width = animator.animatedValue as Int }
            }
            start()
        }
        pillView.animate()
            .alpha(if (pressed) 1f else 0.85f)
            .scaleX(if (pressed) 1.12f else 1f)
            .scaleY(if (pressed) 1.12f else 1f)
            .setInterpolator(FreeformWindow.EXPRESSIVE_SPATIAL_INTERPOLATOR)
            .setDuration(150L)
            .start()
    }

    private fun capsuleDrawable(color: Int, strokeWidth: Int, strokeColor: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(99).toFloat()
            setColor(color)
            if (strokeWidth > 0) setStroke(strokeWidth, strokeColor)
        }
    }

    private fun recycleVelocityTracker() {
        velocityTracker?.recycle()
        velocityTracker = null
    }

    private fun dp(value: Int): Int = (value * pillView.resources.displayMetrics.density).roundToInt()
}

/**
 * Pin freeform
 */
class PinClickListener(private val window: FreeformWindow): View.OnClickListener {
    override fun onClick(v: View) {
        v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        window.handler.post {
            window.handleHangUp()
        }
    }
}

class RightViewClickListener(private val displayId: Int) : View.OnClickListener {
    override fun onClick(v: View) {
        v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        LMOFreeformServiceHolder.back(displayId)
    }
}

class ScaleTouchListener(private val window: FreeformWindow, private val isRight: Boolean = true): View.OnTouchListener {
    private var startX = 0.0f
    private var startY = 0.0f
    private var initialWidth = 0
    private var initialHeight = 0
    private var initialWindowX = 0
    private var initialWindowY = 0
    private var isPinTriggered = false

    companion object {
        private const val PIN_THRESHOLD_DP = 160
        private const val MIN_WINDOW_SIZE_DP = 110
    }

    private fun dp(value: Int): Int =
        (value * window.context.resources.displayMetrics.density).roundToInt()

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        val rootView = window.freeformRootView ?: return true
        val veilView = window.veilView ?: return true
        val freeformView = window.freeformView ?: return true
        
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.rawX
                startY = event.rawY
                isPinTriggered = false
                val currentWidth = if (rootView.layoutParams.width > 0) rootView.layoutParams.width else rootView.width
                val currentHeight = if (rootView.layoutParams.height > 0) rootView.layoutParams.height else rootView.height
                initialWidth = currentWidth
                initialHeight = currentHeight
                initialWindowX = window.windowParams.x
                initialWindowY = window.windowParams.y
                rootView.visibility = View.INVISIBLE
                veilView.visibility = View.VISIBLE
            }
            MotionEvent.ACTION_MOVE -> {
                val layout = window.freeformWindowView ?: window.freeformLayout ?: return true
                val rawXDelta = event.rawX - startX
                val rawYDelta = event.rawY - startY
                val xDelta = if (isRight) rawXDelta else -rawXDelta
                val yDelta = rawYDelta

                val pinThresholdWidth = max(dp(PIN_THRESHOLD_DP), window.freeformConfig.hangUpWidth)
                val pinThresholdHeight = max(dp(PIN_THRESHOLD_DP), window.freeformConfig.hangUpHeight)
                val minAllowedWidth = max(dp(MIN_WINDOW_SIZE_DP), (pinThresholdWidth * 0.75f).roundToInt())
                val minAllowedHeight = max(dp(MIN_WINDOW_SIZE_DP), (pinThresholdHeight * 0.75f).roundToInt())

                val density = window.context.resources.displayMetrics.density
                val chromeH = (104 * density).roundToInt()
                val safeTopMargin = (28 * density).roundToInt()
                val safeBottomMargin = (24 * density).roundToInt()
                val maxAllowedHeight = max(minAllowedHeight, window.defaultDisplayHeight - chromeH - safeTopMargin - safeBottomMargin)
                val maxAllowedWidth = max(minAllowedWidth, (window.defaultDisplayWidth * 0.9).roundToInt() - (32 * density).roundToInt())

                val effXDelta = if (initialWidth <= pinThresholdWidth && xDelta < 0) xDelta * 0.3f else xDelta
                val effYDelta = if (initialHeight <= pinThresholdHeight && yDelta < 0) yDelta * 0.3f else yDelta

                var targetWidth = (initialWidth + effXDelta).roundToInt().coerceIn(minAllowedWidth, maxAllowedWidth)
                var targetHeight = (initialHeight + effYDelta).roundToInt().coerceIn(minAllowedHeight, maxAllowedHeight)

                rootView.layoutParams = rootView.layoutParams.apply {
                    width = targetWidth
                    height = targetHeight
                }

                val deltaW = targetWidth - initialWidth
                val deltaH = targetHeight - initialHeight
                val shiftX = if (isRight) ((deltaW / 2.0f).roundToInt()) else -((deltaW / 2.0f).roundToInt())
                val shiftY = (deltaH / 2.0f).roundToInt()

                window.windowParams.x = initialWindowX + shiftX
                window.windowParams.y = initialWindowY + shiftY
                window.windowManager.updateViewLayout(layout, window.windowParams)

                val shouldTriggerPin = targetWidth <= pinThresholdWidth && targetHeight <= pinThresholdHeight
                if (shouldTriggerPin != isPinTriggered) {
                    isPinTriggered = shouldTriggerPin
                    v.performHapticFeedback(HapticFeedbackConstants.SEGMENT_TICK)
                    if (isPinTriggered) {
                        window.updateTitle(FreeformWindow.TITLE_PIN, window.pinIcon, directionY = -1f)
                    } else {
                        window.resetTitle(directionY = 1f)
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                if (isPinTriggered) {
                    v.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    isPinTriggered = false
                    window.resetTitle(animated = false)
                    rootView.visibility = View.VISIBLE
                    veilView.visibility = View.GONE
                    window.handler.post { window.handleHangUp() }
                    return true
                }

                freeformView.surfaceTexture?.let { surfaceTexture ->
                    window.freeformConfig.width = rootView.layoutParams.width
                    window.freeformConfig.height = rootView.layoutParams.height
                    window.handler.post { window.makeSureFreeformInScreen() }
                    window.measureScale()
                    LMOFreeformServiceHolder.resizeFreeform(
                        window,
                        window.freeformConfig.freeformWidth,
                        window.freeformConfig.freeformHeight,
                        window.freeformConfig.densityDpi
                    )
                    surfaceTexture.setDefaultBufferSize(window.freeformConfig.freeformWidth, window.freeformConfig.freeformHeight)
                    // Delay the unveiling until after the scaling is complete
                    window.handler.postDelayed({
                        rootView.visibility = View.VISIBLE
                        veilView.visibility = View.GONE
                    }, 250)
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                if (isPinTriggered) {
                    isPinTriggered = false
                    window.resetTitle(animated = false)
                }
                val layout = window.freeformWindowView ?: window.freeformLayout
                window.windowParams.x = initialWindowX
                window.windowParams.y = initialWindowY
                if (layout != null) {
                    window.windowManager.updateViewLayout(layout, window.windowParams)
                }
                rootView.layoutParams = rootView.layoutParams.apply {
                    width = window.freeformConfig.width
                    height = window.freeformConfig.height
                }
                rootView.visibility = View.VISIBLE
                veilView.visibility = View.GONE
            }
        }
        return true
    }
}

class HangUpGestureListener(private val window: FreeformWindow) : SimpleOnGestureListener() {
    private var startX = 0
    private var startY = 0
    var isDragging = false

    override fun onDown(e: MotionEvent): Boolean {
        startX = window.windowParams.x
        startY = window.windowParams.y
        isDragging = false
        return super.onDown(e)
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        isDragging = false
        window.handler.post { window.handleHangUp() }
        return true
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        isDragging = true
        if (!isValidMotionEvent(e1) || !isValidMotionEvent(e2)) {
            return true
        }

        val e1RawX = e1?.rawX ?: 0f
        val e1RawY = e1?.rawY ?: 0f

        if (!isValidCoordinate(e1RawX) || !isValidCoordinate(e1RawY) 
                || !isValidCoordinate(e2.rawX) || !isValidCoordinate(e2.rawY)) {
            return true
        }
        
        val newX = (startX + e2.rawX - e1RawX).roundToInt()
        val newY = (startY + e2.rawY - e1RawY).roundToInt()

        try {
            window.handler.post {
                val layout = window.freeformWindowView ?: window.freeformLayout ?: return@post
                window.windowManager.updateViewLayout(layout, window.windowParams.apply {
                    x = newX
                    y = newY
                })
            }
        } catch (e: Exception) {}
        return true
    }

    fun isValidMotionEvent(event: MotionEvent?): Boolean {
        return event != null &&
                !event.rawX.isNaN() &&
                !event.rawY.isNaN() &&
                event.rawX.isFinite() &&
                event.rawY.isFinite()
    }
    
    fun isValidCoordinate(coordinate: Float): Boolean {
        return !coordinate.isNaN() && coordinate.isFinite()
    }
}
