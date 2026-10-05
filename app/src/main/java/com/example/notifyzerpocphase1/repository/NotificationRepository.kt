package com.example.notifyzerpocphase1.repository

import android.content.Context
import com.example.notifyzerpocphase1.data.AppDatabase
import com.example.notifyzerpocphase1.data.NotificationDao
import com.example.notifyzerpocphase1.data.NotificationEntity
import com.example.notifyzerpocphase1.model.CapturedNotification
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

object NotificationRepository {
    private var notificationDao: NotificationDao? = null

    fun initialize(context: Context) {
        if (notificationDao == null) {
            val db = AppDatabase.getDatabase(context)
            notificationDao = db.notificationDao()
        }
    }

    fun setDaoForTesting(dao: NotificationDao) {
        notificationDao = dao
    }

    private val fallbackNotifications = MutableStateFlow<List<CapturedNotification>>(emptyList())

    val notifications: Flow<List<CapturedNotification>>
        get() {
            return notificationDao?.getAllNotifications()?.map { entities ->
                entities.map { it.toDomainModel() }
            } ?: fallbackNotifications.asStateFlow()
        }

    fun addNotification(notification: CapturedNotification) {
        val dao = notificationDao
        if (dao != null) {
            val isDuplicate = dao.countDuplicate(
                packageName = notification.packageName,
                text = notification.text,
                timestamp = notification.timestamp
            ) > 0
            if (!isDuplicate) {
                dao.insertNotification(NotificationEntity.fromDomainModel(notification))
            }
        } else {
            fallbackNotifications.update { current ->
                val isDuplicate = current.any {
                    it.packageName == notification.packageName &&
                    it.text == notification.text &&
                    it.timestamp == notification.timestamp
                }
                if (isDuplicate) current else listOf(notification) + current
            }
        }
    }

    fun clearNotifications() {
        val dao = notificationDao
        if (dao != null) {
            dao.clearNotifications()
        }
        fallbackNotifications.value = emptyList()
    }
}
