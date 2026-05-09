package com.example.recipefood.ui.history

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.ViewTreeObserver
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.recipefood.data.MealRepository
import com.example.recipefood.data.RecipeDatabase
import com.example.recipefood.data.UserSettingsRepository
import com.example.recipefood.databinding.ActivityHistoryBinding
import com.example.recipefood.adapters.MealHistoryAdapter
import com.example.recipefood.model.SavedMeal
import com.example.recipefood.ui.mealdetail.MealDetailActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*
import com.example.recipefood.R

class HistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHistoryBinding
    private lateinit var repository: MealRepository
    private lateinit var settingsRepository: UserSettingsRepository
    private lateinit var adapter: MealHistoryAdapter
    private var selectedDate: Date = Date()
    private var loadMealsJob: Job? = null
    private var calorieGoal: Double = 2000.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = RecipeDatabase.getDatabase(this)
        repository = MealRepository(database.mealDao())
        settingsRepository = UserSettingsRepository(database.userSettingsDao())

        setupProgressBarBackground()

        setupToolbar()
        setupCalendar()
        setupRecyclerView()
        loadCalorieGoal()
        loadMealsForDate(selectedDate)
    }

    private fun setupProgressBarBackground() {
        val container = binding.progressBarContainer
        val bg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 24f
            setColor(Color.parseColor("#1A1A1A"))
        }
        container.background = bg
    }

    private fun loadCalorieGoal() {
        lifecycleScope.launch {
            val settings = settingsRepository.getUserSettingsSync()
            calorieGoal = if (settings != null && settings.targetCalories > 0) settings.targetCalories else 2000.0
        }
    }

    private fun setupToolbar() {
        // toolbar is hidden in v3 layout; use back link instead
        try {
            binding.btnBackLink.setOnClickListener { finish() }
        } catch (_: Exception) {}
    }

    private fun setupCalendar() {
        val tvCurrentDateTop = binding.root.findViewById<android.widget.TextView>(R.id.tvCurrentDate)
        val tvBottomDate = binding.root.findViewById<android.widget.TextView>(R.id.tvBottomDate)
        val btnPrevDay = binding.root.findViewById<android.widget.TextView>(R.id.btnPrevDay)
        val btnNextDay = binding.root.findViewById<android.widget.TextView>(R.id.btnNextDay)

        val showDatePicker = {
            val calendar = Calendar.getInstance()
            calendar.time = selectedDate
            
            val dialog = android.app.DatePickerDialog(this, { _, y, m, d ->
                calendar.set(y, m, d)
                selectedDate = calendar.time
                updateDateText(tvCurrentDateTop, tvBottomDate)
                loadMealsForDate(selectedDate)
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
            
            dialog.datePicker.maxDate = System.currentTimeMillis()
            dialog.show()
        }

        tvCurrentDateTop?.setOnClickListener { showDatePicker() }
        tvBottomDate?.setOnClickListener { showDatePicker() }

        btnPrevDay?.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.time = selectedDate
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            selectedDate = calendar.time
            updateDateText(tvCurrentDateTop, tvBottomDate)
            loadMealsForDate(selectedDate)
        }

        btnNextDay?.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.time = selectedDate
            
            // Limit next day to today max
            val today = Calendar.getInstance()
            if (calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
                return@setOnClickListener // Already today
            }
            
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            selectedDate = calendar.time
            updateDateText(tvCurrentDateTop, tvBottomDate)
            loadMealsForDate(selectedDate)
        }
        
        updateDateText(tvCurrentDateTop, tvBottomDate)
    }

    private fun updateDateText(tvTop: android.widget.TextView?, tvBottom: android.widget.TextView?) {
        val today = Calendar.getInstance()
        val selected = Calendar.getInstance().apply { time = selectedDate }
        
        val dateString = if (today.get(Calendar.YEAR) == selected.get(Calendar.YEAR) &&
            today.get(Calendar.DAY_OF_YEAR) == selected.get(Calendar.DAY_OF_YEAR)) {
            "Сьогодні"
        } else {
            val format = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale("uk", "UA"))
            format.format(selectedDate)
        }
        
        tvTop?.text = dateString
        tvBottom?.text = dateString
    }

    private fun setupRecyclerView() {
        adapter = MealHistoryAdapter(
            onItemClick = { meal ->
                val intent = Intent(this, MealDetailActivity::class.java)
                intent.putExtra("meal_id", meal.id)
                startActivity(intent)
            },
            onDeleteClick = { meal ->
                deleteMeal(meal)
            }
        )

        binding.rvMeals.layoutManager = LinearLayoutManager(this)
        binding.rvMeals.adapter = adapter
    }

    private fun loadMealsForDate(date: Date) {
        loadMealsJob?.cancel()
        loadMealsJob = lifecycleScope.launch {
            repository.getMealsByDate(date).collectLatest { meals ->
                adapter.submitList(meals)
                updateStatistics(meals)
            }
        }
    }

    private fun updateStatistics(meals: List<SavedMeal>) {
        val totalCalories = meals.sumOf { it.totalCalories }
        val totalProteins = meals.sumOf { it.totalProteins }
        val totalFats = meals.sumOf { it.totalFats }
        val totalCarbs = meals.sumOf { it.totalCarbs }

        val goalLabel = if (calorieGoal > 0) "${totalCalories.toInt()} / ${calorieGoal.toInt()} ккал"
                        else "${totalCalories.toInt()} ккал"
        binding.tvTotalCalories.text = goalLabel
        binding.tvTotalProteins.text = String.format("%.1f г", totalProteins)
        binding.tvTotalFats.text = String.format("%.1f г", totalFats)
        binding.tvTotalCarbs.text = String.format("%.1f г", totalCarbs)

        updateProgressBar(totalCalories)
        updateChart(totalProteins, totalFats, totalCarbs)

        binding.statsCard.visibility = android.view.View.VISIBLE
        if (meals.isEmpty()) {
            binding.tvEmptyMessage.visibility = android.view.View.VISIBLE
        } else {
            binding.tvEmptyMessage.visibility = android.view.View.GONE
        }
    }

    private fun updateProgressBar(totalCalories: Double) {
        val container = binding.progressBarContainer
        val fill = binding.progressFill

        val ratio = if (calorieGoal > 0) (totalCalories / calorieGoal).coerceIn(0.0, 1.0) else 0.0
        val fillColor = when {
            ratio < 0.75 -> Color.parseColor("#4CAF50")
            ratio < 1.0  -> Color.parseColor("#FF9800")
            else         -> Color.parseColor("#F44336")
        }

        val fillDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 24f
            setColor(fillColor)
        }
        fill.background = fillDrawable

        if (container.width > 0) {
            val params = fill.layoutParams
            params.width = (container.width * ratio).toInt()
            fill.layoutParams = params
        } else {
            container.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    container.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    val totalWidth = container.width
                    val params = fill.layoutParams
                    params.width = (totalWidth * ratio).toInt()
                    fill.layoutParams = params
                }
            })
        }
    }

    private fun updateChart(proteins: Double, fats: Double, carbs: Double) {
        // PieChart is hidden in v3 layout; guard access
        try {
            if (binding.pieChart.visibility != android.view.View.VISIBLE) return

            val total = proteins + fats + carbs
            if (total > 0) {
                val proteinPercent = (proteins / total * 100).toFloat()
                val fatPercent = (fats / total * 100).toFloat()
                val carbPercent = (carbs / total * 100).toFloat()

                val entries = listOf(
                    com.github.mikephil.charting.data.PieEntry(proteinPercent, "Білки"),
                    com.github.mikephil.charting.data.PieEntry(fatPercent, "Жири"),
                    com.github.mikephil.charting.data.PieEntry(carbPercent, "Вуглеводи")
                )

                val dataSet = com.github.mikephil.charting.data.PieDataSet(entries, "")
                dataSet.colors = listOf(
                    Color.WHITE,
                    Color.parseColor("#B0B0B0"),
                    Color.parseColor("#666666")
                )
                dataSet.valueTextSize = 12f
                dataSet.valueTextColor = Color.BLACK
                dataSet.setDrawValues(true)

                val data = com.github.mikephil.charting.data.PieData(dataSet)
                data.setValueFormatter(object : com.github.mikephil.charting.formatter.ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return if (value < 1f) "" else "${value.toInt()}%"
                    }
                })

                binding.pieChart.data = data
                binding.pieChart.description.isEnabled = false
                binding.pieChart.setDrawEntryLabels(false)
                binding.pieChart.isRotationEnabled = false
                binding.pieChart.setHoleColor(Color.TRANSPARENT)
                binding.pieChart.holeRadius = 0f
                binding.pieChart.transparentCircleRadius = 0f
                binding.pieChart.legend.textColor = Color.WHITE
                binding.pieChart.legend.textSize = 14f
                binding.pieChart.legend.formSize = 12f
                binding.pieChart.animateY(800)
                binding.pieChart.invalidate()
            }
        } catch (_: Exception) {}
    }

    private fun deleteMeal(meal: SavedMeal) {
        lifecycleScope.launch {
            repository.deleteMeal(meal)
            loadMealsForDate(selectedDate)
        }
    }
}
