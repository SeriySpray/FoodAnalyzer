package com.foodanalyzer.models

data class Food(
    var name: String,
    val products: MutableList<Product>,
    var nutrition: NutritionInfo? = null
)