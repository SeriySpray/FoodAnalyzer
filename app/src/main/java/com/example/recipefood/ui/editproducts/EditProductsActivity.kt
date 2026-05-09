package com.example.recipefood.ui.editproducts

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.recipefood.RecipeFoodApp
import com.example.recipefood.adapters.ProductsAdapter
import com.example.recipefood.databinding.ActivityEditProductsBinding
import com.example.recipefood.databinding.DialogAddProductBinding
import com.example.recipefood.model.Food
import com.example.recipefood.model.Product
import com.example.recipefood.ui.results.ResultsActivity
import com.google.gson.Gson
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditProductsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditProductsBinding
    private lateinit var food: Food
    private lateinit var adapter: ProductsAdapter
    private val groqService by lazy { (application as RecipeFoodApp).groqService }

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
            products = food.products,
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
        
        dialogBinding.tvDialogTitle.text = "Додати продукт"
        dialogBinding.btnSave.text = "Додати"

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()
            
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogBinding.btnSave.setOnClickListener {
            val name = dialogBinding.etProductName.text.toString()
            val weight = dialogBinding.etProductWeight.text.toString().toDoubleOrNull() ?: 0.0

            if (name.isNotEmpty() && weight > 0) {
                val product = Product(name, weight)
                food.products.add(product)
                adapter.updateProducts(food.products)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Заповніть всі поля", Toast.LENGTH_SHORT).show()
            }
        }
        
        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showEditProductDialog(position: Int) {
        val product = food.products[position]
        val dialogBinding = DialogAddProductBinding.inflate(layoutInflater)

        dialogBinding.tvDialogTitle.text = "Редагувати продукт"
        dialogBinding.btnSave.text = "Зберегти"
        dialogBinding.etProductName.setText(product.name)
        dialogBinding.etProductWeight.setText(product.weight.toString())

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()
            
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogBinding.btnSave.setOnClickListener {
            val name = dialogBinding.etProductName.text.toString()
            val weight = dialogBinding.etProductWeight.text.toString().toDoubleOrNull() ?: 0.0

            if (name.isNotEmpty() && weight > 0) {
                food.products[position] = Product(name, weight)
                adapter.updateProducts(food.products)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Заповніть всі поля", Toast.LENGTH_SHORT).show()
            }
        }
        
        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun analyzeNutrition() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        binding.btnAnalyze.isEnabled = false

        lifecycleScope.launch {
            try {
                val analyzedFood = withContext(Dispatchers.IO) {
                    groqService.analyzeNutrition(food)
                }
                binding.progressBar.visibility = android.view.View.GONE
                binding.btnAnalyze.isEnabled = true

                val intent = Intent(this@EditProductsActivity, ResultsActivity::class.java)
                intent.putExtra("food_json", Gson().toJson(analyzedFood))
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                binding.progressBar.visibility = android.view.View.GONE
                binding.btnAnalyze.isEnabled = true
                Toast.makeText(this@EditProductsActivity, "Помилка аналізу: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}