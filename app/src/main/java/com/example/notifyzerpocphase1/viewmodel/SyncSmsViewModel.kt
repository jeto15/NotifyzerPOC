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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.example.notifyzerpocphase1.model.HistoricalSms
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SyncSmsViewModel(application: Application) : AndroidViewModel(application) {
    private val smsRepository = SmsRepository(application)
    private val notificationDao = AppDatabase.getDatabase(application).notificationDao()

    private val _syncedConversations = MutableStateFlow<Map<String, List<HistoricalSms>>>(emptyMap())
    val syncedConversations: StateFlow<Map<String, List<HistoricalSms>>> = _syncedConversations.asStateFlow()

    private val historicalSenders = flow {
        try {
            val grouped = smsRepository.getGroupedSms()
            emit(grouped.keys.toList())
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    val uniqueSenders: StateFlow<List<String>> = combine(
        notificationDao.getUniqueSenders(),
        historicalSenders
    ) { capturedSenders, smsSenders ->
        (smsSenders + capturedSenders).filter { it.isNotBlank() && it != "Me" }.distinct()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun syncConversation(sender: String, onResult: (List<HistoricalSms>) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val conversation = smsRepository.getConversation(sender)
            
            conversation.forEach { sms ->
                val title = if (sms.type == Telephony.Sms.MESSAGE_TYPE_SENT) {
                    "Me"
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

            _syncedConversations.update { current ->
                current + (sender to conversation)
            }

            withContext(Dispatchers.Main) {
                onResult(conversation)
            }
        }
    }
}
