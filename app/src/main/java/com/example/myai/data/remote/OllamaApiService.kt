package com.example.myai.data.remote

import android.util.Log
import com.example.myai.data.config.ApiConfig
import com.example.myai.data.model.ChatRequest
import com.example.myai.data.model.ChatResponse
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * API service for Ollama HTTP communication.
 * Handles all HTTP requests to the Ollama API.
 */
class OllamaApiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json".toMediaType()

    /**
     * Send a chat request to the Ollama API.
     */
    suspend fun chat(request: ChatRequest): Result<ChatResponse> = withContext(Dispatchers.IO) {
        try {
            val jsonBody = gson.toJson(request)
            Log.d("OllamaApiService", "Request JSON length: ${jsonBody.length}")
            Log.d("OllamaApiService", "Request JSON (first 500 chars): ${jsonBody.take(500)}...")
            val requestBody = jsonBody.toRequestBody(jsonMediaType)

            val httpRequest = Request.Builder()
                .url(ApiConfig.CHAT_ENDPOINT)
                .post(requestBody)
                .build()

            Log.d("OllamaApiService", "Sending request to Ollama API...")
            val response = client.newCall(httpRequest).execute()
            Log.d("OllamaApiService", "Response code: ${response.code}")

            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                Log.e("OllamaApiService", "Ollama API error: ${response.code} ${response.message}, body: $errorBody")
                return@withContext Result.failure(
                    Exception("Ollama API error: ${response.code} ${response.message}")
                )
            }

            val responseBody = response.body?.string()
                ?: return@withContext Result.failure(Exception("Empty response body"))

            Log.d("OllamaApiService", "Response body length: ${responseBody.length}")
            Log.d("OllamaApiService", "Response body (first 200 chars): ${responseBody.take(200)}...")

            val chatResponse = gson.fromJson(responseBody, ChatResponse::class.java)
            Result.success(chatResponse)
        } catch (e: Exception) {
            Log.e("OllamaApiService", "Error sending request", e)
            Result.failure(e)
        }
    }
}
