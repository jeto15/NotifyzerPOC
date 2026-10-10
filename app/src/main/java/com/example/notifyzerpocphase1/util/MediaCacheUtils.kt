package com.example.notifyzerpocphase1.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

object MediaCacheUtils {

    suspend fun persistAndCacheUri(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        // 1. Take persistable permission if supported (SAF / Google Drive)
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (e: Exception) {
            // Not all URIs support persistable permissions (e.g., gallery picks)
        }

        // 2. Copy file into internal app storage (filesDir/attachments/) for offline reliability
        val attachmentsDir = File(context.filesDir, "attachments").apply {
            if (!exists()) mkdirs()
        }

        val extension = getFileExtension(context, uri) ?: "bin"
        val cachedFile = File(attachmentsDir, "${UUID.randomUUID()}.$extension")

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            cachedFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        Uri.fromFile(cachedFile).toString()
    }

    private fun getFileExtension(context: Context, uri: Uri): String? {
        val mimeType = context.contentResolver.getType(uri)
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
    }
}
