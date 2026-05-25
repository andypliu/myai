package com.example.myai.data.config

import android.content.Context

/**
 * API configuration constants and dynamic provider for Ollama and Nvidia services.
 */
object ApiConfig {
    private const val LOCAL_HOST = "10.0.2.2"
    private const val REMOTE_HOST = "hanfengai.asuscomm.com"
    
    private const val OLLAMA_REMOTE_PORT = 8668
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
        return prefs.getBoolean(PREF_USE_SECURITY, true)
    }

    private fun getProtocol(context: Context): String = if (isSecurityEnabled(context)) "https" else "http"

    fun getOllamaBaseUrl(context: Context): String {
        val security = isSecurityEnabled(context)
        val local = isLocal(context)
        val host = getHost(context)
        val protocol = if (security) "https" else "http"
        val port = if (local) OLLAMA_LOCAL_PORT else OLLAMA_REMOTE_PORT
        
        return "$protocol://$host:$port"
    }

    fun getHost(context: Context): String = if (isLocal(context)) LOCAL_HOST else REMOTE_HOST

    fun getUvicornBaseUrl(context: Context): String {
        val security = isSecurityEnabled(context)
        val local = isLocal(context)
        val host = getHost(context)
        val protocol = if (security) "https" else "http"
        val port = if (local) UVICORN_LOCAL_PORT else UVICORN_REMOTE_PORT
        
        return if (!local) {
            "$protocol://$host:$port/uvicorn"
        } else {
            "$protocol://$host:$port"
        }
    }

    fun getChatEndpoint(context: Context) = "${getOllamaBaseUrl(context)}/api/chat"
    fun getModelsEndpoint(context: Context) = "${getOllamaBaseUrl(context)}/api/tags"
    fun getNvidiaChatEndpoint(context: Context) = "${getUvicornBaseUrl(context)}/v1/messages"

    const val NVIDIA_CATALOG_URL = "https://build.nvidia.com/models"
}
