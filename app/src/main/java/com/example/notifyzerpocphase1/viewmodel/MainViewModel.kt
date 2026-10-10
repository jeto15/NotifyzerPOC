package com.example.notifyzerpocphase1.viewmodel

import android.content.Context
import android.provider.Telephony
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notifyzerpocphase1.data.AppDatabase
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

import com.example.notifyzerpocphase1.repository.GeminiRepository
import com.example.notifyzerpocphase1.util.ApiKeyManager
import com.example.notifyzerpocphase1.util.DossierPromptBuilder
import com.example.notifyzerpocphase1.util.JsonConversationImporter
import com.example.notifyzerpocphase1.util.MockDataImporter
import kotlinx.coroutines.flow.update

class MainViewModel : ViewModel() {
    companion object {
        val TARGET_POC_NUMBERS = listOf("09634255141", "09558015949", MockDataImporter.MOCK_PHONE_NUMBER)
    }

    private val geminiRepository = GeminiRepository()

    private val _notifications = NotificationRepository.notifications
    val notifications: StateFlow<List<CapturedNotification>> = _notifications
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _dossierState = MutableStateFlow<Map<String, String>>(emptyMap())
    val dossierState: StateFlow<Map<String, String>> = _dossierState.asStateFlow()

    private val _isLoadingDossier = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val isLoadingDossier: StateFlow<Map<String, Boolean>> = _isLoadingDossier.asStateFlow()

    private val _dossierError = MutableStateFlow<Map<String, String>>(emptyMap())
    val dossierError: StateFlow<Map<String, String>> = _dossierError.asStateFlow()

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

    fun generateDossierForContact(
        context: Context,
        senderKey: String,
        groupLogs: List<CapturedNotification>
    ) {
        val apiKey = ApiKeyManager.getApiKey(context)
        if (apiKey.isBlank()) {
            _dossierError.update { it + (senderKey to "Please set your Gemini API key in Settings (BYOK).") }
            return
        }

        viewModelScope.launch {
            _isLoadingDossier.update { it + (senderKey to true) }
            _dossierError.update { it - senderKey }

            val displayName = ContactUtils.getContactName(context, senderKey)
            
            val db = AppDatabase.getDatabase(context)
            val profileDao = db.entityProfileDao()
            val entityProfile = profileDao.getProfile(senderKey)

            val prompt = DossierPromptBuilder.buildPrompt(
                displayName = displayName,
                groupLogs = groupLogs,
                entityProfile = entityProfile
            )

            val result = geminiRepository.generateDossier(apiKey = apiKey, prompt = prompt)

            result.onSuccess { text ->
                _dossierState.update { it + (senderKey to text) }
                _isLoadingDossier.update { it + (senderKey to false) }
            }.onFailure { exception ->
                _dossierError.update { it + (senderKey to (exception.localizedMessage ?: "Failed to generate AI Dossier.")) }
                _isLoadingDossier.update { it + (senderKey to false) }
            }
        }
    }

    private val _isPermissionGranted = MutableStateFlow(false)
    val isPermissionGranted: StateFlow<Boolean> = _isPermissionGranted.asStateFlow()

    fun checkPermission(context: Context) {
        _isPermissionGranted.value = PermissionUtils.isNotificationListenerGranted(context)
        // Phase 2: Removed auto-sync on install. History is now fetched lazily via Smart Triggers.
    }

    /**
     * Trigger A (Manual) / Trigger B (Incoming) / Trigger C (Outgoing)
     * Fetches historical SMS only when explicitly triggered for a specific contact.
     */
    fun syncContactHistoryIfNeeded(context: Context, contactNumber: String, isBackground: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            // Check if we've already synced this contact
            val existingCount = db.notificationDao().countMessagesForContact(contactNumber)
            if (existingCount > 0) return@launch // Already analyzed/synced

            val smsRepository = SmsRepository(context)
            val conversation = smsRepository.getConversation(contactNumber)
            
            conversation.forEach { sms ->
                val isSent = sms.type == Telephony.Sms.MESSAGE_TYPE_SENT
                val notification = CapturedNotification(
                    packageName = if (isSent) "historical.sms.sync.sent" else "historical.sms.sync",
                    title = contactNumber,
                    text = sms.body,
                    timestamp = sms.date
                )
                NotificationRepository.addNotification(notification)
            }
            
            // If background trigger (B or C), we can also fire off the AI dossier generation silently here
            if (isBackground && conversation.isNotEmpty()) {
                generateDossierForContact(context, contactNumber, conversation.map { 
                    CapturedNotification(0, if (it.type == Telephony.Sms.MESSAGE_TYPE_SENT) "historical.sms.sync.sent" else "historical.sms.sync", contactNumber, it.body, it.date) 
                })
            }
        }
    }

    fun clearLogs() {
        NotificationRepository.clearNotifications()
    }

    fun loadMockData(context: Context) {
        viewModelScope.launch {
            MockDataImporter.injectMockConversation(context)
        }
    }

    fun importJsonConversation(context: Context, jsonString: String, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            val result = JsonConversationImporter.importJsonConversation(context, jsonString)
            onResult(result)
        }
    }
}
