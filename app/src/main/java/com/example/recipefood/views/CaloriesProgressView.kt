package com.example.recipefood.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.recipefood.R

class CaloriesProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var targetCalories = 0.0
    private var currentCalories = 0.0

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        backgroundPaint.color = ContextCompat.getColor(context, R.color.progress_bg)
        fillPaint.color = ContextCompat.getColor(context, R.color.progress_fill)
    }

    fun setCaloriesData(target: Double, current: Double) {
        targetCalories = target
        currentCalories = current
        backgroundPaint.color = ContextCompat.getColor(context, R.color.progress_bg)
        fillPaint.color = ContextCompat.getColor(context, R.color.progress_fill)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cornerRadius = height / 2f
        val bgRect = RectF(0f, 0f, width.toFloat(), height.toFloat())

        canvas.drawRoundRect(bgRect, cornerRadius, cornerRadius, backgroundPaint)

        if (targetCalories <= 0) return

        val progress = (currentCalories / targetCalories).coerceIn(0.0, 1.0)
        val progressWidth = (width * progress).toFloat()

        if (progressWidth <= 0) return

        val fillRect = RectF(0f, 0f, progressWidth, height.toFloat())
        canvas.drawRoundRect(fillRect, cornerRadius, cornerRadius, fillPaint)
    }
}
