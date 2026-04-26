package com.example.myai.data.repository

import com.example.myai.data.source.ChatDataSource
import com.example.myai.data.model.ChatRequest
import com.example.myai.data.model.ChatResponse
import com.example.myai.data.remote.NvidiaApiService
import com.example.myai.data.source.NvidiaDataSource
import com.example.myai.domain.model.AiServiceType
import com.example.myai.domain.repository.ChatRepository

import kotlinx.coroutines.flow.Flow

class ChatRepositoryImpl(
    private val ollamaDataSource: ChatDataSource,
    private val nvidiaApiService: NvidiaApiService
) : ChatRepository {

    private val nvidiaDataSource = NvidiaDataSource(nvidiaApiService)

    override suspend fun sendMessage(request: ChatRequest, serviceType: AiServiceType): Result<ChatResponse> {
        return when (serviceType) {
            AiServiceType.OLLAMA -> ollamaDataSource.sendMessage(request)
            AiServiceType.NVIDIA -> nvidiaDataSource.sendMessage(request)
        }
    }

    override fun sendMessageStream(request: ChatRequest, serviceType: AiServiceType): Flow<String> {
        return when (serviceType) {
            AiServiceType.OLLAMA -> throw UnsupportedOperationException("Ollama streaming not implemented")
            AiServiceType.NVIDIA -> nvidiaDataSource.sendMessageStream(request)
        }
    }
}
