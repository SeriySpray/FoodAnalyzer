package com.example.recipefood.algorithms

import com.example.recipefood.model.Recipe

object SearchAlgorithms {

    /**
     * Лінійний пошук - використовується для пошуку рецептів за інгредієнтами
     * Тепер підтримує пошук за кількома інгредієнтами (через кому або пробіл)
     * Складність: O(n)
     */
    fun linearSearchByIngredients(recipes: List<Recipe>, searchIngredient: String): List<Recipe> {
        val results = mutableListOf<Recipe>()

        // Розбиваємо пошуковий запит на окремі інгредієнти
        val searchIngredients = parseSearchQuery(searchIngredient)

        if (searchIngredients.isEmpty()) {
            return results
        }

        // Проходимо по кожному рецепту
        for (recipe in recipes) {
            var allFound = true

            // Перевіряємо чи містить рецепт ВСІ шукані інгредієнти
            for (searchItem in searchIngredients) {
                var foundInRecipe = false

                for (ingredient in recipe.ingredients) {
                    if (ingredient.lowercase().contains(searchItem)) {
                        foundInRecipe = true
                        break
                    }
                }

                if (!foundInRecipe) {
                    allFound = false
                    break
                }
            }

            if (allFound) {
                results.add(recipe)
            }
        }

        return results
    }

    /**
     * Розбиває пошуковий запит на окремі терміни
     * Підтримує розділення через кому та пробіли
     */
    private fun parseSearchQuery(query: String): List<String> {
        return query
            .split(",", " ")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
    }

    /**
     * Лінійний пошук за складністю
     */
    fun linearSearchByDifficulty(recipes: List<Recipe>, difficulty: String): List<Recipe> {
        val results = mutableListOf<Recipe>()

        for (recipe in recipes) {
            if (recipe.difficulty.equals(difficulty, ignoreCase = true)) {
                results.add(recipe)
            }
        }

        return results
    }

    /**
     * Бінарний пошук - використовується для пошуку рецепту за точною назвою
     * ВАЖЛИВО: Список повинен бути відсортований за назвою!
     * Складність: O(log n)
     */
    fun binarySearchByName(sortedRecipes: List<Recipe>, searchName: String): Recipe? {
        var left = 0
        var right = sortedRecipes.size - 1
        val searchLower = searchName.lowercase().trim()

        while (left <= right) {
            val mid = left + (right - left) / 2
            val midName = sortedRecipes[mid].name.lowercase()

            when {
                midName == searchLower -> {
                    // Знайдено точний збіг
                    return sortedRecipes[mid]
                }
                midName < searchLower -> {
                    // Шукана назва знаходиться справа
                    left = mid + 1
                }
                else -> {
                    // Шукана назва знаходиться зліва
                    right = mid - 1
                }
            }
        }

        // Рецепт не знайдено
        return null
    }

    /**
     * Пошук за частковою назвою (для більш гнучкого пошуку)
     * Використовує лінійний пошук для часткових збігів
     */
    fun searchByPartialName(recipes: List<Recipe>, searchQuery: String): List<Recipe> {
        val results = mutableListOf<Recipe>()
        val searchLower = searchQuery.lowercase().trim()

        for (recipe in recipes) {
            if (recipe.name.lowercase().contains(searchLower)) {
                results.add(recipe)
            }
        }

        return results
    }

    /**
     * Комплексний пошук - шукає за назвою, інгредієнтами та складністю одночасно
     * Знаходить рецепти які містять ВСІ шукані терміни
     */
    fun complexSearch(
        recipes: List<Recipe>,
        searchQuery: String,
        searchByIngredients: Boolean = true,
        searchByName: Boolean = true,
        searchByDifficulty: Boolean = true
    ): List<Recipe> {
        val results = mutableListOf<Recipe>()
        val searchTerms = parseSearchQuery(searchQuery)

        if (searchTerms.isEmpty()) {
            return emptyList()
        }

        for (recipe in recipes) {
            var allTermsFound = true

            // Перевіряємо чи присутні ВСІ терміни в рецепті
            for (term in searchTerms) {
                var termFound = false

                // Пошук за назвою
                if (searchByName && recipe.name.lowercase().contains(term)) {
                    termFound = true
                }

                // Пошук за інгредієнтами
                if (searchByIngredients && !termFound) {
                    for (ingredient in recipe.ingredients) {
                        if (ingredient.lowercase().contains(term)) {
                            termFound = true
                            break
                        }
                    }
                }

                // Пошук за складністю
                if (searchByDifficulty && !termFound) {
                    if (recipe.difficulty.lowercase().contains(term)) {
                        termFound = true
                    }
                }

                // Якщо термін не знайдено, то цей рецепт не підходить
                if (!termFound) {
                    allTermsFound = false
                    break
                }
            }

            // Додаємо рецепт лише якщо знайдено ВСІ терміни
            if (allTermsFound) {
                results.add(recipe)
            }
        }

        return results
    }
}
