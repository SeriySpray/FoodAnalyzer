package com.example.recipefood.data

import com.example.recipefood.model.Recipe
import com.example.recipefood.model.Folder
import kotlinx.coroutines.flow.Flow

class RecipeRepository(
    private val recipeDao: RecipeDao,
    private val folderDao: FolderDao
) {

    val allRecipes: Flow<List<Recipe>> = recipeDao.getAllRecipes()
    val allFolders: Flow<List<Folder>> = folderDao.getAllFolders()

    suspend fun insert(recipe: Recipe): Long {
        return recipeDao.insert(recipe)
    }

    suspend fun update(recipe: Recipe) {
        recipeDao.update(recipe)
    }

    suspend fun delete(recipe: Recipe) {
        recipeDao.delete(recipe)
    }

    suspend fun getRecipeById(id: Long): Recipe? {
        return recipeDao.getRecipeById(id)
    }

    fun searchByName(query: String): Flow<List<Recipe>> {
        return recipeDao.searchByName(query)
    }

    fun filterByDifficulty(difficulty: String): Flow<List<Recipe>> {
        return recipeDao.filterByDifficulty(difficulty)
    }

    fun getAllRecipesSortedByTime(): Flow<List<Recipe>> {
        return recipeDao.getAllRecipesSortedByTime()
    }

    fun getAllRecipesSortedByName(): Flow<List<Recipe>> {
        return recipeDao.getAllRecipesSortedByName()
    }

    suspend fun deleteAll() {
        recipeDao.deleteAll()
    }

    fun getRecipesByFolder(folderId: Long): Flow<List<Recipe>> {
        return recipeDao.getRecipesByFolder(folderId)
    }

    // Методи для папок
    suspend fun insertFolder(folder: Folder): Long {
        return folderDao.insert(folder)
    }

    suspend fun updateFolder(folder: Folder) {
        folderDao.update(folder)
    }

    suspend fun deleteFolder(folder: Folder) {
        folderDao.delete(folder)
    }

    suspend fun getFolderById(id: Long): Folder? {
        return folderDao.getFolderById(id)
    }

    suspend fun getFolderCount(): Int {
        return folderDao.getFolderCount()
    }

    suspend fun createDefaultFolderIfNeeded() {
        if (getFolderCount() == 0) {
            insertFolder(
                Folder(
                    id = Folder.DEFAULT_FOLDER_ID,
                    name = Folder.DEFAULT_FOLDER_NAME
                )
            )
        }
    }
}
