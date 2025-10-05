package com.foodanalyzer.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.foodanalyzer.adapters.ProductsAdapter
import com.foodanalyzer.databinding.ActivityResultsBinding
import com.foodanalyzer.models.Food
import com.google.gson.Gson

class ResultsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultsBinding
    private lateinit var food: Food

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val foodJson = intent.getStringExtra("food_json")
        food = Gson().fromJson(foodJson, Food::class.java)

        displayResults()

        binding.btnClose.setOnClickListener {
            finishAffinity()
        }
    }

    private fun displayResults() {
        binding.tvFoodName.text = food.name

        food.nutrition?.let { nutrition ->
            binding.tvCalories.text = "${String.format("%.1f", nutrition.calories)} ккал"
            binding.tvProteins.text = "${String.format("%.1f", nutrition.proteins)} г"
            binding.tvFats.text = "${String.format("%.1f", nutrition.fats)} г"
            binding.tvCarbs.text = "${String.format("%.1f", nutrition.carbs)} г"
        }

        val adapter = ProductsAdapter(
            products = food.products.toMutableList(),
            showNutrition = true,
            onDeleteClick = {},
            onEditClick = {}
        )

        binding.rvProducts.layoutManager = LinearLayoutManager(this)
        binding.rvProducts.adapter = adapter
    }
}