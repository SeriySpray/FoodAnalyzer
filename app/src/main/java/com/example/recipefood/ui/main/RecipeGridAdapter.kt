package com.example.recipefood.ui.main

import android.text.SpannableString
import android.text.style.ForegroundColorSpan
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

class RecipeGridAdapter(
    private val onItemClick: (Recipe) -> Unit
) : ListAdapter<Recipe, RecipeGridAdapter.ViewHolder>(Diff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe_grid, parent, false)
        return ViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        view: View,
        private val onClick: (Recipe) -> Unit
    ) : RecyclerView.ViewHolder(view) {

        private val tvName: TextView = view.findViewById(R.id.tvName)
        private val tvMeta: TextView = view.findViewById(R.id.tvMeta)
        private val tvCalories: TextView = view.findViewById(R.id.tvCalories)
        private val cookedBadge: TextView = view.findViewById(R.id.cookedBadge)
        private val cookedBadgeContainer: View = view.findViewById(R.id.cookedBadgeContainer)

        fun bind(recipe: Recipe) {
            tvName.text = recipe.name

            val diffColor = when (recipe.difficulty) {
                Recipe.DIFFICULTY_EASY -> ContextCompat.getColor(itemView.context, R.color.difficulty_easy)
                Recipe.DIFFICULTY_MEDIUM -> ContextCompat.getColor(itemView.context, R.color.difficulty_medium)
                Recipe.DIFFICULTY_HARD -> ContextCompat.getColor(itemView.context, R.color.difficulty_hard)
                else -> ContextCompat.getColor(itemView.context, R.color.difficulty_medium)
            }
            val timeColor = ContextCompat.getColor(itemView.context, R.color.text_primary)
            val meta = "${recipe.cookingTime} хв · ${recipe.difficulty}"
            val spannable = SpannableString(meta)
            val diffStart = meta.indexOf(recipe.difficulty)
            spannable.setSpan(ForegroundColorSpan(timeColor), 0, diffStart, 0)
            spannable.setSpan(ForegroundColorSpan(diffColor), diffStart, meta.length, 0)
            tvMeta.text = spannable

            tvCalories.text = if (recipe.calories != null) {
                "${recipe.calories.toInt()} ккал"
            } else {
                "— ккал"
            }

            cookedBadgeContainer.visibility = View.VISIBLE
            cookedBadge.text = "${recipe.timesCooked}"

            itemView.setOnClickListener { onClick(recipe) }
        }
    }

    class Diff : DiffUtil.ItemCallback<Recipe>() {
        override fun areItemsTheSame(a: Recipe, b: Recipe) = a.id == b.id
        override fun areContentsTheSame(a: Recipe, b: Recipe) = a == b
    }
}
