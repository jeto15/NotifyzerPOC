package com.example.notifyzerpocphase1.viewmodel

import android.app.Application
import android.provider.Telephony
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.notifyzerpocphase1.data.AppDatabase
import com.example.notifyzerpocphase1.model.CapturedNotification
import com.example.notifyzerpocphase1.repository.NotificationRepository
import com.example.notifyzerpocphase1.repository.SmsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SyncSmsViewModel(application: Application) : AndroidViewModel(application) {
    private val smsRepository = SmsRepository(application)
    private val notificationDao = AppDatabase.getDatabase(application).notificationDao()

    val uniqueSenders: StateFlow<List<String>> = notificationDao.getUniqueSenders()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun syncConversation(sender: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val conversation = smsRepository.getConversation(sender)
            
            conversation.forEach { sms ->
                val title = if (sms.type == Telephony.Sms.MESSAGE_TYPE_SENT) {
                    "Me" // Or "Owner"
                } else {
                    sender
                }
                
                val notification = CapturedNotification(
                    packageName = "historical.sms.sync",
                    title = title,
                    text = sms.body,
                    timestamp = sms.date
                )
                
                NotificationRepository.addNotification(notification)
            }
        }
    }
}
