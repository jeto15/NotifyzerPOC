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
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    private val fastModels = listOf("gemini-1.5-flash", "gemini-2.0-flash")

    suspend fun generateDossier(apiKey: String, prompt: String): Result<String> = withContext(Dispatchers.IO) {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("Gemini API key is missing. Please enter your API key in Settings (BYOK).")
            )
        }

        var lastError = ""

        // 1. Direct REST API call to fast model (1-2s ultra-low latency)
        for (modelName in fastModels) {
            val restResult = generateViaRestApi(trimmedKey, "v1beta", modelName, prompt)
            if (restResult.isSuccess) {
                return@withContext restResult
            } else {
                lastError = restResult.exceptionOrNull()?.message ?: ""
            }
        }

        // 2. Backup SDK call if REST fails
        try {
            val generativeModel = GenerativeModel(
                modelName = "gemini-1.5-flash",
                apiKey = trimmedKey
            )
            val response = generativeModel.generateContent(prompt)
            val text = response.text
            if (!text.isNullOrBlank()) {
                return@withContext Result.success(text)
            }
        } catch (e: Exception) {
            lastError = e.message ?: e.toString()
        }

        Result.failure(
            Exception("Gemini API Error: $lastError\n\nPlease check your key permissions at aistudio.google.com/app/apikey.")
        )
    }

    private fun generateViaRestApi(apiKey: String, apiVersion: String, modelName: String, prompt: String): Result<String> {
        return try {
            val isBearer = apiKey.startsWith("AQ.") || apiKey.startsWith("ya29.")
            val url = if (isBearer) {
                "https://generativelanguage.googleapis.com/$apiVersion/models/$modelName:generateContent"
            } else {
                "https://generativelanguage.googleapis.com/$apiVersion/models/$modelName:generateContent?key=$apiKey"
            }
            
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
            val requestBuilder = Request.Builder()
                .url(url)
                .post(requestBody)

            if (isBearer) {
                requestBuilder.addHeader("Authorization", "Bearer $apiKey")
            }

            val response = client.newCall(requestBuilder.build()).execute()
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
