package com.hassan.colorcraft.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ComposeShader
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class HsvColorPickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var selectionHue: Float = 0f
    private var selectionSaturation: Float = 1f
    private var selectionX: Float = 0f
    private var selectionY: Float = 0f

    private var pendingHue: Float? = null
    private var pendingSaturation: Float? = null

    private val gradientBounds = RectF()
    private val gradientPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val indicatorFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val indicatorStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        style = Paint.Style.STROKE
        strokeWidth = 2f * resources.displayMetrics.density
    }

    private val indicatorRadiusPx = 7f * resources.displayMetrics.density

    var onColorSelected: ((hue: Float, saturation: Float) -> Unit)? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return

        gradientBounds.set(0f, 0f, w.toFloat(), h.toFloat())

        val hueColors = IntArray(13)
        for (i in hueColors.indices) {
            val hue = i * 30f
            hueColors[i] = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
        }

        val hueShader = LinearGradient(
            0f, 0f, w.toFloat(), 0f,
            hueColors, null,
            Shader.TileMode.CLAMP
        )

        // Transparent at top / white at bottom, matching the 100%-at-top-to-0%-at-bottom
        // saturation mapping used by the touch handling below.
        val saturationShader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            Color.TRANSPARENT, Color.WHITE,
            Shader.TileMode.CLAMP
        )

        gradientPaint.shader = ComposeShader(hueShader, saturationShader, PorterDuff.Mode.SRC_OVER)

        val hue = pendingHue ?: selectionHue
        val saturation = pendingSaturation ?: selectionSaturation
        applySelection(hue, saturation)
        pendingHue = null
        pendingSaturation = null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return

        canvas.drawRect(gradientBounds, gradientPaint)
        canvas.drawCircle(selectionX, selectionY, indicatorRadiusPx, indicatorFillPaint)
        canvas.drawCircle(selectionX, selectionY, indicatorRadiusPx, indicatorStrokePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                if (width <= 0 || height <= 0) return true

                val clampedX = event.x.coerceIn(0f, width.toFloat())
                val clampedY = event.y.coerceIn(0f, height.toFloat())
                selectionX = clampedX
                selectionY = clampedY

                val hue = (clampedX / width * 360f).coerceIn(0f, 360f)
                val saturation = (1f - (clampedY / height)).coerceIn(0f, 1f)
                selectionHue = hue
                selectionSaturation = saturation

                invalidate()
                onColorSelected?.invoke(hue, saturation)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    fun setSelection(hue: Float, saturation: Float) {
        if (width <= 0 || height <= 0) {
            pendingHue = hue
            pendingSaturation = saturation
            return
        }
        applySelection(hue, saturation)
    }

    private fun applySelection(hue: Float, saturation: Float) {
        selectionHue = hue.coerceIn(0f, 360f)
        selectionSaturation = saturation.coerceIn(0f, 1f)
        selectionX = (selectionHue / 360f * width).coerceIn(0f, width.toFloat())
        selectionY = ((1f - selectionSaturation) * height).coerceIn(0f, height.toFloat())
        invalidate()
    }
}
