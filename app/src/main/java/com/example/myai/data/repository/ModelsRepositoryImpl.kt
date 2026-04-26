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
    private val modelsService: OllamaModelsService
) : ModelsRepository {
    
    override suspend fun getAvailableModels(serviceType: AiServiceType): Result<List<OllamaModel>> {
        return when (serviceType) {
            AiServiceType.OLLAMA -> modelsService.getModels()
            AiServiceType.NVIDIA -> {
                // For Nvidia, we use the hardcoded list provided by the user
                val nvidiaModels = listOf(
                    "deepseek-ai/deepseek-v4-pro",
                    "deepseek-ai/deepseek-v4-flash",
                    "moonshotai/kimi-k2.5",
                    "z-ai/glm-5.1",
                    "minimaxai/minimax-m2.5"
                ).map { name -> 
                    OllamaModel(
                        name = name,
                        modifiedAt = "",
                        size = 0L
                    )
                }
                Result.success(nvidiaModels)
            }
        }
    }
}
