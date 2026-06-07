package com.example.myai.data.model

data class OpenRouterChatResponse(
    val choices: List<OpenRouterChoice>
)

data class OpenRouterChoice(
    val message: OpenRouterResponseMessage
)

data class OpenRouterResponseMessage(
    val content: String
)
