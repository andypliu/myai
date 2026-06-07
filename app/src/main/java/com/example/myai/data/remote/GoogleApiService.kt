package com.example.myai.data.remote

import android.util.Log
import com.example.myai.BuildConfig
import com.example.myai.data.model.GoogleChatRequest
import com.example.myai.data.model.GoogleChatResponse
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class GoogleApiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json".toMediaType()
    private val apiKey = BuildConfig.GOOGLE_API_KEY

    suspend fun generateContent(model: String, request: GoogleChatRequest): Result<GoogleChatResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"
            val jsonBody = gson.toJson(request)
            val requestBody = jsonBody.toRequestBody(jsonMediaType)

            val httpRequest = Request.Builder()
                .url(url)
                .addHeader("x-goog-api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            Log.d("GoogleApiService", "Sending request to $url")
            val response = client.newCall(httpRequest).execute()
            
            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                Log.e("GoogleApiService", "Error: ${response.code} $errorBody")
                return@withContext Result.failure(Exception("Google API error: ${response.code}"))
            }

            val responseBody = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
            val googleResponse = gson.fromJson(responseBody, GoogleChatResponse::class.java)
            Result.success(googleResponse)
        } catch (e: Exception) {
            Log.e("GoogleApiService", "Exception", e)
            Result.failure(e)
        }
    }
}
