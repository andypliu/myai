package com.example.myai.data.remote

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import com.example.myai.data.config.ApiConfig
import com.example.myai.data.mapper.OllamaModelMapper
import com.example.myai.data.model.OllamaModelsDTO
import com.example.myai.domain.model.OllamaModel
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import java.util.concurrent.TimeUnit

class OllamaModelsService(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "MyAIPrefs"
        private const val PREF_USERNAME = "saved_username"
        private const val PREF_PASSWORD = "saved_password"
    }

    private fun getCredentials(): Pair<String, String>? {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val username = prefs.getString(PREF_USERNAME, null)
        val password = prefs.getString(PREF_PASSWORD, null)
        return if (username != null && password != null) Pair(username, password) else null
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .protocols(listOf(Protocol.HTTP_1_1))
        .addInterceptor { chain ->
            val original = chain.request()
            val creds = getCredentials()
            Log.d("OllamaModelsService", "Interceptor - creds found: ${creds != null}, username: ${creds?.first}")
            val request = if (creds != null) {
                val credentials = "${creds.first}:${creds.second}"
                val auth = "Basic ${Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)}"
                Log.d("OllamaModelsService", "Interceptor - adding Authorization header")
                original.newBuilder()
                    .header("Authorization", auth)
                    .build()
            } else {
                Log.w("OllamaModelsService", "Interceptor - no credentials found, sending request without auth")
                original
            }
            chain.proceed(request)
        }
        .build()

    private val gson = Gson()

    suspend fun getModels(): Result<List<OllamaModel>> = withContext(Dispatchers.IO) {
        try {
            val url = ApiConfig.MODELS_ENDPOINT
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            Log.d("OllamaModelsService", "Fetching models from: $url")
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
