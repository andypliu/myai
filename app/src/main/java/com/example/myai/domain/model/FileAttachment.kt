package com.example.myai.domain.model

data class FileAttachment(
    val id: String,
    val uri: String,
    val name: String,
    val mimeType: String,
    val size: Long,
    val base64Data: String? = null  // For API transmission
)
