package com.example.myai.data.remote

import android.util.Log
import com.example.myai.data.config.ApiConfig
import com.example.myai.data.model.ChatRequest
import com.example.myai.data.model.ChatResponse
import com.example.myai.data.model.ChatResponseMessage
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * API service for Nvidia (Local Uvicorn) HTTP communication.
 * Communicates with the local service using Anthropic-like message format.
 */
class NvidiaApiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json".toMediaType()

    /**
     * Send a chat request to the Nvidia (Local) API.
     * Maps Ollama-style ChatRequest to the format expected by the local service.
     * Handles both standard JSON responses and SSE streams.
     */
    fun chatStream(request: ChatRequest): Flow<String> = flow {
        try {
            val nvidiaRequest = mapToNvidiaRequest(request)
            val jsonBody = gson.toJson(nvidiaRequest)
            Log.d("NvidiaApiService", "Full JSON Request Body: $jsonBody")
            val requestBody = jsonBody.toRequestBody(jsonMediaType)

            val httpRequest = Request.Builder()
                .url(ApiConfig.NVIDIA_CHAT_ENDPOINT)
                .addHeader("x-api-key", ApiConfig.NVIDIA_AUTH_TOKEN)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "text/event-stream")
                .post(requestBody)
                .build()

            Log.d("NvidiaApiService", "Sending request to: ${ApiConfig.NVIDIA_CHAT_ENDPOINT}")
            Log.d("NvidiaApiService", "Model: ${request.model}")

            val response = client.newCall(httpRequest).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                Log.e("NvidiaApiService", "Nvidia API error: ${response.code}, body: $errorBody")
                throw Exception("Nvidia API error: ${response.code}")
            }

            val source = response.body?.source() ?: throw Exception("Empty body")

            try {
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    val trimmedLine = line.trim()

                    if (trimmedLine.startsWith("data:")) {
                        val dataJson = trimmedLine.substring(5).trim()
                        if (dataJson == "[DONE]") {
                            Log.d("NvidiaApiService", "Stream [DONE] received")
                            break
                        }

                        val text = extractTextFromData(dataJson)
                        if (text != null) {
                            emit(text)
                        }
                    } else if (trimmedLine.startsWith("{")) {
                        // Fallback: if it's not SSE but raw JSON
                        val fullJson = trimmedLine + source.readUtf8()
                        Log.d("NvidiaApiService", "Received raw JSON instead of SSE")
                        val text = extractTextFromFullJson(fullJson)
                        if (text != null) {
                            emit(text)
                        }
                        break
                    }
                }
            } catch (e: Exception) {
                Log.w("NvidiaApiService", "Stream interrupted or error during parsing", e)
            } finally {
                response.close()
            }
        } catch (e: Exception) {
            Log.e("NvidiaApiService", "Error sending request to Nvidia", e)
            throw e
        }
    }.flowOn(Dispatchers.IO)

    suspend fun chat(request: ChatRequest): Result<ChatResponse> = withContext(Dispatchers.IO) {
        try {
            val nvidiaRequest = mapToNvidiaRequest(request)
            val jsonBody = gson.toJson(nvidiaRequest)
            Log.d("NvidiaApiService", "Full JSON Request Body: $jsonBody")
            val requestBody = jsonBody.toRequestBody(jsonMediaType)

            val httpRequest = Request.Builder()
                .url(ApiConfig.NVIDIA_CHAT_ENDPOINT)
                .addHeader("x-api-key", ApiConfig.NVIDIA_AUTH_TOKEN)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "text/event-stream")
                .post(requestBody)
                .build()

            Log.d("NvidiaApiService", "Sending request to: ${ApiConfig.NVIDIA_CHAT_ENDPOINT}")
            Log.d("NvidiaApiService", "Model: ${request.model}")
            
            val response = client.newCall(httpRequest).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                Log.e("NvidiaApiService", "Nvidia API error: ${response.code}, body: $errorBody")
                return@withContext Result.failure(Exception("Nvidia API error: ${response.code}"))
            }

            val sb = StringBuilder()
            val source = response.body?.source() ?: return@withContext Result.failure(Exception("Empty body"))

            try {
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: break
                    val trimmedLine = line.trim()
                    
                    if (trimmedLine.startsWith("data:")) {
                        val dataJson = trimmedLine.substring(5).trim()
                        if (dataJson == "[DONE]") {
                            Log.d("NvidiaApiService", "Stream [DONE] received")
                            break
                        }

                        val text = extractTextFromData(dataJson)
                        if (text != null) {
                            sb.append(text)
                        }
                    } else if (trimmedLine.startsWith("{")) {
                        // Fallback: if it's not SSE but raw JSON
                        val fullJson = trimmedLine + source.readUtf8()
                        Log.d("NvidiaApiService", "Received raw JSON instead of SSE")
                        val text = extractTextFromFullJson(fullJson)
                        if (text != null) {
                            sb.append(text)
                        }
                        break
                    }
                }
            } catch (e: Exception) {
                Log.w("NvidiaApiService", "Stream interrupted or error during parsing", e)
            } finally {
                response.close()
            }

            val fullContent = sb.toString().trim()
            if (fullContent.isNotEmpty()) {
                Log.d("NvidiaApiService", "Request successful, content length: ${fullContent.length}")
                return@withContext Result.success(
                    ChatResponse(
                        message = ChatResponseMessage(role = "assistant", content = fullContent),
                        done = true
                    )
                )
            }

            Result.failure(Exception("No content extracted from Nvidia response"))
        } catch (e: Exception) {
            Log.e("NvidiaApiService", "Error sending request to Nvidia", e)
            Result.failure(e)
        }
    }

    private fun extractTextFromData(dataJson: String): String? {
        return try {
            val jsonElement = JsonParser.parseString(dataJson)
            if (!jsonElement.isJsonObject) return null
            val obj = jsonElement.asJsonObject

            // 1. Anthropic content_block_delta
            if (obj.has("type") && obj.get("type").asString == "content_block_delta") {
                val delta = obj.getAsJsonObject("delta")
                if (delta != null) {
                    if (delta.has("text")) return delta.get("text").asString
                    // Sometimes it's a different kind of delta in different versions
                    if (delta.has("content")) return delta.get("content").asString
                }
            }

            // 1b. Anthropic text_delta (seen in some implementations)
            if (obj.has("type") && obj.get("type").asString == "text_delta") {
                if (obj.has("text")) return obj.get("text").asString
            }

            // 2. OpenAI delta
            if (obj.has("choices") && obj.get("choices").isJsonArray) {
                val choices = obj.getAsJsonArray("choices")
                if (choices.size() > 0) {
                    val choice = choices.get(0).asJsonObject
                    if (choice.has("delta")) {
                        val delta = choice.getAsJsonObject("delta")
                        if (delta.has("content")) return delta.get("content").asString
                    }
                }
            }

            // 3. Anthropic message_start (sometimes has content)
            if (obj.has("type") && obj.get("type").asString == "message_start") {
                val msg = obj.getAsJsonObject("message")
                if (msg != null && msg.has("content")) {
                    val content = msg.get("content")
                    if (content.isJsonArray) {
                        return content.asJsonArray.mapNotNull {
                            if (it.isJsonObject && it.asJsonObject.has("text")) 
                                it.asJsonObject.get("text").asString else null
                        }.joinToString("")
                    }
                }
            }

            // 4. OpenAI style without choices (direct chunk)
            if (obj.has("content")) {
                return obj.get("content").asString
            }

            null
        } catch (e: Exception) {
            null
        }
    }

    private fun extractTextFromFullJson(json: String): String? {
        return try {
            val jsonObject = JsonParser.parseString(json).asJsonObject
            
            // Anthropic format
            if (jsonObject.has("content") && jsonObject.get("content").isJsonArray) {
                return jsonObject.getAsJsonArray("content").mapNotNull {
                    if (it.isJsonObject) it.asJsonObject.get("text")?.asString else null
                }.joinToString("\n")
            }
            
            // OpenAI format
            if (jsonObject.has("choices") && jsonObject.get("choices").isJsonArray) {
                val choices = jsonObject.getAsJsonArray("choices")
                if (choices.size() > 0) {
                    val message = choices.get(0).asJsonObject.getAsJsonObject("message")
                    return message?.get("content")?.asString
                }
            }
            
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun mapToNvidiaRequest(request: ChatRequest): Map<String, Any> {
        val messages = request.messages.map {
            mapOf("role" to it.role, "content" to it.content)
        }
        
        return mutableMapOf(
            "model" to request.model,
            "messages" to messages,
            "max_tokens" to 4096,
            "stream" to true
        )
    }
}
