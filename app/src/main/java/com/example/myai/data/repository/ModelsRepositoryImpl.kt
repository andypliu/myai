package com.example.myai.data.repository

import com.example.myai.data.remote.OllamaModelsService
import com.example.myai.domain.model.OllamaModel
import com.example.myai.domain.repository.ModelsRepository

/**
 * Implementation of ModelsRepository that uses OllamaModelsService
 * to fetch available AI models.
 */
class ModelsRepositoryImpl(
    private val modelsService: OllamaModelsService
) : ModelsRepository {
    override suspend fun getAvailableModels(): Result<List<OllamaModel>> {
        return modelsService.getModels()
    }
}
