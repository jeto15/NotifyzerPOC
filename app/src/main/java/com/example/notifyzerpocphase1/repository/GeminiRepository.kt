package com.example.notifyzerpocphase1.repository

import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val defaultModels = listOf(
        "gemini-1.5-flash",
        "gemini-2.0-flash",
        "gemini-1.5-pro",
        "gemini-1.5-flash-latest"
    )

    suspend fun generateDossier(apiKey: String, prompt: String): Result<String> = withContext(Dispatchers.IO) {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("Gemini API key is missing. Please enter your API key in Settings (BYOK).")
            )
        }

        if (!trimmedKey.startsWith("AIzaSy")) {
            return@withContext Result.failure(
                IllegalArgumentException(
                    "Key Format Warning: Google AI Studio keys start with 'AIzaSy...'.\n" +
                    "Your key starts with '${trimmedKey.take(8)}...'.\n\n" +
                    "Please make sure you copied the API Key from aistudio.google.com/app/apikey (not an OAuth or project token)."
                )
            )
        }

        // Dynamically discover supported models for this key
        val discoveredModels = fetchAvailableModels(trimmedKey)
        val modelsToTry = (discoveredModels + defaultModels).distinct()

        var lastErrorMessage = ""

        // 1. Try REST API with discovered models
        for (modelName in modelsToTry) {
            val restResult = generateViaRestApi(trimmedKey, "v1beta", modelName, prompt)
            if (restResult.isSuccess) {
                return@withContext restResult
            } else {
                val msg = restResult.exceptionOrNull()?.message ?: ""
                if (msg.isNotBlank()) {
                    lastErrorMessage = msg
                }
            }
        }

        // 2. Try Official SDK as backup
        for (modelName in modelsToTry) {
            try {
                val generativeModel = GenerativeModel(
                    modelName = modelName,
                    apiKey = trimmedKey
                )
                val response = generativeModel.generateContent(prompt)
                val text = response.text
                if (!text.isNullOrBlank()) {
                    return@withContext Result.success(text)
                }
            } catch (e: Exception) {
                val msg = e.message ?: e.toString()
                if (msg.isNotBlank()) {
                    lastErrorMessage = msg
                }
            }
        }

        Result.failure(
            Exception("Gemini API Error: $lastErrorMessage\n\nPlease check your key permissions at aistudio.google.com/app/apikey.")
        )
    }

    private fun fetchAvailableModels(apiKey: String): List<String> {
        val models = mutableListOf<String>()
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JSONObject(body)
                val modelArray = json.optJSONArray("models") ?: JSONArray()
                for (i in 0 until modelArray.length()) {
                    val m = modelArray.getJSONObject(i)
                    val name = m.optString("name") // e.g. "models/gemini-1.5-flash"
                    val methods = m.optJSONArray("supportedGenerationMethods")
                    val supportsGenerate = methods != null && (0 until methods.length()).any { methods.getString(it) == "generateContent" }
                    if (supportsGenerate && name.isNotBlank()) {
                        models.add(name.removePrefix("models/"))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return models
    }

    private fun generateViaRestApi(apiKey: String, apiVersion: String, modelName: String, prompt: String): Result<String> {
        return try {
            val url = "https://generativelanguage.googleapis.com/$apiVersion/models/$modelName:generateContent?key=$apiKey"
            
            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val jsonResponse = JSONObject(responseString)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text")
                        if (text.isNotBlank()) {
                            return Result.success(text)
                        }
                    }
                }
                Result.failure(Exception("Empty candidate text returned."))
            } else {
                val parsedError = try {
                    val errObj = JSONObject(responseString).optJSONObject("error")
                    errObj?.optString("message") ?: responseString
                } catch (e: Exception) {
                    responseString
                }
                Result.failure(Exception("HTTP ${response.code}: $parsedError"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
