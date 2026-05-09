package com.example.recipe_manager

import com.google.gson.Gson
import com.example.recipefood.model.NutritionInfo
import org.junit.Test
import org.junit.Assert.*

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }
}

class NutritionParsingTest {
    @Test
    fun `parseNutritionInfo parses valid json`() {
        val json = """{"calories":450.0,"proteins":25.0,"fats":12.5,"carbs":55.0}"""
        val result = Gson().fromJson(json, NutritionInfo::class.java)
        assertEquals(450.0, result.calories, 0.001)
        assertEquals(25.0, result.proteins, 0.001)
        assertEquals(12.5, result.fats, 0.001)
        assertEquals(55.0, result.carbs, 0.001)
    }

    @Test
    fun `parseNutritionInfo handles integer values`() {
        val json = """{"calories":300,"proteins":20,"fats":10,"carbs":40}"""
        val result = Gson().fromJson(json, NutritionInfo::class.java)
        assertEquals(300.0, result.calories, 0.001)
    }
}
