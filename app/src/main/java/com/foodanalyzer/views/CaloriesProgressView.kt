package com.foodanalyzer.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.foodanalyzer.R

class CaloriesProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var targetCalories = 0.0
    private var deviationCalories = 0.0
    private var currentCalories = 0.0

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.gray_medium)
        style = Paint.Style.FILL
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    fun setCaloriesData(target: Double, deviation: Double, current: Double) {
        targetCalories = target
        deviationCalories = deviation
        currentCalories = current
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cornerRadius = height / 2f
        val bgRect = RectF(0f, 0f, width.toFloat(), height.toFloat())

        // Сірий фон
        canvas.drawRoundRect(bgRect, cornerRadius, cornerRadius, backgroundPaint)

        val maxAcceptable = targetCalories + deviationCalories
        if (maxAcceptable <= 0) return

        // Прогрес відносно максимально допустимого значення
        val progress = (currentCalories / maxAcceptable).coerceIn(0.0, 1.0)
        val progressWidth = (width * progress).toFloat()

        if (progressWidth <= 0) return

        val fillRect = RectF(0f, 0f, progressWidth, height.toFloat())
        canvas.drawRoundRect(fillRect, cornerRadius, cornerRadius, fillPaint)
    }
}
