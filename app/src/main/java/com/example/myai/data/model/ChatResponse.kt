package com.example.myai.data.model

data class ChatResponse(
    val message: ChatResponseMessage,
    val done: Boolean
)

data class ChatResponseMessage(
    val role: String,
    val content: String
)
