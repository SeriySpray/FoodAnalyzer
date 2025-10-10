package com.foodanalyzer.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.foodanalyzer.database.AppDatabase
import com.foodanalyzer.databinding.ActivityMealDetailBinding
import com.foodanalyzer.models.Product
import com.foodanalyzer.repository.MealRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MealDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMealDetailBinding
    private lateinit var repository: MealRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMealDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = AppDatabase.getDatabase(this)
        repository = MealRepository(database.mealDao())

        val mealId = intent.getLongExtra("meal_id", -1)
        if (mealId != -1L) {
            loadMealDetails(mealId)
        }

        setupToolbar()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Деталі страви"
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadMealDetails(mealId: Long) {
        lifecycleScope.launch {
            val meal = repository.getMealById(mealId)
            meal?.let {
                binding.tvFoodName.text = it.name

                val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("uk"))
                binding.tvMealDate.text = dateFormat.format(Date(it.date))

                binding.tvCalories.text = "${String.format("%.1f", it.totalCalories)} ккал"
                binding.tvProteins.text = "${String.format("%.1f", it.totalProteins)} г"
                binding.tvFats.text = "${String.format("%.1f", it.totalFats)} г"
                binding.tvCarbs.text = "${String.format("%.1f", it.totalCarbs)} г"

                // Парсимо продукти з JSON
                val gson = Gson()
                val productListType = object : TypeToken<List<Product>>() {}.type
                val products: List<Product> = gson.fromJson(it.products, productListType)

                displayProducts(products)
            }
        }
    }

    private fun displayProducts(products: List<Product>) {
        binding.productsContainer.removeAllViews()

        products.forEach { product ->
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