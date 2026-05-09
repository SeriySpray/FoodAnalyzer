package com.example.recipefood.model

data class Food(
    var name: String,
    val products: MutableList<Product>,
    var nutrition: NutritionInfo? = null
)
