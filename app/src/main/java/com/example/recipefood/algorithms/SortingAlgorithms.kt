package com.example.recipefood.algorithms

import com.example.recipefood.model.Recipe

object SortingAlgorithms {

    enum class SortType {
        NAME, TIME_ASC, DIFFICULTY, DATE, CALORIES_ASC
    }

    fun sortRecipes(recipes: MutableList<Recipe>, sortType: SortType) {
        when (sortType) {
            SortType.NAME -> quickSortByName(recipes)
            SortType.TIME_ASC -> quickSortByTimeAsc(recipes)
            SortType.DIFFICULTY -> quickSortByDifficulty(recipes)
            SortType.DATE -> quickSortByDate(recipes)
            SortType.CALORIES_ASC -> quickSortByCalories(recipes)
        }
    }

    fun quickSortByName(recipes: MutableList<Recipe>, low: Int = 0, high: Int = recipes.size - 1) {
        if (low < high) {
            val pi = partitionByName(recipes, low, high)
            quickSortByName(recipes, low, pi - 1)
            quickSortByName(recipes, pi + 1, high)
        }
    }

    private fun partitionByName(recipes: MutableList<Recipe>, low: Int, high: Int): Int {
        val pivot = recipes[high].name.lowercase()
        var i = low - 1
        for (j in low until high) {
            if (recipes[j].name.lowercase() <= pivot) {
                i++
                swap(recipes, i, j)
            }
        }
        swap(recipes, i + 1, high)
        return i + 1
    }

    /**
     * Сортування за часом по зростанню (від найшвидших до найдовших)
     */
    fun quickSortByTimeAsc(recipes: MutableList<Recipe>, low: Int = 0, high: Int = recipes.size - 1) {
        if (low < high) {
            val pi = partitionByTimeAsc(recipes, low, high)
            quickSortByTimeAsc(recipes, low, pi - 1)
            quickSortByTimeAsc(recipes, pi + 1, high)
        }
    }

    private fun partitionByTimeAsc(recipes: MutableList<Recipe>, low: Int, high: Int): Int {
        val pivot = recipes[high].cookingTime
        var i = low - 1
        for (j in low until high) {
            if (recipes[j].cookingTime <= pivot) {
                i++
                swap(recipes, i, j)
            }
        }
        swap(recipes, i + 1, high)
        return i + 1
    }

    private fun partitionByTimeDesc(recipes: MutableList<Recipe>, low: Int, high: Int): Int {
        val pivot = recipes[high].cookingTime
        var i = low - 1
        for (j in low until high) {
            if (recipes[j].cookingTime >= pivot) {
                i++
                swap(recipes, i, j)
            }
        }
        swap(recipes, i + 1, high)
        return i + 1
    }

    fun quickSortByDifficulty(recipes: MutableList<Recipe>, low: Int = 0, high: Int = recipes.size - 1) {
        if (low < high) {
            val pi = partitionByDifficulty(recipes, low, high)
            quickSortByDifficulty(recipes, low, pi - 1)
            quickSortByDifficulty(recipes, pi + 1, high)
        }
    }

    private fun partitionByDifficulty(recipes: MutableList<Recipe>, low: Int, high: Int): Int {
        val pivot = getDifficultyLevel(recipes[high].difficulty)
        var i = low - 1
        for (j in low until high) {
            if (getDifficultyLevel(recipes[j].difficulty) <= pivot) {
                i++
                swap(recipes, i, j)
            }
        }
        swap(recipes, i + 1, high)
        return i + 1
    }

    fun quickSortByDate(recipes: MutableList<Recipe>, low: Int = 0, high: Int = recipes.size - 1) {
        if (low < high) {
            val pi = partitionByDate(recipes, low, high)
            quickSortByDate(recipes, low, pi - 1)
            quickSortByDate(recipes, pi + 1, high)
        }
    }

    private fun partitionByDate(recipes: MutableList<Recipe>, low: Int, high: Int): Int {
        val pivot = recipes[high].dateCreated
        var i = low - 1
        for (j in low until high) {
            if (recipes[j].dateCreated >= pivot) {
                i++
                swap(recipes, i, j)
            }
        }
        swap(recipes, i + 1, high)
        return i + 1
    }

    fun quickSortByCalories(recipes: MutableList<Recipe>, low: Int = 0, high: Int = recipes.size - 1) {
        if (low < high) {
            val pi = partitionByCalories(recipes, low, high)
            quickSortByCalories(recipes, low, pi - 1)
            quickSortByCalories(recipes, pi + 1, high)
        }
    }

    private fun partitionByCalories(recipes: MutableList<Recipe>, low: Int, high: Int): Int {
        val pivot = recipes[high].calories ?: Double.MAX_VALUE
        var i = low - 1
        for (j in low until high) {
            if ((recipes[j].calories ?: Double.MAX_VALUE) <= pivot) {
                i++
                swap(recipes, i, j)
            }
        }
        swap(recipes, i + 1, high)
        return i + 1
    }

    private fun getDifficultyLevel(difficulty: String): Int {
        return when (difficulty) {
            Recipe.DIFFICULTY_EASY -> 1
            Recipe.DIFFICULTY_MEDIUM -> 2
            Recipe.DIFFICULTY_HARD -> 3
            else -> 0
        }
    }

    private fun swap(recipes: MutableList<Recipe>, i: Int, j: Int) {
        val temp = recipes[i]
        recipes[i] = recipes[j]
        recipes[j] = temp
    }
}
