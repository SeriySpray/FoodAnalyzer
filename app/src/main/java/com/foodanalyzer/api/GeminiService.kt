package com.foodanalyzer.api

import android.util.Log
import com.foodanalyzer.models.Food
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

class GeminiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val apiKey = com.foodanalyzer.BuildConfig.API_KEY

    private val apiUrl = "https://api.groq.com/openai/v1/chat/completions"

    // Модель з підтримкою vision для аналізу фото
    private val visionModel = "meta-llama/llama-4-scout-17b-16e-instruct"

    // Модель для текстового аналізу КБЖУ
    private val textModel = "llama-3.3-70b-versatile"

    private var lastRequestTime = 0L
    private val minRequestInterval = 1000L // 1 секунда між запитами (Groq швидший)

    init {
        Log.d("GroqService", "Initialized with API key length: ${apiKey.length}")
    }

    private suspend fun waitForRateLimit() {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastRequest = currentTime - lastRequestTime
        if (timeSinceLastRequest < minRequestInterval) {
            val waitTime = minRequestInterval - timeSinceLastRequest
            Log.d("GroqService", "Rate limit: waiting ${waitTime}ms")
            kotlinx.coroutines.delay(waitTime)
        }
        lastRequestTime = System.currentTimeMillis()
    }

    private fun extractContent(responseBody: String): String {
        val jsonResponse = JsonParser.parseString(responseBody).asJsonObject
        return jsonResponse
            .getAsJsonArray("choices")
            ?.get(0)?.asJsonObject
            ?.getAsJsonObject("message")
            ?.get("content")?.asString
            ?.trim()
            ?.removePrefix("```json")
            ?.removePrefix("```")
            ?.removeSuffix("```")
            ?.trim()
            ?: throw Exception("Не вдалося отримати текст відповіді з JSON")
    }

    private fun handleErrorCode(code: Int, body: String): Nothing {
        when (code) {
            401 -> throw Exception("Невірний API ключ Groq (401)")
            429 -> throw Exception("Перевищено ліміт запитів Groq (429). Зачекайте хвилину")
            400 -> throw Exception("Невірний формат запиту (400): ${body.take(200)}")
            413 -> throw Exception("Зображення занадто велике (413). Спробуйте менший файл")
            else -> throw Exception("Помилка Groq API (${code}): ${body.take(200)}")
        }
    }

    suspend fun analyzeFood(base64Image: String): Food = withContext(Dispatchers.IO) {
        Log.d("GroqService", "analyzeFood() called")
        waitForRateLimit()

        if (apiKey.isEmpty()) {
            throw Exception("API ключ порожній! Перевірте keys.properties")
        }

        val prompt = """
            Проаналізуй це фото страви та визнач:
            1. Назву страви
            2. Список продуктів у страві
            3. Приблизну вагу кожного продукту в грамах
            Аналіз виконуй максимально точно опираючись на надійні джерела. Назви страв та продуктів вписуй на українській мові.
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
            addProperty("model", visionModel)
            addProperty("temperature", 0.4)
            addProperty("max_tokens", 2048)
            add("messages", gson.toJsonTree(listOf(
                mapOf(
                    "role" to "user",
                    "content" to listOf(
                        mapOf("type" to "text", "text" to prompt),
                        mapOf(
                            "type" to "image_url",
                            "image_url" to mapOf(
                                "url" to "data:image/jpeg;base64,$base64Image"
                            )
                        )
                    )
                )
            )))
        }.toString()

        val request = Request.Builder()
            .url(apiUrl)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Порожня відповідь від сервера")

            Log.d("GroqService", "Response code: ${response.code}")
            Log.d("GroqService", "Response body (first 300 chars): ${responseBody.take(300)}")

            if (!response.isSuccessful) {
                Log.e("GroqService", "API Error: ${response.code} - $responseBody")
                handleErrorCode(response.code, responseBody)
            }

            val content = extractContent(responseBody)
            Log.d("GroqService", "Parsed content: $content")

            val food = gson.fromJson(content, Food::class.java)
            Log.d("GroqService", "Successfully parsed Food: ${food.name}, products: ${food.products.size}")
            food
        } catch (e: Exception) {
            Log.e("GroqService", "Exception in analyzeFood", e)
            throw e
        }
    }

    suspend fun analyzeNutrition(food: Food): Food = withContext(Dispatchers.IO) {
        Log.d("GroqService", "analyzeNutrition() called for: ${food.name}")
        waitForRateLimit()

        val productsInfo = food.products.joinToString("\n") { "- ${it.name}: ${it.weight}г" }

        val prompt = """
            Проаналізуй харчову цінність страви "${food.name}" з таких продуктів:
            $productsInfo

            Порахуй для КОЖНОГО продукту та для ВСІЄЇ страви загалом:
            - Калорії (ккал)
            - Білки (г)
            - Жири (г)
            - Вуглеводи (г)
            Аналіз виконуй максимально точно опираючись на надійні джерела. Назви страв та продуктів вписуй на українській мові.
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
            addProperty("model", textModel)
            addProperty("temperature", 0.4)
            addProperty("max_tokens", 4096)
            add("messages", gson.toJsonTree(listOf(
                mapOf("role" to "user", "content" to prompt)
            )))
        }.toString()

        val request = Request.Builder()
            .url(apiUrl)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Порожня відповідь")

            Log.d("GroqService", "Nutrition response code: ${response.code}")

            if (!response.isSuccessful) {
                Log.e("GroqService", "Nutrition API Error: ${response.code}")
                handleErrorCode(response.code, responseBody)
            }

            val content = extractContent(responseBody)
            Log.d("GroqService", "Nutrition analysis complete")
            gson.fromJson(content, Food::class.java)
        } catch (e: Exception) {
            Log.e("GroqService", "Exception in analyzeNutrition", e)
            throw e
        }
    }
}
