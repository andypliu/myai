package com.example.myai.domain.repository

import com.example.myai.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun getMessages(): Flow<List<ChatMessage>>
    suspend fun saveMessage(message: ChatMessage)
    suspend fun clearMessages()
    suspend fun deleteMessage(id: String)
}
