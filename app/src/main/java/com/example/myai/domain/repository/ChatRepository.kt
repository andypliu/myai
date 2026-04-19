package com.example.myai.domain.repository

import com.example.myai.data.model.ChatRequest
import com.example.myai.data.model.ChatResponse
import com.example.myai.domain.model.ChatMessage

interface ChatRepository {
    suspend fun sendMessage(request: ChatRequest): Result<ChatResponse>
}
