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
                OllamaModel("gemini-3.1-flash-lite", "google", 0),
                OllamaModel("gemini-2.5-flash", "google", 0)
            ))
            AiServiceType.ON_DEVICE -> Result.success(listOf(OllamaModel("Gemma 2B", "just now", 0)))
            AiServiceType.AICORE -> Result.success(listOf(OllamaModel("Gemini Nano", "system", 0)))
            AiServiceType.OPENROUTER -> Result.success(listOf(
                OllamaModel("openrouter/free", "openrouter", 0),
                OllamaModel("openai/gpt-oss-120b:free", "openrouter", 0),
                OllamaModel("google/gemma-4-31b-it:free", "openrouter", 0),
                OllamaModel("z-ai/glm-4.5-air:free", "openrouter", 0),
                OllamaModel("nvidia/nemotron-3-super-120b-a12b:free", "openrouter", 0),
                OllamaModel("poolside/laguna-m.1:free", "openrouter", 0),
                OllamaModel("nvidia/nemotron-3-ultra-550b-a55b:free", "openrouter", 0),
                OllamaModel("nvidia/nemotron-3-nano-omni-30b-a3b-reasoning:free", "openrouter", 0),
                OllamaModel("nvidia/nemotron-nano-12b-v2-vl:free", "openrouter", 0),
                OllamaModel("liquid/lfm-2.5-1.2b-thinking:free", "openrouter", 0),
                OllamaModel("liquid/lfm-2.5-1.2b-instruct:free", "openrouter", 0),
                OllamaModel("nvidia/nemotron-3.5-content-safety:free", "openrouter", 0),
                OllamaModel("meta-llama/llama-3.3-70b-instruct:free", "openrouter", 0),
                OllamaModel("nousresearch/hermes-3-llama-3.1-405b:free", "openrouter", 0)
            ))
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
                OllamaModel("gemini-3.1-flash-lite", "google", 0),
                OllamaModel("gemini-2.5-flash", "google", 0)
            ))
            AiServiceType.ON_DEVICE -> Result.success(listOf(OllamaModel("Gemma 2B", "just now", 0)))
            AiServiceType.AICORE -> Result.success(listOf(OllamaModel("Gemini Nano", "system", 0)))
            AiServiceType.OPENROUTER -> getAvailableModels(AiServiceType.OPENROUTER)
        }
    }
}
