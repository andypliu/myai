package com.example.myai.data.source

import com.example.myai.data.model.ChatRequest
import com.example.myai.data.model.ChatResponse
import com.example.myai.data.remote.OllamaApiService

/**
 * Ollama-specific implementation of ChatDataSource.
 * Delegates HTTP communication to OllamaApiService.
 */
class OllamaDataSource(
    private val apiService: OllamaApiService
) : ChatDataSource {

    override suspend fun sendMessage(request: ChatRequest): Result<ChatResponse> {
        return apiService.chat(request)
    }
}
