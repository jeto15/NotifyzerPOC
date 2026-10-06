package com.example.notifyzerpocphase1.repository

import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiRepository {

    private val supportedModels = listOf(
        "gemini-2.0-flash",
        "gemini-1.5-flash-latest",
        "gemini-1.5-flash",
        "gemini-1.5-pro-latest",
        "gemini-1.5-pro"
    )

    suspend fun generateDossier(apiKey: String, prompt: String): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("Gemini API key is missing. Please enter your API key in Settings (BYOK).")
            )
        }

        var lastException: Exception? = null

        for (modelName in supportedModels) {
            try {
                val generativeModel = GenerativeModel(
                    modelName = modelName,
                    apiKey = apiKey
                )
                val response = generativeModel.generateContent(prompt)
                val text = response.text
                if (!text.isNullOrBlank()) {
                    return@withContext Result.success(text)
                }
            } catch (e: Exception) {
                lastException = e
                val errorMsg = e.message ?: ""
                if (errorMsg.contains("404") || errorMsg.contains("NOT_FOUND") || errorMsg.contains("not found")) {
                    continue
                } else {
                    return@withContext Result.failure(e)
                }
            }
        }

        Result.failure(
            lastException ?: Exception("Failed to generate AI Dossier with supported Gemini models.")
        )
    }
}
