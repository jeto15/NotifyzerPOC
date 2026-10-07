package com.example.notifyzerpocphase1.repository

import android.content.Context
import android.provider.Telephony
import com.example.notifyzerpocphase1.model.HistoricalSms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmsRepository(private val context: Context) {

    suspend fun getGroupedSms(): Map<String, List<HistoricalSms>> = withContext(Dispatchers.IO) {
        val smsList = mutableListOf<HistoricalSms>()
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE
        )

        context.contentResolver.query(uri, projection, null, null, Telephony.Sms.DATE + " DESC")?.use { cursor ->
            val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val typeIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE)

            while (cursor.moveToNext()) {
                val address = cursor.getString(addressIndex) ?: "Unknown"
                val body = cursor.getString(bodyIndex) ?: ""
                val date = cursor.getLong(dateIndex)
                val type = cursor.getInt(typeIndex)

                smsList.add(HistoricalSms(address, body, date, type))
            }
        }

        return@withContext smsList.groupBy { it.address }
    }

    suspend fun getConversation(address: String): List<HistoricalSms> = withContext(Dispatchers.IO) {
        val smsList = mutableListOf<HistoricalSms>()
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE
        )

        val digitsOnly = address.replace(Regex("[^0-9]"), "")
        val searchSuffix = if (digitsOnly.length >= 7) digitsOnly.takeLast(7) else digitsOnly

        // Tier 1: Query by digits suffix if available
        if (searchSuffix.isNotEmpty()) {
            try {
                context.contentResolver.query(
                    uri,
                    projection,
                    "${Telephony.Sms.ADDRESS} LIKE ?",
                    arrayOf("%$searchSuffix%"),
                    Telephony.Sms.DATE + " ASC"
                )?.use { cursor ->
                    val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                    val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                    val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
                    val typeIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE)

                    while (cursor.moveToNext()) {
                        val addr = cursor.getString(addressIndex) ?: "Unknown"
                        val body = cursor.getString(bodyIndex) ?: ""
                        val date = cursor.getLong(dateIndex)
                        val type = cursor.getInt(typeIndex)
                        smsList.add(HistoricalSms(addr, body, date, type))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Tier 2: Scan all messages for address match (exact, partial, or digits)
        if (smsList.isEmpty()) {
            val allMessages = mutableListOf<HistoricalSms>()
            try {
                context.contentResolver.query(
                    uri,
                    projection,
                    null,
                    null,
                    Telephony.Sms.DATE + " ASC"
                )?.use { cursor ->
                    val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                    val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                    val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
                    val typeIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE)

                    while (cursor.moveToNext()) {
                        val addr = cursor.getString(addressIndex) ?: ""
                        val body = cursor.getString(bodyIndex) ?: ""
                        val date = cursor.getLong(dateIndex)
                        val type = cursor.getInt(typeIndex)
                        allMessages.add(HistoricalSms(addr, body, date, type))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (allMessages.isNotEmpty()) {
                // Try matching address against SMS addresses in allMessages
                val matched = allMessages.filter { sms ->
                    val smsDigits = sms.address.replace(Regex("[^0-9]"), "")
                    val exactMatch = sms.address.equals(address, ignoreCase = true)
                    val containsMatch = address.contains(sms.address, ignoreCase = true) || sms.address.contains(address, ignoreCase = true)
                    val digitsMatch = searchSuffix.isNotEmpty() && smsDigits.endsWith(searchSuffix)

                    exactMatch || containsMatch || digitsMatch
                }

                if (matched.isNotEmpty()) {
                    smsList.addAll(matched)
                }
            }
        }

        return@withContext smsList
    }
}
