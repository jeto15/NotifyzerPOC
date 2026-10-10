package com.example.notifyzerpocphase1.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.notifyzerpocphase1.data.AppDatabase
import com.example.notifyzerpocphase1.data.AttachmentRecord
import com.example.notifyzerpocphase1.data.NotificationEntity
import com.example.notifyzerpocphase1.model.StagedAttachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    
    private val db = AppDatabase.getDatabase(application)
    private val notificationDao = db.notificationDao()
    
    // --- Input State ---
    private val _messageText = MutableStateFlow("")
    val messageText: StateFlow<String> = _messageText.asStateFlow()

    private val _stagedAttachments = MutableStateFlow<List<StagedAttachment>>(emptyList())
    val stagedAttachments: StateFlow<List<StagedAttachment>> = _stagedAttachments.asStateFlow()

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
                // We will need to add an insert method to notificationDao or a dedicated attachmentDao
                // For now, let's assume we add an insertAttachment method to notificationDao
                notificationDao.insertAttachments(attachmentRecords)
            }

            // Clear UI State
            _messageText.value = ""
            _stagedAttachments.value = emptyList()
        }
    }
}
