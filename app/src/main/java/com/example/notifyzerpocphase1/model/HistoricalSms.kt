package com.example.notifyzerpocphase1.model

data class HistoricalSms(
    val address: String,
    val body: String,
    val date: Long,
    val type: Int // 1 = Inbox, 2 = Sent, etc.
)
