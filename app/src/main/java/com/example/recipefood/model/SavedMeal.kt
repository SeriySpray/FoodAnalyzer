package com.example.recipefood.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_meals")
data class SavedMeal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val date: Long, // Timestamp в мілісекундах
    val totalCalories: Double,
    val totalProteins: Double,
    val totalFats: Double,
    val totalCarbs: Double,
    val products: String // JSON string з продуктами
)
