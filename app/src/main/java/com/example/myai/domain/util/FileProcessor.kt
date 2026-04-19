package com.example.myai.domain.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream
import java.util.UUID
import android.util.Base64
import com.example.myai.domain.model.FileAttachment

object FileProcessor {

    private const val MAX_FILE_SIZE = 10 * 1024 * 1024 // 10MB

    /**
     * Get file metadata from URI
     */
    fun getFileInfo(context: Context, uri: Uri): FileAttachment? {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)

                val name = if (nameIndex >= 0) it.getString(nameIndex) else "unknown"
                val size = if (sizeIndex >= 0) it.getLong(sizeIndex) else 0L

                // Check file size limit
                if (size > MAX_FILE_SIZE) {
                    return null
                }

                val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"

                return FileAttachment(
                    id = UUID.randomUUID().toString(),
                    uri = uri.toString(),
                    name = name,
                    mimeType = mimeType,
                    size = size
                )
            }
        }
        return null
    }

    /**
     * Convert file URI to base64 string
     */
    fun fileToBase64(context: Context, uri: Uri): String? {
        return try {
            android.util.Log.d("FileProcessor", "Converting file to base64: $uri")
            val inputStream = context.contentResolver.openInputStream(uri)
            val outputStream = ByteArrayOutputStream()
            val buffer = ByteArray(4096)
            var bytesRead: Int

            inputStream?.use { input ->
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
            }

            val bytes = outputStream.toByteArray()
            android.util.Log.d("FileProcessor", "File size: ${bytes.size} bytes")
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            android.util.Log.d("FileProcessor", "Base64 length: ${base64.length}")
            base64
        } catch (e: Exception) {
            android.util.Log.e("FileProcessor", "Error converting file to base64", e)
            null
        }
    }

    /**
     * Check if file is an image
     */
    fun isImage(mimeType: String): Boolean {
        return mimeType.startsWith("image/")
    }

    /**
     * Check if file is a document
     */
    fun isDocument(mimeType: String): Boolean {
        return mimeType.startsWith("application/") ||
               mimeType.startsWith("text/")
    }

    /**
     * Get file size in human-readable format
     */
    fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> "${size / (1024 * 1024)} MB"
        }
    }
}
