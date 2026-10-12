package com.example.notifyzerpocphase1.service

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.notifyzerpocphase1.data.AppDatabase
import com.example.notifyzerpocphase1.data.MessageRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("SmsReceiver", "Received intent: ${intent.action}")
        
        if (intent.action == Telephony.Sms.Intents.SMS_DELIVER_ACTION || 
            intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages == null || messages.isEmpty()) return
            
            val scope = CoroutineScope(Dispatchers.IO)

            scope.launch {
                val db = AppDatabase.getDatabase(context)
                val messageDao = db.messageDao()

                // Group multi-part messages
                val sender = messages[0]?.originatingAddress ?: "Unknown"
                val body = messages.joinToString(separator = "") { it?.messageBody ?: "" }
                val timestamp = messages[0]?.timestampMillis ?: System.currentTimeMillis()

                Log.d("SmsReceiver", "Processing incoming SMS from $sender: $body")

                // 1. Write to our internal Room DB (so UI updates)
                val messageRecord = MessageRecord(
                    entityPhoneNumber = sender,
                    sourceApp = "default.sms.incoming",
                    content = body,
                    timestamp = timestamp,
                    isIncoming = true
                )
                messageDao.insertMessage(messageRecord)

                // 2. REQUIRED: As the Default SMS App, we MUST write incoming messages to the native Telephony Provider!
                // If we don't, the message disappears into the void and won't show up in our Inbox query.
                try {
                    val values = ContentValues().apply {
                        put(Telephony.Sms.ADDRESS, sender)
                        put(Telephony.Sms.BODY, body)
                        put(Telephony.Sms.DATE, timestamp)
                        put(Telephony.Sms.DATE_SENT, timestamp)
                        put(Telephony.Sms.READ, 0)
                        put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
                    }
                    context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
                    Log.d("SmsReceiver", "Successfully wrote incoming SMS to native Telephony provider.")
                } catch (e: Exception) {
                    Log.e("SmsReceiver", "Failed to write incoming SMS to native provider", e)
                }
            }
        }
    }
}
