package com.example.myai.data.repository

import com.example.myai.data.source.ChatDataSource
import com.example.myai.data.model.ChatRequest
import com.example.myai.data.model.ChatResponse
import com.example.myai.data.remote.NvidiaApiService
import com.example.myai.data.remote.GoogleApiService
import com.example.myai.data.remote.OpenRouterApiService
import com.example.myai.data.source.NvidiaDataSource
import com.example.myai.data.source.GoogleDataSource
import com.example.myai.data.source.OpenRouterDataSource
import com.example.myai.domain.model.AiServiceType
import com.example.myai.domain.repository.ChatRepository

import kotlinx.coroutines.flow.Flow

class ChatRepositoryImpl(
    private val ollamaDataSource: ChatDataSource,
    nvidiaApiService: NvidiaApiService,
    googleApiService: GoogleApiService,
    openRouterApiService: OpenRouterApiService
) : ChatRepository {

    private val nvidiaDataSource = NvidiaDataSource(nvidiaApiService)
    private val googleDataSource = GoogleDataSource(googleApiService)
    private val openRouterDataSource = OpenRouterDataSource(openRouterApiService)

    override suspend fun sendMessage(request: ChatRequest, serviceType: AiServiceType): Result<ChatResponse> {
        return when (serviceType) {
            AiServiceType.OLLAMA -> ollamaDataSource.sendMessage(request)
            AiServiceType.NVIDIA -> nvidiaDataSource.sendMessage(request)
            AiServiceType.GOOGLE -> googleDataSource.sendMessage(request)
            AiServiceType.OPENROUTER -> openRouterDataSource.sendMessage(request)
            AiServiceType.ON_DEVICE -> Result.failure(Exception("Use sendMessageStream for ON_DEVICE"))
            AiServiceType.AICORE -> Result.failure(Exception("Use sendMessageStream for AICORE"))
        }
    }

    override fun sendMessageStream(request: ChatRequest, serviceType: AiServiceType): Flow<String> {
        return when (serviceType) {
            AiServiceType.OLLAMA -> throw UnsupportedOperationException("Ollama streaming not implemented")
            AiServiceType.NVIDIA -> nvidiaDataSource.sendMessageStream(request)
            AiServiceType.GOOGLE -> throw UnsupportedOperationException("Google streaming not implemented")
            AiServiceType.OPENROUTER -> throw UnsupportedOperationException("OpenRouter streaming not implemented")
            AiServiceType.ON_DEVICE -> throw UnsupportedOperationException("On-device handled in ViewModel for now")
            AiServiceType.AICORE -> throw UnsupportedOperationException("AI Core handled in ViewModel for now")
        }
    }
}
