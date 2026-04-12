package com.example.myai.data.remote

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class OllamaModelsService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun getModels(): Result<List<OllamaModel>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("http://10.0.2.2:11434/api/tags")
                .get()
                .build()

            Log.d("OllamaModelsService", "Fetching models from Ollama API...")
            val response = client.newCall(request).execute()
            Log.d("OllamaModelsService", "Response code: ${response.code}")

            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                Log.e("OllamaModelsService", "Ollama API error: ${response.code} ${response.message}, body: $errorBody")
                return@withContext Result.failure(
                    Exception("Ollama API error: ${response.code} ${response.message}")
                )
            }

            val responseBody = response.body?.string()
                ?: return@withContext Result.failure(Exception("Empty response body"))

            Log.d("OllamaModelsService", "Response body: $responseBody")

            val modelsResponse = gson.fromJson(responseBody, ModelsResponse::class.java)
            Result.success(modelsResponse.models)
        } catch (e: Exception) {
            Log.e("OllamaModelsService", "Error fetching models", e)
            Result.failure(e)
        }
    }
}

data class ModelsResponse(
    val models: List<OllamaModel>
)

data class OllamaModel(
    val name: String,
    val modified_at: String,
    val size: Long
)
