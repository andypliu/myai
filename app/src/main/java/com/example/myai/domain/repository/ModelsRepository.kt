package com.example.myai.domain.repository

/**
 * Repository interface for model-related data operations.
 * Defines the contract for fetching available AI models.
 */
interface ModelsRepository {
    /**
     * Fetches the list of available AI models.
     * @return Result containing the list of models or an error
     */
    suspend fun getAvailableModels(): Result<List<com.example.myai.domain.model.OllamaModel>>
}
