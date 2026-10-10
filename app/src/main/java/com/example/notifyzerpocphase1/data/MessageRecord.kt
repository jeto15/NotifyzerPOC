package com.example.notifyzerpocphase1.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "message_records",
    foreignKeys = [
        ForeignKey(
            entity = EntityProfile::class,
            parentColumns = ["phoneNumber"],
            childColumns = ["entityPhoneNumber"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("entityPhoneNumber"), Index("timestamp")]
)
data class MessageRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entityPhoneNumber: String,
    val sourceApp: String, // e.g. "default.sms", "whatsapp", "historical.sms.sync"
    val content: String,
    val timestamp: Long,
    val isIncoming: Boolean
)
