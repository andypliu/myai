package com.example.myai.data.repository

import com.example.myai.data.remote.OllamaApiService
import com.example.myai.domain.model.ChatRequest
import com.example.myai.domain.model.ChatResponse
import com.example.myai.domain.repository.ChatRepository

class ChatRepositoryImpl(
    private val apiService: OllamaApiService
) : ChatRepository {
    override suspend fun sendMessage(request: ChatRequest): Result<ChatResponse> {
        return apiService.chat(request)
    }
}
