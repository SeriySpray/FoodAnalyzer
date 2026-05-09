package com.example.recipefood.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.recipefood.R
import com.example.recipefood.ui.history.HistoryActivity
import com.example.recipefood.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

class DashboardActivity : AppCompatActivity() {

    private lateinit var viewModel: DashboardViewModel

    private lateinit var dateTextView: TextView
    private lateinit var caloriesTodayTextView: TextView
    private lateinit var caloriesGoalTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        dateTextView = findViewById(R.id.dateTextView)
        caloriesTodayTextView = findViewById(R.id.caloriesTodayTextView)
        
        val progressBarContainer = findViewById<android.widget.FrameLayout>(R.id.progressBarContainer)
        val progressFill = findViewById<android.view.View>(R.id.progressFill)
        
        caloriesGoalTextView = findViewById(R.id.caloriesGoalTextView)
        val proteinsTextView = findViewById<TextView>(R.id.proteinsTextView)
        val fatsTextView = findViewById<TextView>(R.id.fatsTextView)
        val carbsTextView = findViewById<TextView>(R.id.carbsTextView)
        
        val proteinsGoalTextView = findViewById<TextView>(R.id.proteinsGoalTextView)
        val fatsGoalTextView = findViewById<TextView>(R.id.fatsGoalTextView)
        val carbsGoalTextView = findViewById<TextView>(R.id.carbsGoalTextView)

        val proteinsCircular = findViewById<com.google.android.material.progressindicator.CircularProgressIndicator>(R.id.proteinsCircular)
        val fatsCircular = findViewById<com.google.android.material.progressindicator.CircularProgressIndicator>(R.id.fatsCircular)
        val carbsCircular = findViewById<com.google.android.material.progressindicator.CircularProgressIndicator>(R.id.carbsCircular)

        val streakTextView = findViewById<TextView>(R.id.streakTextView)
        // viewHistoryButton was here

        val bg = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 24f
            setColor(android.graphics.Color.parseColor("#1A1A1A"))
        }
        progressBarContainer.background = bg

        val dateFormat = SimpleDateFormat("EEEE, d MMMM", Locale("uk"))
        dateTextView.text = dateFormat.format(Date())

        viewModel = ViewModelProvider(this)[DashboardViewModel::class.java]

        viewModel.state.observe(this) { state ->
            caloriesTodayTextView.text = "${state.todayCalories.toInt()} ккал"

            val ratio = if (state.targetCalories > 0) {
                (state.todayCalories / state.targetCalories).coerceIn(0.0, 1.0)
            } else 0.0

            val fillColor = when {
                ratio < 0.75 -> android.graphics.Color.parseColor("#4CAF50")
                ratio < 1.0  -> android.graphics.Color.parseColor("#FF9800")
                else         -> android.graphics.Color.parseColor("#F44336")
            }

            val fillDrawable = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 24f
                setColor(fillColor)
            }
            progressFill.background = fillDrawable

            if (progressBarContainer.width > 0) {
                val params = progressFill.layoutParams
                params.width = (progressBarContainer.width * ratio).toInt()
                progressFill.layoutParams = params
            } else {
                progressBarContainer.viewTreeObserver.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        progressBarContainer.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        val totalWidth = progressBarContainer.width
                        val params = progressFill.layoutParams
                        params.width = (totalWidth * ratio).toInt()
                        progressFill.layoutParams = params
                    }
                })
            }

            caloriesGoalTextView.text = getString(R.string.of_goal, state.targetCalories.toInt())

            proteinsTextView.text = "${state.todayProteins.toInt()}"
            fatsTextView.text = "${state.todayFats.toInt()}"
            carbsTextView.text = "${state.todayCarbs.toInt()}"
            
            proteinsGoalTextView.text = "з ${state.targetProteins.toInt()}г"
            fatsGoalTextView.text = "з ${state.targetFats.toInt()}г"
            carbsGoalTextView.text = "з ${state.targetCarbs.toInt()}г"

            val pRatio = if (state.targetProteins > 0) ((state.todayProteins / state.targetProteins) * 100).toInt() else 0
            val fRatio = if (state.targetFats > 0) ((state.todayFats / state.targetFats) * 100).toInt() else 0
            val cRatio = if (state.targetCarbs > 0) ((state.todayCarbs / state.targetCarbs) * 100).toInt() else 0

            proteinsCircular.progress = min(pRatio, 100)
            fatsCircular.progress = min(fRatio, 100)
            carbsCircular.progress = min(cRatio, 100)

            streakTextView.text = state.streak.toString()
        }

        // viewHistoryButton was here
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
