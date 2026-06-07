package com.example.myai.data.repository

import com.example.myai.data.remote.OllamaModelsService
import com.example.myai.domain.model.AiServiceType
import com.example.myai.domain.model.OllamaModel
import com.example.myai.domain.repository.ModelsRepository

/**
 * Implementation of ModelsRepository that handles switching between
 * Ollama and Nvidia (Local) models.
 */
class ModelsRepositoryImpl(
    private val modelsService: OllamaModelsService,
    private val nvidiaModelsService: com.example.myai.data.remote.NvidiaModelsService
) : ModelsRepository {
    
    private var cachedNvidiaModels: List<OllamaModel>? = null

    override suspend fun getAvailableModels(serviceType: AiServiceType): Result<List<OllamaModel>> {
        return when (serviceType) {
            AiServiceType.OLLAMA -> modelsService.getModels()
            AiServiceType.NVIDIA -> {
                cachedNvidiaModels?.let { return Result.success(it) }
                
                nvidiaModelsService.getModels().onSuccess {
                    cachedNvidiaModels = it
                }
            }
            AiServiceType.GOOGLE -> Result.success(listOf(
                OllamaModel("gemini-3.5-flash", "google", 0),
                OllamaModel("gemini-3.1-flash-lite", "google", 0)
            ))
            AiServiceType.ON_DEVICE -> Result.success(listOf(OllamaModel("Gemma 2B", "just now", 0)))
            AiServiceType.AICORE -> Result.success(listOf(OllamaModel("Gemini Nano", "system", 0)))
        }
    }

    override suspend fun refreshModels(serviceType: AiServiceType): Result<List<OllamaModel>> {
        return when (serviceType) {
            AiServiceType.OLLAMA -> modelsService.getModels()
            AiServiceType.NVIDIA -> {
                nvidiaModelsService.getModels().onSuccess {
                    cachedNvidiaModels = it
                }
            }
            AiServiceType.GOOGLE -> Result.success(listOf(
                OllamaModel("gemini-3.5-flash", "google", 0),
                OllamaModel("gemini-3.1-flash-lite", "google", 0)
            ))
            AiServiceType.ON_DEVICE -> Result.success(listOf(OllamaModel("Gemma 2B", "just now", 0)))
            AiServiceType.AICORE -> Result.success(listOf(OllamaModel("Gemini Nano", "system", 0)))
        }
    }
}
