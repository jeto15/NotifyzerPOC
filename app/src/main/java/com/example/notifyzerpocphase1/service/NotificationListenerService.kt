package com.example.notifyzerpocphase1.service

import android.app.Notification
import android.provider.Telephony
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.notifyzerpocphase1.model.CapturedNotification
import com.example.notifyzerpocphase1.repository.NotificationRepository

class NotificationListenerService : NotificationListenerService() {

    override fun onCreate() {
        super.onCreate()
        NotificationRepository.initialize(applicationContext)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return
        val notification = sbn.notification ?: return
        
        // Filter out ongoing events, foreground services, and group summaries
        if ((notification.flags and Notification.FLAG_ONGOING_EVENT) != 0) return
        if ((notification.flags and Notification.FLAG_FOREGROUND_SERVICE) != 0) return
        if ((notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return

        val defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(this)
        val commonSmsPackages = listOf(
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging",
            "com.android.mms"
        )

        val isSms = if (defaultSmsPackage != null) {
            packageName == defaultSmsPackage
        } else {
            packageName in commonSmsPackages
        }

        if (!isSms) {
            return
        }

        val extras = notification.extras

        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        
        // Ensure that EXTRA_TEXT is not null and not blank
        if (text.isNullOrBlank()) {
            return
        }

        val timestamp = sbn.postTime.takeIf { it > 0 } ?: System.currentTimeMillis()

        val capturedNotification = CapturedNotification(
            packageName = packageName,
            title = title,
            text = text,
            timestamp = timestamp
        )

        NotificationRepository.addNotification(capturedNotification)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        // Optional handling when notification is removed
    }
}
