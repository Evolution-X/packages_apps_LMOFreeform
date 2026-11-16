package com.libremobileos.freeform.server.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import kotlin.math.max
import kotlin.math.roundToInt

@SuppressLint("ViewConstructor")
class FreeformFullscreenPreview(
    context: Context,
    private val windowManager: WindowManager,
    private val resourceHolder: RemoteResourceHolder
) : View(context) {

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val startRect = Rect()
    private val targetRect = Rect()
    private val currentRect = RectF()

    private var startRadius = 0f
    private var currentRadius = 0f
    private var progress = 0f
    private var isAttached = false
    private var isTriggered = false
    private var currentAnimator: ValueAnimator? = null

    private val windowParams = WindowManager.LayoutParams().apply {
        type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        width = WindowManager.LayoutParams.MATCH_PARENT
        height = WindowManager.LayoutParams.MATCH_PARENT
        flags = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        setFitInsetsTypes(0)
        setFitInsetsSides(0)
        renderingHints = renderingHints or WindowManager.LayoutParams.RENDERING_HINT_FORCE_HARDWARE_ACCELERATED
        format = PixelFormat.TRANSLUCENT
        gravity = Gravity.TOP or Gravity.START
    }

    init {
        visibility = GONE
        systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        )
        updateColors()
    }

    fun updateColors() {
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val baseColor = resourceHolder.getColor("color_primary_container").takeIf { it != 0 }
            ?: resourceHolder.getColor("color_surface_highest").takeIf { it != 0 }
            ?: resourceHolder.getColor("color_surface_high").takeIf { it != 0 }
            ?: if (isDark) Color.parseColor("#4F378B") else Color.parseColor("#EADDFF")

        val outlineColor = resourceHolder.getColor("color_outline_variant").takeIf { it != 0 }
            ?: if (isDark) Color.parseColor("#49454F") else Color.parseColor("#CAC4D0")

        val density = resources.displayMetrics.density
        strokePaint.strokeWidth = density * 1.5f
        fillPaint.color = baseColor
        fillPaint.alpha = (255 * 0.70f).roundToInt()
        strokePaint.color = outlineColor
        strokePaint.alpha = (255 * 0.60f).roundToInt()
    }

    fun getRealScreenBounds(): Rect {
        val bounds = runCatching {
            windowManager.maximumWindowMetrics.bounds
        }.getOrNull()
        if (bounds != null && bounds.width() > 0 && bounds.height() > 0) {
            return Rect(0, 0, bounds.width(), bounds.height())
        }
        val dm = DisplayMetrics()
        context.display?.getRealMetrics(dm)
        val w = if (dm.widthPixels > 0) dm.widthPixels else resources.displayMetrics.widthPixels
        val h = if (dm.heightPixels > 0) dm.heightPixels else resources.displayMetrics.heightPixels
        return Rect(0, 0, w, h)
    }

    fun attach() {
        if (!isAttached) {
            runCatching {
                windowManager.addView(this, windowParams)
                isAttached = true
            }
        }
    }

    fun detach() {
        cancel(immediate = true)
        if (isAttached) {
            runCatching {
                windowManager.removeView(this)
                isAttached = false
            }
        }
    }

    fun triggerFullscreen(windowBounds: Rect, cornerRadiusPx: Float) {
        if (isTriggered && (visibility == VISIBLE || currentAnimator?.isRunning == true)) return
        isTriggered = true
        currentAnimator?.cancel()
        currentAnimator = null
        animate().cancel()
        updateColors()

        val realBounds = getRealScreenBounds()
        startRect.set(windowBounds)
        targetRect.set(0, 0, max(width, realBounds.width()), max(height, realBounds.height()))
        startRadius = cornerRadiusPx

        val fromR = RectF()
        val fromRad: Float
        val fromAlpha: Float
        if (visibility == VISIBLE && currentRect.width() > 0) {
            fromR.set(currentRect)
            fromRad = currentRadius
            fromAlpha = alpha
        } else {
            fromR.set(startRect)
            fromRad = startRadius
            fromAlpha = 1f
            currentRect.set(startRect)
            currentRadius = startRadius
        }
        visibility = VISIBLE

        currentAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 320L
            interpolator = FreeformWindow.EXPRESSIVE_SPATIAL_INTERPOLATOR
            addUpdateListener { anim ->
                val f = anim.animatedValue as Float
                currentRect.left = fromR.left + (targetRect.left - fromR.left) * f
                currentRect.top = fromR.top + (targetRect.top - fromR.top) * f
                currentRect.right = fromR.right + (targetRect.right - fromR.right) * f
                currentRect.bottom = fromR.bottom + (targetRect.bottom - fromR.bottom) * f

                currentRadius = max(0f, fromRad * (1f - f))
                strokePaint.alpha = ((1f - f) * 180).roundToInt().coerceIn(0, 255)
                alpha = (fromAlpha + (1f - fromAlpha) * f).coerceIn(0f, 1f)
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    currentAnimator = null
                    setProgress(1f)
                    alpha = 1f
                }
            })
            start()
        }
    }

    fun commitFullscreen() {
        isTriggered = false
        currentAnimator?.cancel()
        currentAnimator = null
        animate().cancel()
        setProgress(1f)
        visibility = VISIBLE
        animate()
            .alpha(0f)
            .setInterpolator(FreeformWindow.EXPRESSIVE_EFFECTS_INTERPOLATOR)
            .setDuration(180L)
            .withEndAction {
                visibility = GONE
                alpha = 1f
                setProgress(0f)
            }
            .start()
    }

    fun cancel(targetBounds: Rect? = null, immediate: Boolean = false) {
        if (immediate || visibility != VISIBLE) {
            isTriggered = false
            currentAnimator?.cancel()
            currentAnimator = null
            animate().cancel()
            visibility = GONE
            alpha = 1f
            setProgress(0f)
            return
        }

        if (targetBounds != null) {
            startRect.set(targetBounds)
        }

        // If already cancelling, just update target destination and let it finish smoothly
        if (!isTriggered && currentAnimator?.isRunning == true) {
            return
        }

        isTriggered = false
        currentAnimator?.cancel()
        currentAnimator = null
        animate().cancel()

        val fromR = RectF(currentRect)
        val fromRad = currentRadius
        val fromStrokeAlpha = strokePaint.alpha
        val startAlpha = alpha

        currentAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 250L
            interpolator = FreeformWindow.EXPRESSIVE_SPATIAL_INTERPOLATOR
            addUpdateListener { anim ->
                val f = anim.animatedValue as Float
                currentRect.left = fromR.left + (startRect.left - fromR.left) * f
                currentRect.top = fromR.top + (startRect.top - fromR.top) * f
                currentRect.right = fromR.right + (startRect.right - fromR.right) * f
                currentRect.bottom = fromR.bottom + (startRect.bottom - fromR.bottom) * f

                currentRadius = fromRad + (startRadius - fromRad) * f
                strokePaint.alpha = (fromStrokeAlpha + ((180 - fromStrokeAlpha) * f).roundToInt()).coerceIn(0, 255)
                alpha = (startAlpha * (1f - f * f)).coerceIn(0f, 1f)
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    currentAnimator = null
                    visibility = GONE
                    alpha = 1f
                    setProgress(0f)
                }
            })
            start()
        }
    }

    private fun setProgress(value: Float) {
        progress = value.coerceIn(0f, 1f)
        val p = progress

        currentRect.left = startRect.left + (targetRect.left - startRect.left) * p
        currentRect.top = startRect.top + (targetRect.top - startRect.top) * p
        currentRect.right = startRect.right + (targetRect.right - startRect.right) * p
        currentRect.bottom = startRect.bottom + (targetRect.bottom - startRect.bottom) * p

        currentRadius = max(0f, startRadius * (1f - p))
        strokePaint.alpha = ((1f - p) * 180).roundToInt().coerceIn(0, 255)

        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            targetRect.right = max(targetRect.right, w)
            targetRect.bottom = max(targetRect.bottom, h)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (visibility != VISIBLE) return
        canvas.drawRoundRect(currentRect, currentRadius, currentRadius, fillPaint)
        if (strokePaint.strokeWidth > 0 && strokePaint.alpha > 0 && currentRadius > 0f) {
            canvas.drawRoundRect(currentRect, currentRadius, currentRadius, strokePaint)
        }
    }
}
