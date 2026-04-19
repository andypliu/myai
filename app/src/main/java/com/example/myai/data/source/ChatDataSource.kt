package com.example.myai.data.source

import com.example.myai.data.model.ChatRequest
import com.example.myai.data.model.ChatResponse

/**
 * Data source interface for sending chat messages.
 * This abstraction allows for different implementations (e.g., Ollama, OpenAI, etc.)
 */
interface ChatDataSource {
    suspend fun sendMessage(request: ChatRequest): Result<ChatResponse>
}
