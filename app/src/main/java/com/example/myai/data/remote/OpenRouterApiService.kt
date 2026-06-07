package com.example.myai.data.remote

import android.util.Log
import com.example.myai.BuildConfig
import com.example.myai.data.model.OpenRouterChatRequest
import com.example.myai.data.model.OpenRouterChatResponse
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class OpenRouterApiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json".toMediaType()
    private val apiKey = BuildConfig.OPENROUTER_API_KEY

    suspend fun chatCompletions(request: OpenRouterChatRequest): Result<OpenRouterChatResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "https://openrouter.ai/api/v1/chat/completions"
            val jsonBody = gson.toJson(request)
            val requestBody = jsonBody.toRequestBody(jsonMediaType)

            val httpRequest = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = client.newCall(httpRequest).execute()
            
            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                Log.e("OpenRouterApiService", "Error: ${response.code} $errorBody")
                return@withContext Result.failure(Exception("OpenRouter API error: ${response.code}"))
            }

            val responseBody = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
            val openRouterResponse = gson.fromJson(responseBody, OpenRouterChatResponse::class.java)
            Result.success(openRouterResponse)
        } catch (e: Exception) {
            Log.e("OpenRouterApiService", "Exception", e)
            Result.failure(e)
        }
    }
}
