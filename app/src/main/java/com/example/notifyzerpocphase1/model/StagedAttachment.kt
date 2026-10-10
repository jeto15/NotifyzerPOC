package com.example.notifyzerpocphase1.model

import android.net.Uri

data class StagedAttachment(
    val uri: Uri,
    val mimeType: String,
    val name: String,
    val size: Long
)
