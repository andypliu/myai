package com.example.myai.domain.model

data class ChatResponse(
    val message: ChatResponseMessage,
    val done: Boolean
)

data class ChatResponseMessage(
    val role: String,
    val content: String
)
