package com.example.recipefood.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "recipes")
@TypeConverters(Converters::class)
data class Recipe(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val ingredients: List<String>,
    val difficulty: String, // "Легкий", "Середній", "Складний"
    val cookingTime: Int, // час у хвилинах
    val instructions: String,
    val dateCreated: Long = System.currentTimeMillis(),
    val timesCooked: Int = 0, // кількість разів приготування
    val wasCooked: Boolean = false, // чи був приготований хоча б раз
    val folderId: Long = 1L, // ID папки (за замовчуванням "Ваші рецепти")
    // КБЖВ поля (nullable — може бути не розраховано)
    val calories: Double? = null,
    val proteins: Double? = null,
    val fats: Double? = null,
    val carbs: Double? = null
) {
    companion object {
        const val DIFFICULTY_EASY = "Легкий"
        const val DIFFICULTY_MEDIUM = "Середній"
        const val DIFFICULTY_HARD = "Складний"
    }
}

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromIngredientsList(value: List<String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toIngredientsList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }
}
