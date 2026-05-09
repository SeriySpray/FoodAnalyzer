package com.example.recipefood.ui.search

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipefood.R
import com.example.recipefood.model.Recipe
import com.example.recipefood.ui.detail.RecipeDetailActivity
import com.example.recipefood.ui.main.RecipeAdapter
import com.example.recipefood.viewmodel.RecipeViewModel
import android.widget.EditText

class SearchActivity : AppCompatActivity() {

    private lateinit var viewModel: RecipeViewModel
    private lateinit var adapter: RecipeAdapter

    private lateinit var searchEditText: EditText
    private lateinit var searchResultsRecyclerView: RecyclerView
    private lateinit var noResultsTextView: TextView
    private lateinit var chipSearchAll: TextView
    private lateinit var chipSearchName: TextView
    private lateinit var chipSearchIngredients: TextView

    private var selectedChip: TextView? = null

    private var allRecipes: List<Recipe> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        // Ініціалізація UI
        searchEditText = findViewById(R.id.searchEditText)
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView)
        noResultsTextView = findViewById(R.id.noResultsTextView)
        chipSearchAll = findViewById(R.id.chipSearchAll)
        chipSearchName = findViewById(R.id.chipSearchName)
        chipSearchIngredients = findViewById(R.id.chipSearchIngredients)

        // Налаштування RecyclerView
        adapter = RecipeAdapter { recipe ->
            openRecipeDetail(recipe)
        }

        searchResultsRecyclerView.adapter = adapter
        searchResultsRecyclerView.layoutManager = LinearLayoutManager(this)

        // Ініціалізація ViewModel
        viewModel = ViewModelProvider(this)[RecipeViewModel::class.java]

        // Завантаження всіх рецептів — показуємо одразу
        viewModel.allRecipes.observe(this) { recipes ->
            allRecipes = recipes
            if (searchEditText.text.isNullOrBlank()) {
                adapter.submitList(recipes)
                searchResultsRecyclerView.visibility = View.VISIBLE
                noResultsTextView.visibility = View.GONE
            }
        }

        // Спостереження за результатами пошуку
        viewModel.searchResults.observe(this) { results ->
            if (searchEditText.text.isNullOrBlank()) return@observe
            if (results.isEmpty()) {
                searchResultsRecyclerView.visibility = View.GONE
                noResultsTextView.visibility = View.VISIBLE
            } else {
                searchResultsRecyclerView.visibility = View.VISIBLE
                noResultsTextView.visibility = View.GONE
                adapter.submitList(results)
            }
        }

        // Пошук при натисканні Enter
        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }

        // Пошук при зміні типу пошуку (chips are now TextViews)
        selectedChip = chipSearchAll
        chipSearchAll.alpha = 1.0f
        chipSearchName.alpha = 0.5f
        chipSearchIngredients.alpha = 0.5f

        val chipClickListener = View.OnClickListener { v ->
            selectedChip?.alpha = 0.5f
            selectedChip = v as TextView
            selectedChip?.alpha = 1.0f
            performSearch()
        }
        chipSearchAll.setOnClickListener(chipClickListener)
        chipSearchName.setOnClickListener(chipClickListener)
        chipSearchIngredients.setOnClickListener(chipClickListener)
    }

    private fun performSearch() {
        val query = searchEditText.text.toString().trim()

        if (query.isEmpty()) {
            adapter.submitList(allRecipes)
            searchResultsRecyclerView.visibility = View.VISIBLE
            noResultsTextView.visibility = View.GONE
            return
        }

        when (selectedChip?.id) {
            R.id.chipSearchAll -> {
                // Комплексний пошук (використовує лінійний пошук)
                viewModel.searchRecipes(allRecipes, query)
            }
            R.id.chipSearchName -> {
                // Бінарний пошук за ТОЧНОЮ назвою
                viewModel.searchByNameBinary(allRecipes, query)
            }
            R.id.chipSearchIngredients -> {
                // Лінійний пошук за інгредієнтами
                viewModel.searchByIngredients(allRecipes, query)
            }
            else -> viewModel.searchRecipes(allRecipes, query)
        }
    }

    private fun openRecipeDetail(recipe: Recipe) {
        val intent = Intent(this, RecipeDetailActivity::class.java)
        intent.putExtra("RECIPE_ID", recipe.id)
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
