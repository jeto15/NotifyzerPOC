package com.example.notifyzerpocphase1.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.notifyzerpocphase1.model.CapturedNotification

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val title: String?,
    val text: String?,
    val timestamp: Long
) {
    fun toDomainModel(): CapturedNotification {
        return CapturedNotification(
            id = id,
            packageName = packageName,
            title = title,
            text = text,
            timestamp = timestamp
        )
    }

    companion object {
        fun fromDomainModel(notification: CapturedNotification): NotificationEntity {
            return NotificationEntity(
                id = 0L,
                packageName = notification.packageName,
                title = notification.title,
                text = notification.text,
                timestamp = notification.timestamp
            )
        }
    }
}
