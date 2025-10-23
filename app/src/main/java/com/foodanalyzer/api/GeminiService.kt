package com.foodanalyzer.api
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
    private val apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-exp:generateContent?key=$apiKey"

    suspend fun analyzeFood(base64Image: String): Food = withContext(Dispatchers.IO) {
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
            add("contents", gson.toJsonTree(listOf(
                mapOf(
                    "parts" to listOf(
                        mapOf("text" to prompt),
                        mapOf(
                            "inline_data" to mapOf(
                                "mime_type" to "image/jpeg",
                                "data" to base64Image
                            )
                        )
                    )
                )
            )))
            add("generationConfig", JsonObject().apply {
                addProperty("temperature", 0.4)
                addProperty("topK", 32)
                addProperty("topP", 1)
                addProperty("maxOutputTokens", 2048)
            })
        }.toString()

        val request = Request.Builder()
            .url(apiUrl)
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
            .getAsJsonArray("candidates")
            ?.get(0)?.asJsonObject
            ?.getAsJsonObject("content")
            ?.getAsJsonArray("parts")
            ?.get(0)?.asJsonObject
            ?.get("text")?.asString
            ?.trim()
            ?.removePrefix("```json")
            ?.removePrefix("```")
            ?.removeSuffix("```")
            ?.trim()
            ?: throw Exception("Не вдалося отримати текст відповіді")

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
            add("contents", gson.toJsonTree(listOf(
                mapOf(
                    "parts" to listOf(
                        mapOf("text" to prompt)
                    )
                )
            )))
            add("generationConfig", JsonObject().apply {
                addProperty("temperature", 0.4)
                addProperty("topK", 32)
                addProperty("topP", 1)
                addProperty("maxOutputTokens", 4096)
            })
        }.toString()

        val request = Request.Builder()
            .url(apiUrl)
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
            .getAsJsonArray("candidates")
            ?.get(0)?.asJsonObject
            ?.getAsJsonObject("content")
            ?.getAsJsonArray("parts")
            ?.get(0)?.asJsonObject
            ?.get("text")?.asString
            ?.trim()
            ?.removePrefix("```json")
            ?.removePrefix("```")
            ?.removeSuffix("```")
            ?.trim()
            ?: throw Exception("Не вдалося отримати текст відповіді")

        gson.fromJson(content, Food::class.java)
    }
}