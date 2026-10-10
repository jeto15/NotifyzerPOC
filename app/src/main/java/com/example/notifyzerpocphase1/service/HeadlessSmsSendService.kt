package com.example.notifyzerpocphase1.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

class HeadlessSmsSendService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Headless SMS Send Service required for ROLE_SMS
        // Handles android.intent.action.RESPOND_VIA_MESSAGE
        return START_NOT_STICKY
    }
}
