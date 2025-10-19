package com.foodanalyzer.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.foodanalyzer.MainActivity
import com.foodanalyzer.adapters.ProductsAdapter
import com.foodanalyzer.database.AppDatabase
import com.foodanalyzer.databinding.ActivityResultsBinding
import com.foodanalyzer.models.Food
import com.foodanalyzer.models.SavedMeal
import com.foodanalyzer.repository.MealRepository
import com.google.gson.Gson
import kotlinx.coroutines.launch

class ResultsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultsBinding
    private lateinit var food: Food
    private lateinit var repository: MealRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = AppDatabase.getDatabase(this)
        repository = MealRepository(database.mealDao())

        val foodJson = intent.getStringExtra("food_json")
        food = Gson().fromJson(foodJson, Food::class.java)

        displayResults()

        binding.btnSave.setOnClickListener {
            saveMeal()
        }

        binding.btnClose.setOnClickListener {
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

    private fun saveMeal() {
        food.nutrition?.let { nutrition ->
            val savedMeal = SavedMeal(
                name = food.name,
                date = System.currentTimeMillis(),
                totalCalories = nutrition.calories,
                totalProteins = nutrition.proteins,
                totalFats = nutrition.fats,
                totalCarbs = nutrition.carbs,
                products = Gson().toJson(food.products)
            )

            lifecycleScope.launch {
                try {
                    repository.insertMeal(savedMeal)

                    // Відправляємо broadcast для оновлення головного екрану
                    val intent = Intent("com.foodanalyzer.MEAL_SAVED")
                    sendBroadcast(intent)

                    Toast.makeText(this@ResultsActivity, "Страву збережено!", Toast.LENGTH_SHORT).show()
                    binding.btnSave.isEnabled = false
                    binding.btnSave.text = "Збережено"
                } catch (e: Exception) {
                    Toast.makeText(this@ResultsActivity, "Помилка збереження: ${e.message}", Toast.LENGTH_SHORT).show()
                }

            }
        }
    }
}