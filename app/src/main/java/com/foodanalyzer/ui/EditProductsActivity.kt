package com.foodanalyzer.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.foodanalyzer.adapters.ProductsAdapter
import com.foodanalyzer.api.GeminiService
import com.foodanalyzer.databinding.ActivityEditProductsBinding
import com.foodanalyzer.databinding.DialogAddProductBinding
import com.foodanalyzer.models.Food
import com.foodanalyzer.models.Product
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditProductsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditProductsBinding
    private lateinit var food: Food
    private lateinit var adapter: ProductsAdapter
    private val geminiService = GeminiService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProductsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val foodJson = intent.getStringExtra("food_json")
        food = Gson().fromJson(foodJson, Food::class.java)

        binding.etFoodName.setText(food.name)

        setupRecyclerView()

        binding.btnAddProduct.setOnClickListener {
            showAddProductDialog()
        }

        binding.btnAnalyze.setOnClickListener {
            food.name = binding.etFoodName.text.toString()
            analyzeNutrition()
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = ProductsAdapter(
            products = food.products.toMutableList(),
            onDeleteClick = { position ->
                food.products.removeAt(position)
                adapter.updateProducts(food.products)
            },
            onEditClick = { position ->
                showEditProductDialog(position)
            }
        )

        binding.rvProducts.layoutManager = LinearLayoutManager(this)
        binding.rvProducts.adapter = adapter
    }

    private fun showAddProductDialog() {
        val dialogBinding = DialogAddProductBinding.inflate(layoutInflater)

        AlertDialog.Builder(this)
            .setTitle("Додати продукт")
            .setView(dialogBinding.root)
            .setPositiveButton("Додати") { _, _ ->
                val name = dialogBinding.etProductName.text.toString()
                val weight = dialogBinding.etProductWeight.text.toString().toDoubleOrNull() ?: 0.0

                if (name.isNotEmpty() && weight > 0) {
                    val product = Product(name, weight)
                    food.products.add(product)
                    adapter.updateProducts(food.products)
                } else {
                    Toast.makeText(this, "Заповніть всі поля", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Скасувати", null)
            .show()
    }

    private fun showEditProductDialog(position: Int) {
        val product = food.products[position]
        val dialogBinding = DialogAddProductBinding.inflate(layoutInflater)

        dialogBinding.etProductName.setText(product.name)
        dialogBinding.etProductWeight.setText(product.weight.toString())

        AlertDialog.Builder(this)
            .setTitle("Редагувати продукт")
            .setView(dialogBinding.root)
            .setPositiveButton("Зберегти") { _, _ ->
                val name = dialogBinding.etProductName.text.toString()
                val weight = dialogBinding.etProductWeight.text.toString().toDoubleOrNull() ?: 0.0

                if (name.isNotEmpty() && weight > 0) {
                    food.products[position] = Product(name, weight)
                    adapter.updateProducts(food.products)
                } else {
                    Toast.makeText(this, "Заповніть всі поля", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Скасувати", null)
            .show()
    }

    private fun analyzeNutrition() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.btnAnalyze.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val analyzedFood = geminiService.analyzeNutrition(food)

                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.btnAnalyze.isEnabled = true

                    val intent = Intent(this@EditProductsActivity, ResultsActivity::class.java)
                    intent.putExtra("food_json", Gson().toJson(analyzedFood))
                    startActivity(intent)
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = android.view.View.GONE
                    binding.btnAnalyze.isEnabled = true
                    Toast.makeText(this@EditProductsActivity, "Помилка аналізу: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}