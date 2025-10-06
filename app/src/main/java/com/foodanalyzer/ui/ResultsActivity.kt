package com.foodanalyzer.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.foodanalyzer.MainActivity
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
            // Повертаємось на головний екран
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
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

        // Додаємо продукти динамічно
        binding.productsContainer.removeAllViews()

        food.products.forEach { product ->
            val itemView = layoutInflater.inflate(
                com.foodanalyzer.R.layout.item_product,
                binding.productsContainer,
                false
            )

            itemView.findViewById<android.widget.TextView>(com.foodanalyzer.R.id.tvProductName).text = product.name
            itemView.findViewById<android.widget.TextView>(com.foodanalyzer.R.id.tvProductWeight).text = "${product.weight} г"

            val nutritionLayout = itemView.findViewById<android.widget.LinearLayout>(com.foodanalyzer.R.id.nutritionLayout)
            nutritionLayout.visibility = android.view.View.VISIBLE

            itemView.findViewById<android.widget.ImageButton>(com.foodanalyzer.R.id.btnEdit).visibility = android.view.View.GONE
            itemView.findViewById<android.widget.ImageButton>(com.foodanalyzer.R.id.btnDelete).visibility = android.view.View.GONE

            product.nutrition?.let { nutrition ->
                itemView.findViewById<android.widget.TextView>(com.foodanalyzer.R.id.tvCalories).text =
                    "Калорії: ${String.format("%.1f", nutrition.calories)} ккал"
                itemView.findViewById<android.widget.TextView>(com.foodanalyzer.R.id.tvProteins).text =
                    "Білки: ${String.format("%.1f", nutrition.proteins)} г"
                itemView.findViewById<android.widget.TextView>(com.foodanalyzer.R.id.tvFats).text =
                    "Жири: ${String.format("%.1f", nutrition.fats)} г"
                itemView.findViewById<android.widget.TextView>(com.foodanalyzer.R.id.tvCarbs).text =
                    "Вуглеводи: ${String.format("%.1f", nutrition.carbs)} г"
            }

            binding.productsContainer.addView(itemView)
        }
    }
}
