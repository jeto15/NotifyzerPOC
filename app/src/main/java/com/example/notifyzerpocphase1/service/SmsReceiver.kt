package com.example.notifyzerpocphase1.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.notifyzerpocphase1.data.AppDatabase
import com.example.notifyzerpocphase1.data.MessageRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_DELIVER_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            val scope = CoroutineScope(Dispatchers.IO)

            scope.launch {
                val db = AppDatabase.getDatabase(context)
                val messageDao = db.messageDao()

                messages?.forEach { sms ->
                    val sender = sms.originatingAddress ?: "Unknown"
                    val body = sms.messageBody ?: ""
                    val timestamp = sms.timestampMillis

                    val messageRecord = MessageRecord(
                        entityPhoneNumber = sender,
                        sourceApp = "default.sms.incoming",
                        content = body,
                        timestamp = timestamp,
                        isIncoming = true
                    )
                    messageDao.insertMessage(messageRecord)
                }
            }
        }
    }
}
