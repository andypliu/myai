package com.example.myai.domain.model

data class ChatRequest(
    val model: String = "gemma4:e2b",
    val messages: List<ChatRequestMessage>,
    val stream: Boolean = false,
    val images: List<String>? = null,  // Base64 encoded images
    val documents: List<DocumentData>? = null  // Document data
)

data class ChatRequestMessage(
    val role: String,
    val content: String
)
