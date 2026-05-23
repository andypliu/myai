package com.example.myai.data.config

import android.content.Context

/**
 * API configuration constants and dynamic provider for Ollama and Nvidia services.
 */
object ApiConfig {
    private const val LOCAL_HOST = "10.0.2.2"
    private const val REMOTE_HOST = "hanfengai.asuscomm.com"
    
    private const val OLLAMA_REMOTE_PORT = 443
    private const val OLLAMA_LOCAL_PORT = 11434
    
    private const val UVICORN_REMOTE_PORT = 8668
    private const val UVICORN_LOCAL_PORT = 8082

    private const val PREFS_NAME = "MyAIPrefs"
    const val PREF_USE_LOCAL = "use_local_host"
    const val PREF_USE_SECURITY = "use_security"

    fun isLocal(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(PREF_USE_LOCAL, false)
    }

    fun isSecurityEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(PREF_USE_SECURITY, false)
    }

    private fun getProtocol(context: Context): String = if (isSecurityEnabled(context)) "https" else "http"

    fun getOllamaBaseUrl(context: Context): String {
        val security = isSecurityEnabled(context)
        val local = isLocal(context)
        val host = getHost(context)
        val port = when {
            local -> OLLAMA_LOCAL_PORT
            security -> OLLAMA_REMOTE_PORT
            else -> 8082 // Not local and not secure
        }
        return "${getProtocol(context)}://$host:$port"
    }

    fun getHost(context: Context): String = if (isLocal(context)) LOCAL_HOST else REMOTE_HOST

    fun getUvicornBaseUrl(context: Context): String {
        val security = isSecurityEnabled(context)
        val local = isLocal(context)
        val host = getHost(context)
        val port = when {
            local -> UVICORN_LOCAL_PORT
            security -> UVICORN_REMOTE_PORT
            else -> UVICORN_LOCAL_PORT // Not local and not secure
        }
        return "${getProtocol(context)}://$host:$port"
    }

    fun getChatEndpoint(context: Context) = "${getOllamaBaseUrl(context)}/api/chat"
    fun getModelsEndpoint(context: Context) = "${getOllamaBaseUrl(context)}/api/tags"
    fun getNvidiaChatEndpoint(context: Context) = "${getUvicornBaseUrl(context)}/v1/messages"

    const val NVIDIA_CATALOG_URL = "https://build.nvidia.com/models"
}
