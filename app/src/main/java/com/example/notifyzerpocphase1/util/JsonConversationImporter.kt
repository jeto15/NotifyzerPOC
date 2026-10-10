package com.example.notifyzerpocphase1.util

import android.content.Context
import com.example.notifyzerpocphase1.data.AppDatabase
import com.example.notifyzerpocphase1.data.EntityProfile
import com.example.notifyzerpocphase1.data.NotificationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object JsonConversationImporter {

    suspend fun importJsonConversation(context: Context, jsonString: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            val phoneNumber = root.optString("phoneNumber").trim()

            if (phoneNumber.isBlank()) {
                return@withContext Result.failure(
                    IllegalArgumentException("Missing required 'phoneNumber' field in JSON root.")
                )
            }

            // 1. Parse & Insert EntityProfile
            val displayName = root.optString("displayName", phoneNumber)
            val identityRole = root.optString("identityRole", "Unknown Role")
            val history = root.optString("history", "No historical context provided.")
            val currentDynamic = root.optString("currentDynamic", "Active communication thread.")
            val tacticalObjective = root.optString("tacticalObjective", "Maintain strategic positioning and clear communication.")

            val profile = EntityProfile(
                phoneNumber = phoneNumber,
                displayName = displayName,
                identityRole = identityRole,
                history = history,
                currentDynamic = currentDynamic,
                tacticalObjective = tacticalObjective
            )

            val db = AppDatabase.getDatabase(context)
            db.entityProfileDao().insertProfile(profile)

            // 2. Parse & Insert Messages
            val messagesArray = root.optJSONArray("messages") ?: JSONArray()
            var importedCount = 0

            for (i in 0 until messagesArray.length()) {
                val msgObj = messagesArray.getJSONObject(i)
                val direction = msgObj.optString("direction", "incoming").lowercase()
                val isOutgoing = direction == "outgoing" || direction == "sent"
                val text = msgObj.optString("text").trim()
                val timestamp = msgObj.optLong("timestamp", System.currentTimeMillis() - ((messagesArray.length() - i) * 60000L))

                if (text.isNotBlank()) {
                    val packageName = if (isOutgoing) "historical.sms.sync.sent" else "historical.sms.sync"
                    val entity = NotificationEntity(
                        packageName = packageName,
                        title = phoneNumber,
                        text = text,
                        timestamp = timestamp
                    )

                    if (db.notificationDao().countDuplicate(entity.packageName, entity.text, entity.timestamp) == 0) {
                        db.notificationDao().insertNotification(entity)
                        importedCount++
                    }
                }
            }

            Result.success("Successfully imported conversation for $phoneNumber ($importedCount new messages).")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
