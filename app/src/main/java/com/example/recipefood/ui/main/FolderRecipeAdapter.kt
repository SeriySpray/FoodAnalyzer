package com.example.recipefood.ui.main

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.recipefood.R
import com.example.recipefood.model.Folder
import com.example.recipefood.model.Recipe
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Адаптер для відображення папок та рецептів з можливістю згортання/розгортання
 */
class FolderRecipeAdapter(
    private val onRecipeClick: (Recipe) -> Unit,
    private val onFolderClick: (Folder) -> Unit,
    private val onFolderRename: (Folder) -> Unit,
    private val onFolderDelete: (Folder) -> Unit,
    private val onAddRecipeToFolder: (Folder) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items = listOf<ListItem>()
    private var expandedFolderId: Long? = null

    companion object {
        private const val TYPE_FOLDER = 0
        private const val TYPE_RECIPE = 1
    }

    sealed class ListItem {
        data class FolderItem(val folder: Folder, val recipeCount: Int) : ListItem()
        data class RecipeItem(val recipe: Recipe) : ListItem()
    }

    fun submitList(folders: List<Folder>, allRecipes: List<Recipe>) {
        val oldFolders = items.filterIsInstance<ListItem.FolderItem>().map { it.folder }
        val newItems = mutableListOf<ListItem>()
        
        // Знайти папку, яка щойно розгорнулась
        expandedFolderId = null
        folders.forEach { folder ->
            val oldFolder = oldFolders.find { it.id == folder.id }
            if (folder.isExpanded && oldFolder != null && !oldFolder.isExpanded) {
                // Ця папка щойно розгорнулась
                expandedFolderId = folder.id
            }
        }
        
        folders.forEach { folder ->
            val folderRecipes = allRecipes.filter { it.folderId == folder.id }
            newItems.add(ListItem.FolderItem(folder, folderRecipes.size))
            
            // Якщо папка розгорнута, додати рецепти
            if (folder.isExpanded) {
                folderRecipes.forEach { recipe ->
                    newItems.add(ListItem.RecipeItem(recipe))
                }
            }
        }
        
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ListItem.FolderItem -> TYPE_FOLDER
            is ListItem.RecipeItem -> TYPE_RECIPE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_FOLDER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_folder, parent, false)
                FolderViewHolder(view)
            }
            TYPE_RECIPE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_recipe, parent, false)
                RecipeViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ListItem.FolderItem -> {
                val isLastFolder = isLastFolderItem(position)
                (holder as FolderViewHolder).bind(item.folder, item.recipeCount, isLastFolder)
            }
            is ListItem.RecipeItem -> {
                val recipe = item.recipe
                (holder as RecipeViewHolder).bind(recipe)
                // Анімація випадіння зверху вниз тільки для рецептів з щойно розгорнутої папки
                if (expandedFolderId != null && recipe.folderId == expandedFolderId) {
                    holder.itemView.translationY = -50f
                    holder.itemView.alpha = 0f
                    holder.itemView.animate()
                        .translationY(0f)
                        .alpha(1f)
                        .setDuration(250)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .start()
                } else {
                    // Скинути значення для випадків без анімації
                    holder.itemView.translationY = 0f
                    holder.itemView.alpha = 1f
                }
            }
        }
    }
    
    private fun isLastFolderItem(position: Int): Boolean {
        for (i in position + 1 until items.size) {
            if (items[i] is ListItem.FolderItem) {
                return false
            }
        }
        return true
    }

    override fun getItemCount() = items.size

    inner class FolderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val folderNameTextView: TextView = itemView.findViewById(R.id.folderNameTextView)
        private val expandIconTextView: TextView = itemView.findViewById(R.id.expandIconTextView)
        private val addRecipeButton: View = itemView.findViewById(R.id.addRecipeButton)
        private val folderMenuButton: View = itemView.findViewById(R.id.folderMenuButton)
        private val bottomDivider: View = itemView.findViewById(R.id.bottomDivider)

        fun bind(folder: Folder, recipeCount: Int, isLastFolder: Boolean) {
            folderNameTextView.text = folder.name
            
            // Показати нижню лінію тільки для останньої папки
            bottomDivider.visibility = if (isLastFolder) View.VISIBLE else View.GONE
            
            // Іконка стрілки (▼ для розгорнутої, ▶ для згорнутої)
            expandIconTextView.text = if (folder.isExpanded) "▼" else "▶"
            
            // Клік по папці - згортання/розгортання з анімацією
            itemView.setOnClickListener {
                animateExpandIcon(folder.isExpanded)
                onFolderClick(folder)
            }
            
            // Клік по кнопці додавання рецепта
            addRecipeButton.setOnClickListener {
                onAddRecipeToFolder(folder)
            }
            
            // Клік по кнопці меню - показати PopupMenu
            folderMenuButton.setOnClickListener { view ->
                showFolderMenu(view, folder)
            }
        }
        
        private fun showFolderMenu(view: View, folder: Folder) {
            val popupView = LayoutInflater.from(view.context)
                .inflate(R.layout.popup_folder_menu, null)
            
            val popupWindow = android.widget.PopupWindow(
                popupView,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                true
            )
            
            // Встановлюємо фон прозорим для коректного відображення заокруглених кутів
            popupWindow.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            popupWindow.elevation = 0f
            
            // Обробники кліків
            popupView.findViewById<View>(R.id.menuRenameFolder).setOnClickListener {
                onFolderRename(folder)
                popupWindow.dismiss()
            }
            
            popupView.findViewById<View>(R.id.menuDeleteFolder).setOnClickListener {
                onFolderDelete(folder)
                popupWindow.dismiss()
            }
            
            // Показуємо popup під кнопкою меню
            popupWindow.showAsDropDown(view, -200, 0)
        }
        
        private fun animateExpandIcon(isExpanded: Boolean) {
            val startRotation = if (isExpanded) 0f else 90f
            val endRotation = if (isExpanded) 90f else 0f
            
            val rotateAnimator = ObjectAnimator.ofFloat(expandIconTextView, "rotation", startRotation, endRotation)
            rotateAnimator.duration = 200
            rotateAnimator.interpolator = AccelerateDecelerateInterpolator()
            rotateAnimator.start()
        }
    }

    inner class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.recipeNameTextView)
        private val ingredientsPreviewTextView: TextView = itemView.findViewById(R.id.ingredientsPreviewTextView)
        private val difficultyBadge: TextView = itemView.findViewById(R.id.difficultyBadge)
        private val cookingTimeTextView: TextView = itemView.findViewById(R.id.cookingTimeTextView)
        private val cookedBadge: TextView = itemView.findViewById(R.id.cookedBadge)
        private val cookedBadgeContainer: View = itemView.findViewById(R.id.cookedBadgeContainer)

        fun bind(recipe: Recipe) {
            nameTextView.text = recipe.name
            
            // Попередній перегляд інгредієнтів (перші 3)
            val ingredientsPreview = recipe.ingredients.take(3).joinToString(", ")
            ingredientsPreviewTextView.text = ingredientsPreview
            
            // Складність
            difficultyBadge.text = recipe.difficulty
            val difficultyColor = when (recipe.difficulty) {
                Recipe.DIFFICULTY_EASY -> itemView.context.getColor(R.color.difficulty_easy)
                Recipe.DIFFICULTY_MEDIUM -> itemView.context.getColor(R.color.difficulty_medium)
                Recipe.DIFFICULTY_HARD -> itemView.context.getColor(R.color.difficulty_hard)
                else -> itemView.context.getColor(R.color.difficulty_medium)
            }
            try {
                difficultyBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(difficultyColor)
            } catch (_: Exception) {}
            
            // Час приготування
            cookingTimeTextView.text = "${recipe.cookingTime} хв"
            
            // Відображення бейджа з кількістю приготувань
            cookedBadgeContainer.visibility = View.VISIBLE
            cookedBadge.text = "${recipe.timesCooked}"

            // Клік на картку
            itemView.setOnClickListener {
                onRecipeClick(recipe)
            }
        }
    }
}
