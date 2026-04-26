package com.example.myai.data.source

import com.example.myai.data.model.ChatRequest
import com.example.myai.data.model.ChatResponse
import com.example.myai.data.remote.NvidiaApiService

import kotlinx.coroutines.flow.Flow

/**
 * Nvidia-specific implementation of ChatDataSource.
 * Delegates HTTP communication to NvidiaApiService.
 */
class NvidiaDataSource(
    private val apiService: NvidiaApiService
) : ChatDataSource {

    override suspend fun sendMessage(request: ChatRequest): Result<ChatResponse> {
        return apiService.chat(request)
    }

    fun sendMessageStream(request: ChatRequest): Flow<String> {
        return apiService.chatStream(request)
    }
}
