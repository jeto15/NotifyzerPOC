package com.example.notifyzerpocphase1.repository

import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiRepository {

    suspend fun generateDossier(apiKey: String, prompt: String): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Gemini API key is missing. Please enter your API key in Settings."))
        }

        try {
            val generativeModel = GenerativeModel(
                modelName = "gemini-1.5-flash",
                apiKey = apiKey
            )
            val response = generativeModel.generateContent(prompt)
            val text = response.text
            if (!text.isNullOrBlank()) {
                Result.success(text)
            } else {
                Result.failure(Exception("Gemini returned an empty response."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
