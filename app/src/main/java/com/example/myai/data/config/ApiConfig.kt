package com.example.myai.data.config

/**
 * API configuration constants for Ollama service.
 */
object ApiConfig {
    const val LOCAL_HOST = "10.0.2.2"
    const val REMOTE_HOST = "hanfengai.asuscomm.com"
    const val OLLAMA_PORT = 443  // Caddy uses HTTPS on port 443
    const val UVICORN_PORT = 8082
    const val HOST = REMOTE_HOST
    const val BASE_URL = "https://$HOST:$OLLAMA_PORT"
    const val CHAT_ENDPOINT = "$BASE_URL/api/chat"
    const val MODELS_ENDPOINT = "$BASE_URL/api/tags"
    const val ROOT_ENDPOINT = BASE_URL  // Used for basic connectivity/auth check

    // Nvidia / Local Uvicorn Service
    const val NVIDIA_BASE_URL = "http://$REMOTE_HOST:$UVICORN_PORT"
    const val NVIDIA_CHAT_ENDPOINT = "$NVIDIA_BASE_URL/v1/messages"
    const val NVIDIA_AUTH_TOKEN = "freecc"
}
