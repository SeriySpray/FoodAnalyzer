package com.foodanalyzer.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.foodanalyzer.databinding.ItemMealHistoryBinding
import com.foodanalyzer.models.SavedMeal
import java.text.SimpleDateFormat
import java.util.*

class MealHistoryAdapter(
    private val onItemClick: (SavedMeal) -> Unit,
    private val onDeleteClick: (SavedMeal) -> Unit
) : ListAdapter<SavedMeal, MealHistoryAdapter.MealViewHolder>(MealDiffCallback()) {

    inner class MealViewHolder(val binding: ItemMealHistoryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealViewHolder {
        val binding = ItemMealHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MealViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MealViewHolder, position: Int) {
        val meal = getItem(position)

        holder.binding.apply {
            tvMealName.text = meal.name

            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            tvMealTime.text = timeFormat.format(Date(meal.date))

            tvCalories.text = "${String.format("%.0f", meal.totalCalories)} ккал"
            tvProteins.text = "Білки: ${String.format("%.1f", meal.totalProteins)}г"
            tvFats.text = "Жири: ${String.format("%.1f", meal.totalFats)}г"
            tvCarbs.text = "Вуглеводи: ${String.format("%.1f", meal.totalCarbs)}г"

            root.setOnClickListener {
                onItemClick(meal)
            }

            btnDelete.setOnClickListener {
                onDeleteClick(meal)
            }
        }
    }

    class MealDiffCallback : DiffUtil.ItemCallback<SavedMeal>() {
        override fun areItemsTheSame(oldItem: SavedMeal, newItem: SavedMeal): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SavedMeal, newItem: SavedMeal): Boolean {
            return oldItem == newItem
        }
    }
}