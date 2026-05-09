package com.example.recipefood.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.recipefood.data.MealRepository
import com.example.recipefood.data.RecipeDatabase
import com.example.recipefood.data.UserSettingsRepository
import com.example.recipefood.model.UserSettings
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

data class DashboardState(
    val selectedDate: Date = Date(),
    val todayCalories: Double = 0.0,
    val todayProteins: Double = 0.0,
    val todayFats: Double = 0.0,
    val todayCarbs: Double = 0.0,
    val targetCalories: Double = 2000.0,
    val targetProteins: Double = 150.0,
    val targetFats: Double = 65.0,
    val targetCarbs: Double = 250.0,
    val streak: Int = 0,
    val meals: List<com.example.recipefood.model.SavedMeal> = emptyList()
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val mealRepository: MealRepository
    private val userSettingsRepository: UserSettingsRepository

    private val _state = MutableLiveData(DashboardState())
    val state: LiveData<DashboardState> = _state

    private var currentLoadJob: kotlinx.coroutines.Job? = null

    init {
        val database = RecipeDatabase.getDatabase(application)
        mealRepository = MealRepository(database.mealDao())
        userSettingsRepository = UserSettingsRepository(database.userSettingsDao())
        loadDashboard(Date())
    }

    private fun loadDashboard(date: Date) {
        currentLoadJob?.cancel()
        currentLoadJob = viewModelScope.launch {
            mealRepository.getMealsByDate(date).collectLatest { meals ->
                val totalCalories = meals.sumOf { it.totalCalories }
                val totalProteins = meals.sumOf { it.totalProteins }
                val totalFats = meals.sumOf { it.totalFats }
                val totalCarbs = meals.sumOf { it.totalCarbs }

                val settings = userSettingsRepository.getUserSettingsSync()
                val targetCalories = settings?.targetCalories?.takeIf { it > 0 } ?: 2000.0
                val targetProteins = settings?.targetProteins?.takeIf { it > 0 } ?: 150.0
                val targetFats = settings?.targetFats?.takeIf { it > 0 } ?: 65.0
                val targetCarbs = settings?.targetCarbs?.takeIf { it > 0 } ?: 250.0

                val newStreak = if (isToday(date)) calculateStreak(settings, meals.isNotEmpty()) else settings?.currentStreak ?: 0

                _state.value = DashboardState(
                    selectedDate = date,
                    todayCalories = totalCalories,
                    todayProteins = totalProteins,
                    todayFats = totalFats,
                    todayCarbs = totalCarbs,
                    targetCalories = targetCalories,
                    targetProteins = targetProteins,
                    targetFats = targetFats,
                    targetCarbs = targetCarbs,
                    streak = newStreak,
                    meals = meals
                )
            }
        }
    }

    fun setDate(date: Date) {
        loadDashboard(date)
    }

    fun deleteMeal(meal: com.example.recipefood.model.SavedMeal) {
        viewModelScope.launch {
            mealRepository.deleteMeal(meal)
        }
    }

    private fun isToday(date: Date): Boolean {
        val cal1 = Calendar.getInstance()
        val cal2 = Calendar.getInstance().apply { time = date }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private suspend fun calculateStreak(settings: UserSettings?, hasEatenToday: Boolean): Int {
        if (settings == null) return 0
        if (!hasEatenToday) return settings.currentStreak

        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        if (settings.lastStreakDate == todayStart) return settings.currentStreak

        val yesterdayStart = todayStart - 24 * 60 * 60 * 1000L
        val newStreak = if (settings.lastStreakDate == yesterdayStart) {
            settings.currentStreak + 1
        } else {
            1
        }

        userSettingsRepository.updateStreak(newStreak, todayStart)
        return newStreak
    }

    fun refresh() {
        _state.value?.selectedDate?.let { loadDashboard(it) } ?: loadDashboard(Date())
    }
}
