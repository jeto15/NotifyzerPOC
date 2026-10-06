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
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val candidateModels = listOf(
        "gemini-1.5-flash",
        "gemini-2.0-flash",
        "gemini-1.5-pro"
    )

    suspend fun generateDossier(apiKey: String, prompt: String): Result<String> = withContext(Dispatchers.IO) {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("Gemini API key is missing. Please enter your API key in Settings (BYOK).")
            )
        }

        // Try Official SDK first
        for (modelName in candidateModels) {
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
                // If error is not 404, log it and keep trying or fallback to REST
                val msg = e.message ?: ""
                if (msg.contains("API_KEY_INVALID") || msg.contains("403") || msg.contains("PERMISSION_DENIED")) {
                    return@withContext Result.failure(
                        Exception("Invalid API Key or Permission Denied. Please check your Gemini API key in Settings.")
                    )
                }
            }
        }

        // Fallback: Direct OkHttp REST API call
        for (modelName in candidateModels) {
            val restResult = generateViaRestApi(trimmedKey, modelName, prompt)
            if (restResult.isSuccess) {
                return@withContext restResult
            }
        }

        Result.failure(
            Exception(
                "Gemini API returned 404 (Not Found).\n\n" +
                "Possible Causes & Troubleshooting:\n" +
                "1. Incorrect API Key: Verify your key at aistudio.google.com/app/apikey\n" +
                "2. Uninitialized Project: Ensure 'Generative Language API' is enabled in Google Cloud Console for this key's project.\n" +
                "3. Key Restrictions: Ensure your key does not have IP/Application restrictions preventing Android calls."
            )
        )
    }

    private fun generateViaRestApi(apiKey: String, modelName: String, prompt: String): Result<String> {
        return try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
            
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
                Result.failure(Exception("Empty candidate text returned from Gemini REST API."))
            } else {
                Result.failure(Exception("HTTP ${response.code}: $responseString"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
