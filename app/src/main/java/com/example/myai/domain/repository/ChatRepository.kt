package com.example.myai.domain.repository

import com.example.myai.data.model.ChatRequest
import com.example.myai.data.model.ChatResponse
import com.example.myai.domain.model.ChatMessage

import com.example.myai.domain.model.AiServiceType

import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    suspend fun sendMessage(request: ChatRequest, serviceType: AiServiceType): Result<ChatResponse>
    fun sendMessageStream(request: ChatRequest, serviceType: AiServiceType): Flow<String>
}
