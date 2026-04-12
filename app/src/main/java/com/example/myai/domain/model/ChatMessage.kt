package com.example.myai.domain.model

data class ChatMessage(
    val id: String,
    val content: String,
    val isUser: Boolean,
    val isTyping: Boolean = false,
    val attachments: List<FileAttachment>? = null,
    val timestamp: Long = System.currentTimeMillis()
)
