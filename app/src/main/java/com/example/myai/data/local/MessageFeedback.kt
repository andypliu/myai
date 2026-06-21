package com.example.myai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "message_feedback")
data class MessageFeedback(
    @PrimaryKey val messageId: String,
    val model: String,
    val content: String,
    val timestamp: Long,
    val isLiked: Boolean // true for like, false for dislike
)
