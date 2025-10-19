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

    private var minCalories = 0.0
    private var maxCalories = 0.0
    private var currentCalories = 0.0

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.gray_medium)
        style = Paint.Style.FILL
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.white)
        style = Paint.Style.FILL
    }

    private val warningPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.FILL
        textSize = 48f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.white)
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    fun setCaloriesData(min: Double, max: Double, current: Double) {
        minCalories = min
        maxCalories = max
        currentCalories = current
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cornerRadius = height / 2f
        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())

        // Малюємо фон
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, backgroundPaint)

        if (maxCalories > minCalories && maxCalories > 0) {
            val isExceeded = currentCalories > maxCalories

            // Розраховуємо прогрес відносно діапазону
            val progress = when {
                currentCalories < minCalories -> {
                    // Менше мінімуму - показуємо прогрес до мінімуму
                    (currentCalories / minCalories).coerceIn(0.0, 1.0)
                }
                else -> {
                    // В діапазоні або перевищено - показуємо повний прогрес
                    1.0
                }
            }

            val progressWidth = (width * progress).toFloat()

            // Малюємо основний прогрес
            if (progressWidth > 0) {
                val progressRect = RectF(0f, 0f, progressWidth, height.toFloat())

                // Вибираємо колір залежно від статусу
                val paint = when {
                    currentCalories < minCalories -> progressPaint.apply {
                        alpha = 150 // Напівпрозорий, якщо не досягнуто мінімуму
                    }
                    else -> progressPaint.apply {
                        alpha = 255 // Повністю білий
                    }
                }

                canvas.drawRoundRect(progressRect, cornerRadius, cornerRadius, paint)
            }

            // Якщо перевищено норму - малюємо знак оклику по центру
            if (isExceeded) {
                val centerX = width / 2f
                val centerY = height / 2f

                // Малюємо знак оклику
                canvas.drawText("Норму перевищено!", centerX, centerY + (warningPaint.textSize / 3), warningPaint)
            }

            // Малюємо текстові позначки
            val minText = "${minCalories.toInt()}"
            val maxText = "${maxCalories.toInt()}"

            // Позначка мінімуму (ліворуч внизу прогрес-бару)
            canvas.drawText(minText, 40f, height + 40f, textPaint.apply {
                textSize = 24f
                textAlign = Paint.Align.LEFT
                color = ContextCompat.getColor(context, R.color.gray_text)
            })

            // Позначка максимуму (праворуч внизу прогрес-бару)
            canvas.drawText(maxText, width - 40f, height + 40f, textPaint.apply {
                textSize = 24f
                textAlign = Paint.Align.RIGHT
                color = ContextCompat.getColor(context, R.color.gray_text)
            })

            // Якщо перевищено - показуємо попередження по центру
            if (isExceeded) {
                val excessText = "+${(currentCalories - maxCalories).toInt()}"
                canvas.drawText(excessText, width / 2f, height + 40f, textPaint.apply {
                    textSize = 24f
                    textAlign = Paint.Align.CENTER
                    color = Color.parseColor("#FF6B6B")
                    isFakeBoldText = true
                })
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = MeasureSpec.getSize(heightMeasureSpec) + 60 // Додаткове місце для тексту
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(desiredHeight, MeasureSpec.EXACTLY))
    }
}