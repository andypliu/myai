package com.example.myai.data.repository

import com.example.myai.data.source.ChatDataSource
import com.example.myai.data.model.ChatRequest
import com.example.myai.data.model.ChatResponse
import com.example.myai.domain.repository.ChatRepository

class ChatRepositoryImpl(
    private val dataSource: ChatDataSource
) : ChatRepository {
    override suspend fun sendMessage(request: ChatRequest): Result<ChatResponse> {
        return dataSource.sendMessage(request)
    }
}
