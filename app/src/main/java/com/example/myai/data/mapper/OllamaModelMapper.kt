package com.example.myai.data.mapper

import com.example.myai.data.model.OllamaModelDTO
import com.example.myai.data.model.OllamaModelsDTO
import com.example.myai.domain.model.OllamaModel

/**
 * Mapper for converting Ollama DTOs to domain models.
 */
object OllamaModelMapper {

    /**
     * Convert OllamaModelsDTO to list of OllamaModel domain models.
     */
    fun toDomainModels(dto: OllamaModelsDTO): List<OllamaModel> {
        return dto.models.map { toDomainModel(it) }
    }

    /**
     * Convert OllamaModelDTO to OllamaModel domain model.
     */
    fun toDomainModel(dto: OllamaModelDTO): OllamaModel {
        return OllamaModel(
            name = dto.name,
            modifiedAt = dto.modified_at,
            size = dto.size
        )
    }
}
