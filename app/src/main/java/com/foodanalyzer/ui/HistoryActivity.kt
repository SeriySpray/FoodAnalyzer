package com.foodanalyzer.ui

import android.content.Intent
import android.os.Bundle
import android.widget.CalendarView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.foodanalyzer.database.AppDatabase
import com.foodanalyzer.databinding.ActivityHistoryBinding
import com.foodanalyzer.repository.MealRepository
import com.foodanalyzer.adapters.MealHistoryAdapter
import com.foodanalyzer.models.SavedMeal
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*

class HistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHistoryBinding
    private lateinit var repository: MealRepository
    private lateinit var adapter: MealHistoryAdapter
    private var selectedDate: Date = Date()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = AppDatabase.getDatabase(this)
        repository = MealRepository(database.mealDao())

        setupToolbar()
        setupCalendar()
        setupRecyclerView()
        loadMealsForDate(selectedDate)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Історія страв"
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupCalendar() {
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            selectedDate = calendar.time
            loadMealsForDate(selectedDate)
        }
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
        lifecycleScope.launch {
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

        binding.tvTotalCalories.text = String.format("%.1f ккал", totalCalories)
        binding.tvTotalProteins.text = String.format("%.1f г", totalProteins)
        binding.tvTotalFats.text = String.format("%.1f г", totalFats)
        binding.tvTotalCarbs.text = String.format("%.1f г", totalCarbs)

        // Оновлення діаграми
        updateChart(totalProteins, totalFats, totalCarbs)

        // Показати/сховати статистику
        if (meals.isEmpty()) {
            binding.statsCard.visibility = android.view.View.GONE
            binding.tvEmptyMessage.visibility = android.view.View.VISIBLE
        } else {
            binding.statsCard.visibility = android.view.View.VISIBLE
            binding.tvEmptyMessage.visibility = android.view.View.GONE
        }
    }

    private fun updateChart(proteins: Double, fats: Double, carbs: Double) {
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
                android.graphics.Color.parseColor("#FF6B35"), // Білки - помаранчевий
                android.graphics.Color.parseColor("#F7931E"), // Жири - жовтий
                android.graphics.Color.parseColor("#EEAECA")  // Вуглеводи - рожевий
            )
            dataSet.valueTextSize = 12f
            dataSet.valueTextColor = android.graphics.Color.WHITE

            val data = com.github.mikephil.charting.data.PieData(dataSet)
            data.setValueFormatter(object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return String.format("%.1f%%", value)
                }
            })

            binding.pieChart.data = data
            binding.pieChart.description.isEnabled = false

            //Підпис під діаграмою
            binding.pieChart.legend.textColor = android.graphics.Color.BLACK
            binding.pieChart.legend.textSize = 16f
            binding.pieChart.legend.formSize = 14f

            //Підпис на діаграмі
            binding.pieChart.setEntryLabelColor(android.graphics.Color.BLACK)
            binding.pieChart.setEntryLabelTextSize(12f)

            binding.pieChart.animateY(1000)
            binding.pieChart.invalidate()
        }
    }

    private fun deleteMeal(meal: SavedMeal) {
        lifecycleScope.launch {
            repository.deleteMeal(meal)
            loadMealsForDate(selectedDate)
        }
    }
}