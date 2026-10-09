package com.example.notifyzerpocphase1.util

import android.content.Context
import android.util.Log
import com.example.notifyzerpocphase1.data.AppDatabase
import com.example.notifyzerpocphase1.data.EntityProfile
import com.example.notifyzerpocphase1.data.NotificationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MockDataImporter {
    const val MOCK_PHONE_NUMBER = "+15550199999"
    private const val TAG = "MockDataImporter"

    suspend fun injectMockConversation(context: Context) = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val profileDao = db.entityProfileDao()
        val notificationDao = db.notificationDao()
        
        // 1. Define the Mock Profile
        val mockProfile = EntityProfile(
            phoneNumber = MOCK_PHONE_NUMBER,
            identityRole = "Co-founder / Business Partner",
            history = "Known for 3 years, built a startup together. History of missed deadlines.",
            currentDynamic = "Tense but professional. Relying on them for Q3 deliverables.",
            tacticalObjective = "Enforce firm boundaries on delivery dates without causing a fracture in the working relationship."
        )
        
        // Insert into Room
        profileDao.insertProfile(mockProfile)
        Log.d(TAG, "Injected mock profile for $MOCK_PHONE_NUMBER")
        
        // 2. Define the Mock Messages
        val baseTime = System.currentTimeMillis() - 86400000L // 1 day ago
        val mockMessages = listOf(
            NotificationEntity(
                packageName = "historical.sms.sync",
                title = MOCK_PHONE_NUMBER,
                text = "Hey, I know I promised the deliverables by tomorrow, but I might need an extension until Friday.",
                timestamp = baseTime
            ),
            NotificationEntity(
                packageName = "historical.sms.sync.sent",
                title = MOCK_PHONE_NUMBER,
                text = "We committed to tomorrow for the client. What exactly is blocking you from finishing by EOD?",
                timestamp = baseTime + 1800000L // +30 mins
            ),
            NotificationEntity(
                packageName = "historical.sms.sync",
                title = MOCK_PHONE_NUMBER,
                text = "Just overwhelmed with the UI revisions. Can you help me out?",
                timestamp = baseTime + 3600000L // +1 hour
            ),
            NotificationEntity(
                packageName = "historical.sms.sync.sent",
                title = MOCK_PHONE_NUMBER,
                text = "I can handle the CSS tweaks if you finish the core logic. Let's stick to the deadline.",
                timestamp = baseTime + 5400000L // +1.5 hours
            )
        )
        
        // Insert Messages safely
        mockMessages.forEach { msg ->
            if (notificationDao.countDuplicate(msg.packageName, msg.text, msg.timestamp) == 0) {
                notificationDao.insertNotification(msg)
            }
        }
        Log.d(TAG, "Injected mock messages for $MOCK_PHONE_NUMBER")

        // 3. Query Verification (Print to Logcat)
        val verifiedData = profileDao.getAllDossierData()
        Log.d(TAG, "=== QUERY VERIFICATION ===")
        verifiedData.forEach { dossier ->
            Log.d(TAG, "Profile Phone: ${dossier.profile.phoneNumber}")
            Log.d(TAG, "Total Linked Messages: ${dossier.messages.size}")
            dossier.messages.forEach { msg ->
                val direction = if (msg.packageName.endsWith(".sent")) "Outgoing" else "Incoming"
                Log.d(TAG, "  -> [$direction] ${msg.text}")
            }
        }
        Log.d(TAG, "==========================")
    }
}
