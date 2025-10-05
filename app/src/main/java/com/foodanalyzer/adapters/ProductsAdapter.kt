package com.foodanalyzer.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.foodanalyzer.databinding.ItemProductBinding
import com.foodanalyzer.models.Product

class ProductsAdapter(
    private var products: MutableList<Product>,
    private val showNutrition: Boolean = false,
    private val onDeleteClick: (Int) -> Unit,
    private val onEditClick: (Int) -> Unit
) : RecyclerView.Adapter<ProductsAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(val binding: ItemProductBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]

        holder.binding.apply {
            tvProductName.text = product.name
            tvProductWeight.text = "${product.weight} г"

            if (showNutrition && product.nutrition != null) {
                nutritionLayout.visibility = View.VISIBLE
                btnEdit.visibility = View.GONE
                btnDelete.visibility = View.GONE

                tvCalories.text = "Калорії: ${String.format("%.1f", product.nutrition!!.calories)} ккал"
                tvProteins.text = "Білки: ${String.format("%.1f", product.nutrition!!.proteins)} г"
                tvFats.text = "Жири: ${String.format("%.1f", product.nutrition!!.fats)} г"
                tvCarbs.text = "Вуглеводи: ${String.format("%.1f", product.nutrition!!.carbs)} г"
            } else {
                nutritionLayout.visibility = View.GONE
                btnEdit.visibility = View.VISIBLE
                btnDelete.visibility = View.VISIBLE

                btnEdit.setOnClickListener {
                    onEditClick(position)
                }

                btnDelete.setOnClickListener {
                    onDeleteClick(position)
                }
            }
        }
    }

    override fun getItemCount() = products.size

    fun updateProducts(newProducts: MutableList<Product>) {
        products = newProducts
        notifyDataSetChanged()
    }
}