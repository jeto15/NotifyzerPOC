package com.example.notifyzerpocphase1.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachments(attachments: List<AttachmentRecord>)

    @Transaction
    suspend fun insertMessageWithAttachments(message: MessageRecord, attachments: List<AttachmentRecord>): Long {
        val messageId = insertMessage(message)
        if (attachments.isNotEmpty()) {
            val mappedAttachments = attachments.map { it.copy(messageId = messageId) }
            insertAttachments(mappedAttachments)
        }
        return messageId
    }

    @Transaction
    @Query("SELECT * FROM message_records WHERE entityPhoneNumber = :phoneNumber ORDER BY timestamp ASC")
    fun getMessagesForContact(phoneNumber: String): Flow<List<MessageWithAttachments>>

    @Query("SELECT COUNT(*) FROM message_records WHERE entityPhoneNumber = :phoneNumber")
    suspend fun countMessagesForContact(phoneNumber: String): Int
}
