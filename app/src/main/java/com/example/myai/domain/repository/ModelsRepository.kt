package com.example.myai.domain.repository

import com.example.myai.domain.model.AiServiceType
import com.example.myai.domain.model.OllamaModel

/**
 * Repository interface for model-related data operations.
 * Defines the contract for fetching available AI models.
 */
interface ModelsRepository {
    /**
     * Fetches the list of available AI models for a specific service.
     * @return Result containing the list of models or an error
     */
    suspend fun getAvailableModels(serviceType: AiServiceType): Result<List<OllamaModel>>
}
