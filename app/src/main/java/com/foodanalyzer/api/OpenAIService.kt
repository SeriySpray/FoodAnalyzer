package com.foodanalyzer.api

import com.foodanalyzer.models.Food
import com.foodanalyzer.models.NutritionInfo
import com.foodanalyzer.models.Product
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class OpenAIService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    // ВАЖЛИВО: Замініть YOUR_API_KEY на ваш справжній ключ OpenAI API
    private val apiKey = "YOUR_API_KEY"
    private val apiUrl = "https://api.openai.com/v1/chat/completions"

    suspend fun analyzeFood(base64Image: String): Food = withContext(Dispatchers.IO) {
        val prompt = """
            Проаналізуй це фото страви та визнач:
            1. Назву страви
            2. Список продуктів у страві
            3. Приблизну вагу кожного продукту в грамах
            
            Поверни результат у форматі JSON:
            {
              "name": "Назва страви",
              "products": [
                {"name": "Продукт 1", "weight": 100},
                {"name": "Продукт 2", "weight": 50}
              ]
            }
            
            Відповідай ТІЛЬКИ валідним JSON без додаткового тексту.
        """.trimIndent()

        val requestBody = JsonObject().apply {
            addProperty("model", "gpt-4-vision-preview")
            add("messages", gson.toJsonTree(listOf(
                mapOf(
                    "role" to "user",
                    "content" to listOf(
                        mapOf("type" to "text", "text" to prompt),
                        mapOf(
                            "type" to "image_url",
                            "image_url" to mapOf("url" to "data:image/jpeg;base64,$base64Image")
                        )
                    )
                )
            )))
            addProperty("max_tokens", 1000)
        }.toString()

        val request = Request.Builder()
            .url(apiUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Порожня відповідь")

        if (!response.isSuccessful) {
            throw Exception("Помилка API: $responseBody")
        }

        val jsonResponse = JsonParser.parseString(responseBody).asJsonObject
        val content = jsonResponse
            .getAsJsonArray("choices")
            .get(0).asJsonObject
            .getAsJsonObject("message")
            .get("content").asString
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        gson.fromJson(content, Food::class.java)
    }

    suspend fun analyzeNutrition(food: Food): Food = withContext(Dispatchers.IO) {
        val productsInfo = food.products.joinToString("\n") {
            "- ${it.name}: ${it.weight}г"
        }

        val prompt = """
            Проаналізуй харчову цінність страви "${food.name}" з таких продуктів:
            $productsInfo
            
            Порахуй для КОЖНОГО продукту та для ВСІЄЇ страви загалом:
            - Калорії (ккал)
            - Білки (г)
            - Жири (г)
            - Вуглеводи (г)
            
            Поверни результат у форматі JSON:
            {
              "name": "${food.name}",
              "nutrition": {
                "calories": 500.0,
                "proteins": 25.0,
                "fats": 15.0,
                "carbs": 60.0
              },
              "products": [
                {
                  "name": "Продукт 1",
                  "weight": 100,
                  "nutrition": {
                    "calories": 200.0,
                    "proteins": 10.0,
                    "fats": 5.0,
                    "carbs": 25.0
                  }
                }
              ]
            }
            
            Відповідай ТІЛЬКИ валідним JSON без додаткового тексту.
        """.trimIndent()

        val requestBody = JsonObject().apply {
            addProperty("model", "gpt-4-turbo-preview")
            add("messages", gson.toJsonTree(listOf(
                mapOf("role" to "user", "content" to prompt)
            )))
            addProperty("max_tokens", 2000)
        }.toString()

        val request = Request.Builder()
            .url(apiUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Порожня відповідь")

        if (!response.isSuccessful) {
            throw Exception("Помилка API: $responseBody")
        }

        val jsonResponse = JsonParser.parseString(responseBody).asJsonObject
        val content = jsonResponse
            .getAsJsonArray("choices")
            .get(0).asJsonObject
            .getAsJsonObject("message")
            .get("content").asString
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        gson.fromJson(content, Food::class.java)
    }
}