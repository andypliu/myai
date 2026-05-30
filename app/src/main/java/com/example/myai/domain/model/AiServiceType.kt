package com.example.myai.domain.model

enum class AiServiceType(val label: String) {
    OLLAMA("Ollama"),
    NVIDIA("Nvidia"),
    ON_DEVICE("On-Device"),
    AICORE("AI Core")
}
