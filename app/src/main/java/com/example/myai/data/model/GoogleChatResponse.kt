package com.example.myai.data.model

data class GoogleChatResponse(
    val candidates: List<GoogleCandidate>? = null,
    val usageMetadata: GoogleUsageMetadata? = null,
    val modelVersion: String? = null,
    val responseId: String? = null
)

data class GoogleCandidate(
    val content: GoogleContent,
    val finishReason: String? = null,
    val index: Int? = null
)

data class GoogleUsageMetadata(
    val promptTokenCount: Int? = null,
    val candidatesTokenCount: Int? = null,
    val totalTokenCount: Int? = null
)
