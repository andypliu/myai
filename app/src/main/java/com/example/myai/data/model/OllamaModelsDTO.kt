package com.example.myai.data.model

/**
 * Data Transfer Object for Ollama models API response.
 * This represents the raw JSON response from the Ollama API.
 */
data class OllamaModelsDTO(
    val models: List<OllamaModelDTO>
)

data class OllamaModelDTO(
    val name: String,
    val modified_at: String,
    val size: Long
)
