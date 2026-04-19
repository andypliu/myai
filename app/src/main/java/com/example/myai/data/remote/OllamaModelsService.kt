package com.example.myai.data.remote

import android.util.Log
import com.example.myai.data.config.ApiConfig
import com.example.myai.data.mapper.OllamaModelMapper
import com.example.myai.data.model.OllamaModelsDTO
import com.example.myai.domain.model.OllamaModel
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
                .url(ApiConfig.MODELS_ENDPOINT)
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

            val modelsDTO = gson.fromJson(responseBody, OllamaModelsDTO::class.java)
            val domainModels = OllamaModelMapper.toDomainModels(modelsDTO)
            Result.success(domainModels)
        } catch (e: Exception) {
            Log.e("OllamaModelsService", "Error fetching models", e)
            Result.failure(e)
        }
    }
}
