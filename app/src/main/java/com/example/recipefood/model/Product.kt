package com.example.recipefood.model

data class Product(
    val name: String,
    val weight: Double,
    var nutrition: NutritionInfo? = null
)
