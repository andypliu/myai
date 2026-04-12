package com.example.myai.domain.repository

import com.example.myai.domain.model.ChatMessage
import com.example.myai.domain.model.ChatRequest
import com.example.myai.domain.model.ChatResponse

interface ChatRepository {
    suspend fun sendMessage(request: ChatRequest): Result<ChatResponse>
}
