package com.example.notifyzerpocphase1.viewmodel

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.notifyzerpocphase1.data.AppDatabase
import com.example.notifyzerpocphase1.data.AttachmentRecord
import com.example.notifyzerpocphase1.data.NotificationEntity
import com.example.notifyzerpocphase1.model.HistoricalSms
import com.example.notifyzerpocphase1.model.StagedAttachment
import com.example.notifyzerpocphase1.repository.SmsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    
    private val db = AppDatabase.getDatabase(application)
    private val notificationDao = db.notificationDao()
    
    // --- State ---
    private val _conversation = MutableStateFlow<List<HistoricalSms>>(emptyList())
    val conversation: StateFlow<List<HistoricalSms>> = _conversation.asStateFlow()

    // --- Input State ---
    private val _messageText = MutableStateFlow("")
    val messageText: StateFlow<String> = _messageText.asStateFlow()

    private val _stagedAttachments = MutableStateFlow<List<StagedAttachment>>(emptyList())
    val stagedAttachments: StateFlow<List<StagedAttachment>> = _stagedAttachments.asStateFlow()

    fun loadConversation(context: Context, contactNumber: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val smsRepo = SmsRepository(context)
            _conversation.value = smsRepo.getConversation(contactNumber)
        }
    }

    fun updateMessageText(text: String) {
        _messageText.value = text
    }

    fun addAttachment(context: Context, uri: Uri, mimeType: String, name: String, size: Long) {
        // Take persistable permission if possible (SAF)
        try {
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        } catch (e: SecurityException) {
            // Ignore, not all URIs support persistable permissions (e.g., photo picker might not need it)
        }

        val newAttachment = StagedAttachment(uri, mimeType, name, size)
        _stagedAttachments.value = _stagedAttachments.value + newAttachment
    }

    fun removeAttachment(attachment: StagedAttachment) {
        _stagedAttachments.value = _stagedAttachments.value.filter { it != attachment }
    }

    // --- Dispatch Logic ---
    fun sendMessage(targetContactNumber: String) {
        val currentText = _messageText.value.trim()
        val attachments = _stagedAttachments.value.toList()

        if (currentText.isBlank() && attachments.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            // 1. Create the Message Record (outgoing)
            val messageEntity = NotificationEntity(
                packageName = "historical.sms.sync.sent", // Marks as outgoing in our current schema
                title = targetContactNumber,
                text = currentText.ifBlank { "[Media Attachment]" },
                timestamp = System.currentTimeMillis()
            )
            
            // 2. Insert Message & retrieve generated ID
            val insertedId = notificationDao.insertNotificationAndGetId(messageEntity)

            // 3. Insert Attachments mapped to this message
            val attachmentRecords = attachments.map {
                AttachmentRecord(
                    messageId = insertedId,
                    uri = it.uri.toString(),
                    mimeType = it.mimeType,
                    fileName = it.name,
                    fileSizeBytes = it.size
                )
            }
            if (attachmentRecords.isNotEmpty()) {
                notificationDao.insertAttachments(attachmentRecords)
            }

            // 4. Send actual SMS via SmsManager if text is present
            if (currentText.isNotBlank()) {
                val context = getApplication<Application>()
                try {
                    val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        context.getSystemService(SmsManager::class.java)
                    } else {
                        SmsManager.getDefault()
                    }
                    smsManager.sendTextMessage(targetContactNumber, null, currentText, null, null)

                    // Because we are the Default SMS app, we MUST write the outgoing message to the native Telephony provider
                    // otherwise it won't show up in the history correctly.
                    val values = ContentValues().apply {
                        put(Telephony.Sms.ADDRESS, targetContactNumber)
                        put(Telephony.Sms.BODY, currentText)
                        put(Telephony.Sms.DATE, System.currentTimeMillis())
                        put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
                        put(Telephony.Sms.READ, 1)
                    }
                    context.contentResolver.insert(Telephony.Sms.Sent.CONTENT_URI, values)
                    
                    // Reload conversation after sending
                    loadConversation(context, targetContactNumber)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Clear UI State
            _messageText.value = ""
            _stagedAttachments.value = emptyList()
        }
    }
}
