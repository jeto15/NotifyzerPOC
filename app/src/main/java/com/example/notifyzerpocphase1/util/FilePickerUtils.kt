package com.example.notifyzerpocphase1.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import androidx.core.content.FileProvider

object FilePickerUtils {

    fun getFileDetails(context: Context, uri: Uri): Pair<String, Long> {
        var name = "attachment"
        var size = 0L
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) name = cursor.getString(nameIndex)
                    if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Pair(name, size)
    }

    fun getMimeType(context: Context, uri: Uri): String {
        return context.contentResolver.getType(uri) ?: "application/octet-stream"
    }

    // Creates a temporary file and returns its URI for the Camera to write to
    fun createTempImageUri(context: Context): Uri {
        val tempFile = File.createTempFile("capture_", ".jpg", context.cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
    }
}
