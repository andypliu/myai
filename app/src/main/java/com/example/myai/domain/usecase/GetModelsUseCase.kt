package com.example.myai.domain.usecase

import com.example.myai.domain.model.OllamaModel
import com.example.myai.domain.repository.ModelsRepository

/**
 * Use case for fetching available AI models.
 * Encapsulates the business logic for retrieving the list of models.
 */
class GetModelsUseCase(
    private val repository: ModelsRepository
) {
    suspend operator fun invoke(): Result<List<OllamaModel>> {
        return repository.getAvailableModels()
    }
}
