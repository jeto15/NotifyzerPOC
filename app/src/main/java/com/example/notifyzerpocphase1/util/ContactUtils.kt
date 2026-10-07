package com.example.notifyzerpocphase1.util

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import java.util.concurrent.ConcurrentHashMap

object ContactUtils {
    private val contactNameCache = ConcurrentHashMap<String, String>()

    fun getContactName(context: Context, phoneNumber: String): String {
        if (phoneNumber.isBlank() || phoneNumber == "Unknown" || phoneNumber == "Me") {
            return phoneNumber
        }

        contactNameCache[phoneNumber]?.let { return it }

        var resolvedName = phoneNumber
        try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        val name = cursor.getString(nameIndex)
                        if (!name.isNullOrBlank()) {
                            resolvedName = "$name ($phoneNumber)"
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        contactNameCache[phoneNumber] = resolvedName
        return resolvedName
    }

    fun clearCache() {
        contactNameCache.clear()
    }
}
