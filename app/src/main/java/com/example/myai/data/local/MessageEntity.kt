package com.example.myai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val content: String,
    val isUser: Boolean,
    val isTyping: Boolean,
    val timestamp: Long,
    val attachmentsJson: String? = null // Store attachments as JSON string
)
