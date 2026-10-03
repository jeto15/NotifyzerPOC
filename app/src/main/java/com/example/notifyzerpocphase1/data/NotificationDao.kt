package com.example.notifyzerpocphase1.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT DISTINCT title FROM notifications WHERE title IS NOT NULL AND title != ''")
    fun getUniqueSenders(): Flow<List<String>>

    @Insert
    fun insertNotification(notification: NotificationEntity)

    @Query("DELETE FROM notifications")
    fun clearNotifications()
}
