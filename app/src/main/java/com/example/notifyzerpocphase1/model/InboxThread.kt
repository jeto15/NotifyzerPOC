package com.example.notifyzerpocphase1.model

data class InboxThread(
    val address: String,
    val snippet: String,
    val timestamp: Long,
    val unreadCount: Int = 0
)
