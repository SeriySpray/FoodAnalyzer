package com.foodanalyzer.models

data class Product(
    val name: String,
    val weight: Double,
    var nutrition: NutritionInfo? = null
)