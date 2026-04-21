package com.example.myai.data.config

/**
 * API configuration constants for Ollama service.
 */
object ApiConfig {
    const val LOCAL_HOST = "10.0.2.2"
    const val REMOTE_HOST = "hanfengai.asuscomm.com"
    const val PORT = 443  // Caddy uses HTTPS on port 443
    const val HOST = REMOTE_HOST
    const val BASE_URL = "https://$HOST:$PORT"
    const val CHAT_ENDPOINT = "$BASE_URL/api/chat"
    const val MODELS_ENDPOINT = "$BASE_URL/api/tags"
}
