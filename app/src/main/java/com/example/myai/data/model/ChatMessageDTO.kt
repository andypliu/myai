package com.example.myai.data.model

/**
 * Data Transfer Object for chat messages.
 * This represents the raw data structure for chat messages.
 */
data class ChatMessageDTO(
    val id: String,
    val content: String,
    val isUser: Boolean,
    val isTyping: Boolean = false,
    val attachments: List<FileAttachmentDTO>? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class FileAttachmentDTO(
    val id: String,
    val uri: String,
    val name: String,
    val mimeType: String,
    val size: Long,
    val base64Data: String? = null
)
