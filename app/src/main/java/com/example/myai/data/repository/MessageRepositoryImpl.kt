package com.example.myai.data.repository

import com.example.myai.data.source.LocalChatDataSource
import com.example.myai.domain.model.ChatMessage
import com.example.myai.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow

class MessageRepositoryImpl(
    private val localDataSource: LocalChatDataSource
) : MessageRepository {
    override fun getMessages(): Flow<List<ChatMessage>> = localDataSource.getMessages()
    
    override suspend fun saveMessage(message: ChatMessage) {
        localDataSource.saveMessage(message)
    }

    override suspend fun clearMessages() {
        localDataSource.clearMessages()
    }

    override suspend fun deleteMessage(id: String) {
        localDataSource.deleteMessage(id)
    }
}
