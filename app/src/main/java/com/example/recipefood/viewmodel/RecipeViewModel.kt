package com.example.recipefood.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.recipefood.algorithms.SearchAlgorithms
import com.example.recipefood.algorithms.SortingAlgorithms
import com.example.recipefood.data.RecipeDatabase
import com.example.recipefood.data.RecipeRepository
import com.example.recipefood.model.Recipe
import com.example.recipefood.model.Folder
import kotlinx.coroutines.launch

class RecipeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RecipeRepository
    val allRecipes: LiveData<List<Recipe>>
    val allFolders: LiveData<List<Folder>>

    private val _sortedRecipes = MutableLiveData<List<Recipe>>()
    val sortedRecipes: LiveData<List<Recipe>> = _sortedRecipes

    private val _searchResults = MutableLiveData<List<Recipe>>()
    val searchResults: LiveData<List<Recipe>> = _searchResults

    private val _currentSortType = MutableLiveData<SortingAlgorithms.SortType>()
    val currentSortType: LiveData<SortingAlgorithms.SortType> = _currentSortType

    init {
        val database = RecipeDatabase.getDatabase(application)
        val recipeDao = database.recipeDao()
        val folderDao = database.folderDao()
        repository = RecipeRepository(recipeDao, folderDao)
        allRecipes = repository.allRecipes.asLiveData()
        allFolders = repository.allFolders.asLiveData()
        _currentSortType.value = SortingAlgorithms.SortType.DATE
        
        // Створити початкову папку при першому запуску
        viewModelScope.launch {
            repository.createDefaultFolderIfNeeded()
        }
    }

    // CRUD операції

    fun insert(recipe: Recipe) = viewModelScope.launch {
        repository.insert(recipe)
    }

    fun update(recipe: Recipe) = viewModelScope.launch {
        repository.update(recipe)
    }

    fun delete(recipe: Recipe) = viewModelScope.launch {
        repository.delete(recipe)
    }

    suspend fun getRecipeById(id: Long): Recipe? {
        return repository.getRecipeById(id)
    }

    // Сортування з використанням QuickSort

    fun sortRecipes(recipes: List<Recipe>, sortType: SortingAlgorithms.SortType) {
        viewModelScope.launch {
            val mutableList = recipes.toMutableList()
            SortingAlgorithms.sortRecipes(mutableList, sortType)
            _sortedRecipes.value = mutableList
            _currentSortType.value = sortType
        }
    }

    fun sortByName(recipes: List<Recipe>) {
        sortRecipes(recipes, SortingAlgorithms.SortType.NAME)
    }

    fun sortByTimeAsc(recipes: List<Recipe>) {
        sortRecipes(recipes, SortingAlgorithms.SortType.TIME_ASC)
    }

    fun sortByDifficulty(recipes: List<Recipe>) {
        sortRecipes(recipes, SortingAlgorithms.SortType.DIFFICULTY)
    }

    fun sortByDate(recipes: List<Recipe>) {
        sortRecipes(recipes, SortingAlgorithms.SortType.DATE)
    }

    // Пошук з використанням різних алгоритмів

    /**
     * Комплексний пошук (використовує лінійний пошук)
     * Підтримує пошук за кількома термінами (через кому або пробіл)
     */
    fun searchRecipes(recipes: List<Recipe>, query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                _searchResults.value = recipes
                return@launch
            }

            val results = SearchAlgorithms.complexSearch(recipes, query)
            _searchResults.value = results
        }
    }

    /**
     * Пошук за інгредієнтами (лінійний пошук)
     * Підтримує пошук за кількома інгредієнтами (через кому або пробіл)
     */
    fun searchByIngredients(recipes: List<Recipe>, ingredient: String) {
        viewModelScope.launch {
            val results = SearchAlgorithms.linearSearchByIngredients(recipes, ingredient)
            _searchResults.value = results
        }
    }

    /**
     * Пошук за назвою (бінарний пошук)
     * Важливо: список повинен бути відсортований за назвою!
     */
    fun searchByNameBinary(recipes: List<Recipe>, name: String) {
        viewModelScope.launch {
            // Спочатку сортуємо список
            val sortedList = recipes.toMutableList()
            SortingAlgorithms.quickSortByName(sortedList)

            // Виконуємо бінарний пошук
            val result = SearchAlgorithms.binarySearchByName(sortedList, name)
            _searchResults.value = if (result != null) listOf(result) else emptyList()
        }
    }

    /**
     * Пошук за частковою назвою (лінійний пошук)
     */
    fun searchByPartialName(recipes: List<Recipe>, query: String) {
        viewModelScope.launch {
            val results = SearchAlgorithms.searchByPartialName(recipes, query)
            _searchResults.value = results
        }
    }

    /**
     * Фільтрація за складністю (лінійний пошук)
     */
    fun filterByDifficulty(recipes: List<Recipe>, difficulty: String) {
        viewModelScope.launch {
            val results = SearchAlgorithms.linearSearchByDifficulty(recipes, difficulty)
            _searchResults.value = results
        }
    }

    /**
     * Скидання результатів пошуку
     */
    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }

    /**
     * Відмітити рецепт як приготований
     */
    fun markRecipeAsCooked(recipe: Recipe) = viewModelScope.launch {
        val updatedRecipe = recipe.copy(
            timesCooked = recipe.timesCooked + 1,
            wasCooked = true
        )
        repository.update(updatedRecipe)
    }

    /**
     * Скинути лічильник приготування
     */
    fun resetCookingCounter(recipe: Recipe) = viewModelScope.launch {
        val updatedRecipe = recipe.copy(
            timesCooked = 0,
            wasCooked = false
        )
        repository.update(updatedRecipe)
    }

    // Методи для роботи з папками

    fun getRecipesByFolder(folderId: Long): LiveData<List<Recipe>> {
        return repository.getRecipesByFolder(folderId).asLiveData()
    }

    fun insertFolder(folder: Folder) = viewModelScope.launch {
        repository.insertFolder(folder)
    }

    fun updateFolder(folder: Folder) = viewModelScope.launch {
        repository.updateFolder(folder)
    }

    fun deleteFolder(folder: Folder) = viewModelScope.launch {
        repository.deleteFolder(folder)
    }

    suspend fun getFolderById(id: Long): Folder? {
        return repository.getFolderById(id)
    }

    fun toggleFolderExpanded(folder: Folder) = viewModelScope.launch {
        val updatedFolder = folder.copy(isExpanded = !folder.isExpanded)
        repository.updateFolder(updatedFolder)
    }
}
