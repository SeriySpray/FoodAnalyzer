package com.example.recipefood.ui.main

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.recipefood.R
import com.example.recipefood.model.Recipe
import java.text.SimpleDateFormat
import java.util.*

class RecipeAdapter(
    private val onItemClick: (Recipe) -> Unit
) : ListAdapter<Recipe, RecipeAdapter.RecipeViewHolder>(RecipeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe, parent, false)
        return RecipeViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class RecipeViewHolder(
        itemView: View,
        private val onItemClick: (Recipe) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val nameTextView: TextView = itemView.findViewById(R.id.recipeNameTextView)
        private val ingredientsPreviewTextView: TextView = itemView.findViewById(R.id.ingredientsPreviewTextView)
        private val difficultyBadge: TextView = itemView.findViewById(R.id.difficultyBadge)
        private val cookingTimeTextView: TextView = itemView.findViewById(R.id.cookingTimeTextView)
        private val cookedBadge: TextView = itemView.findViewById(R.id.cookedBadge)
        private val cookedBadgeContainer: View = itemView.findViewById(R.id.cookedBadgeContainer)

        fun bind(recipe: Recipe) {
            nameTextView.text = recipe.name

            // Показати перші кілька інгредієнтів
            val ingredientsPreview = recipe.ingredients.take(3).joinToString(", ")
            val hasMore = recipe.ingredients.size > 3
            ingredientsPreviewTextView.text = if (hasMore) {
                "$ingredientsPreview..."
            } else {
                ingredientsPreview
            }

            // Бейдж складності
            difficultyBadge.visibility = View.VISIBLE
            difficultyBadge.text = recipe.difficulty
            val diffColor = when (recipe.difficulty) {
                Recipe.DIFFICULTY_EASY -> R.color.difficulty_easy
                Recipe.DIFFICULTY_MEDIUM -> R.color.difficulty_medium
                Recipe.DIFFICULTY_HARD -> R.color.difficulty_hard
                else -> R.color.medium_gray
            }
            val drawable = GradientDrawable()
            drawable.shape = GradientDrawable.RECTANGLE
            drawable.cornerRadius = 48f
            drawable.setColor(ContextCompat.getColor(itemView.context, diffColor))
            difficultyBadge.background = drawable

            // Час приготування
            cookingTimeTextView.visibility = View.VISIBLE
            cookingTimeTextView.text = "${recipe.cookingTime} хв"
            cookingTimeTextView.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_primary))

            // Відображення бейджа з кількістю приготувань
            cookedBadgeContainer.visibility = View.VISIBLE
            cookedBadge.text = "${recipe.timesCooked}"

            // Клік на картку
            itemView.setOnClickListener {
                onItemClick(recipe)
            }
        }
    }

    class RecipeDiffCallback : DiffUtil.ItemCallback<Recipe>() {
        override fun areItemsTheSame(oldItem: Recipe, newItem: Recipe): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Recipe, newItem: Recipe): Boolean {
            return oldItem == newItem
        }
    }
}
