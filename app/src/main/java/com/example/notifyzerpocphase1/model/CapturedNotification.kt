package com.example.notifyzerpocphase1.model

data class CapturedNotification(
    val id: Long = 0L,
    val packageName: String,
    val title: String?,
    val text: String?,
    val timestamp: Long
)
