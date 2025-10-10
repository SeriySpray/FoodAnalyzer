package com.foodanalyzer.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

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

// Type Converters для Room
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromProductList(value: List<Product>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toProductList(value: String): List<Product> {
        val listType = object : TypeToken<List<Product>>() {}.type
        return gson.fromJson(value, listType)
    }
}