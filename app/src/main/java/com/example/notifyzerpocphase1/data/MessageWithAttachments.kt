package com.example.notifyzerpocphase1.data

import androidx.room.Embedded
import androidx.room.Relation

data class MessageWithAttachments(
    @Embedded val message: MessageRecord,
    @Relation(
        parentColumn = "id",
        entityColumn = "messageId"
    )
    val attachments: List<AttachmentRecord>
)
