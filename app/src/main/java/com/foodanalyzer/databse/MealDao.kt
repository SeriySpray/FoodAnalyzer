package com.foodanalyzer.database

import androidx.room.*
import com.foodanalyzer.models.SavedMeal
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Insert
    suspend fun insertMeal(meal: SavedMeal): Long

    @Query("SELECT * FROM saved_meals ORDER BY date DESC")
    fun getAllMeals(): Flow<List<SavedMeal>>

    @Query("SELECT * FROM saved_meals WHERE date >= :startDate AND date < :endDate ORDER BY date DESC")
    fun getMealsByDate(startDate: Long, endDate: Long): Flow<List<SavedMeal>>

    @Query("SELECT * FROM saved_meals WHERE id = :mealId")
    suspend fun getMealById(mealId: Long): SavedMeal?

    @Delete
    suspend fun deleteMeal(meal: SavedMeal)

    @Query("DELETE FROM saved_meals")
    suspend fun deleteAllMeals()
}