package com.example.myai.domain.model

/**
 * Domain model representing an AI model available for use.
 */
data class OllamaModel(
    val name: String,
    val modifiedAt: String,
    val size: Long
)
