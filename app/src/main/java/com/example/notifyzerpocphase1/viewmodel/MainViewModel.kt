package com.example.notifyzerpocphase1.viewmodel

import android.content.Context
import android.provider.Telephony
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notifyzerpocphase1.model.CapturedNotification
import com.example.notifyzerpocphase1.repository.NotificationRepository
import com.example.notifyzerpocphase1.repository.SmsRepository
import com.example.notifyzerpocphase1.util.PermissionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.map

import com.example.notifyzerpocphase1.util.ContactUtils

class MainViewModel : ViewModel() {
    companion object {
        val TARGET_POC_NUMBERS = listOf("09634255141", "09558015949")
    }

    private val _notifications = NotificationRepository.notifications
    val notifications: StateFlow<List<CapturedNotification>> = _notifications
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun getFilteredNotifications(context: Context, list: List<CapturedNotification>): List<CapturedNotification> {
        return list.filter { isTargetPocNotification(it, context) }
    }

    fun isTargetPocNotification(item: CapturedNotification, context: Context): Boolean {
        if (item.packageName.startsWith("historical.sms.sync")) return true

        val rawTitle = item.title ?: ""
        val titleDigits = rawTitle.replace(Regex("[^0-9]"), "")
        val textDigits = item.text?.replace(Regex("[^0-9]"), "") ?: ""

        return TARGET_POC_NUMBERS.any { targetNumber ->
            val contactName = ContactUtils.getContactName(context, targetNumber)
            val targetDigits = targetNumber.replace(Regex("[^0-9]"), "").takeLast(9)

            val matchesTitleDigits = targetDigits.isNotEmpty() && titleDigits.endsWith(targetDigits)
            val matchesTextDigits = targetDigits.isNotEmpty() && textDigits.endsWith(targetDigits)
            val containsRaw = rawTitle.contains(targetNumber) || item.text?.contains(targetNumber) == true
            val matchesContactName = contactName.isNotBlank() && (
                rawTitle.contains(contactName, ignoreCase = true) || contactName.contains(rawTitle, ignoreCase = true)
            )

            matchesTitleDigits || matchesTextDigits || containsRaw || matchesContactName
        }
    }

    private val _isPermissionGranted = MutableStateFlow(false)
    val isPermissionGranted: StateFlow<Boolean> = _isPermissionGranted.asStateFlow()

    fun checkPermission(context: Context) {
        _isPermissionGranted.value = PermissionUtils.isNotificationListenerGranted(context)
        autoSyncTargetNumber(context)
    }

    fun autoSyncTargetNumber(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val smsRepository = SmsRepository(context)

            TARGET_POC_NUMBERS.forEach { targetNumber ->
                val conversation = smsRepository.getConversation(targetNumber)
                
                conversation.forEach { sms ->
                    val isSent = sms.type == Telephony.Sms.MESSAGE_TYPE_SENT
                    
                    val notification = CapturedNotification(
                        packageName = if (isSent) "historical.sms.sync.sent" else "historical.sms.sync",
                        title = targetNumber,
                        text = sms.body,
                        timestamp = sms.date
                    )
                    
                    NotificationRepository.addNotification(notification)
                }
            }
        }
    }

    fun clearLogs() {
        NotificationRepository.clearNotifications()
    }
}
