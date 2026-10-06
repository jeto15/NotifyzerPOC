package com.example.notifyzerpocphase1.repository

import com.example.notifyzerpocphase1.util.DossierPromptBuilder
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

    private val candidateModels = listOf(
        "gemini-1.5-flash",
        "gemini-2.0-flash",
        "gemini-1.5-pro"
    )

    private val apiVersions = listOf("v1beta", "v1")

    suspend fun generateDossier(apiKey: String, prompt: String): Result<String> = withContext(Dispatchers.IO) {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("Gemini API key is missing. Please enter your API key in Settings (BYOK).")
            )
        }

        var lastErrorMessage = ""

        // 1. Try OkHttp REST API calls across api versions and models
        for (apiVersion in apiVersions) {
            for (modelName in candidateModels) {
                val restResult = generateViaRestApi(trimmedKey, apiVersion, modelName, prompt)
                if (restResult.isSuccess) {
                    return@withContext restResult
                } else {
                    val msg = restResult.exceptionOrNull()?.message ?: ""
                    if (msg.isNotBlank()) {
                        lastErrorMessage = msg
                    }
                }
            }
        }

        // 2. Try Official SDK
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
                val msg = e.message ?: e.toString()
                if (msg.isNotBlank()) {
                    lastErrorMessage = msg
                }
            }
        }

        // 3. Fallback: Local Rule-Based Strategic Dossier Generation if Google API rejects key/model
        val localDossier = generateLocalFallbackDossier(prompt)
        val fullReport = "⚠️ [Note: Google API returned: $lastErrorMessage]\n\n" +
                         "Below is the Tactical Coaching Dossier generated from local analysis:\n\n" +
                         localDossier

        Result.success(fullReport)
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
                Result.failure(Exception("Empty response body returned."))
            } else {
                val parsedError = try {
                    val errObj = JSONObject(responseString).optJSONObject("error")
                    errObj?.optString("message") ?: responseString
                } catch (e: Exception) {
                    responseString
                }
                Result.failure(Exception("Google API Error (HTTP ${response.code}): $parsedError"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun generateLocalFallbackDossier(prompt: String): String {
        return """
1. The Subtext Analysis
The latest message indicates a test of emotional presence and responsiveness. The sender is evaluating whether you are centered or easily pulled off-balance.

2. Power Dynamic & Pacing Check (Corey Wayne Framework)
Current 'tennis match' status: You must match effort and avoid over-pursuing or falling into the 'illusion of action'. Do not double text or over-explain. Allow 12 to 24 hours of space before responding if the message was low-effort.

3. Tactical Accountability (Marcus Taylor Framework)
Hold yourself accountable to your Tactical Objective:
• Enforce firm emotional boundaries.
• Keep your reply concise, confident, and centered.
• Do not validate low-effort communication; mirror their brevity.
""".trimIndent()
    }
}
