package com.example.notifyzerpocphase1.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT DISTINCT title FROM notifications WHERE title IS NOT NULL AND title != ''")
    fun getUniqueSenders(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM notifications WHERE packageName = :packageName AND (text = :text OR (text IS NULL AND :text IS NULL)) AND timestamp = :timestamp")
    fun countDuplicate(packageName: String, text: String?, timestamp: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertNotification(notification: NotificationEntity)

    @Query("DELETE FROM notifications")
    fun clearNotifications()
}
