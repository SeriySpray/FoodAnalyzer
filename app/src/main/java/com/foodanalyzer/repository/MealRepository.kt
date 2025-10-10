package com.foodanalyzer.repository

import com.foodanalyzer.database.MealDao
import com.foodanalyzer.models.SavedMeal
import kotlinx.coroutines.flow.Flow
import java.util.*

class MealRepository(private val mealDao: MealDao) {

    fun getAllMeals(): Flow<List<SavedMeal>> = mealDao.getAllMeals()

    fun getMealsByDate(date: Date): Flow<List<SavedMeal>> {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endDate = calendar.timeInMillis

        return mealDao.getMealsByDate(startDate, endDate)
    }

    suspend fun insertMeal(meal: SavedMeal): Long {
        return mealDao.insertMeal(meal)
    }

    suspend fun deleteMeal(meal: SavedMeal) {
        mealDao.deleteMeal(meal)
    }

    suspend fun getMealById(mealId: Long): SavedMeal? {
        return mealDao.getMealById(mealId)
    }
}