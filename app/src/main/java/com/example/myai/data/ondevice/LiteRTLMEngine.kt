package com.example.myai.data.ondevice

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.ProgressListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * Real engine using LiteRT-LM (MediaPipe GenAI) LlmInference.
 * Implemented as a Singleton to manage the heavy model lifecycle and prevent memory leaks.
 * Optimized for streaming performance and clean output filtering.
 */
class LiteRTLMEngine private constructor(private val context: Context) {
    private var llmInference: LlmInference? = null
    private var currentModelPath: String? = null
    private val mutex = Mutex()

    companion object {
        private const val TAG = "LiteRTLMEngine"

        @Volatile
        private var INSTANCE: LiteRTLMEngine? = null

        fun getInstance(context: Context): LiteRTLMEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LiteRTLMEngine(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Initializes the LlmInference engine with the provided model file.
     */
    suspend fun initialize(modelFile: File) = mutex.withLock {
        if (!modelFile.exists()) {
            Log.e(TAG, "Model file does not exist: ${modelFile.absolutePath}")
            return@withLock
        }

        if (llmInference != null && currentModelPath == modelFile.absolutePath) {
            Log.d(TAG, "Engine already initialized with this model.")
            return@withLock
        }

        cleanupInternal()

        try {
            Log.d(TAG, "Initializing LlmInference with ${modelFile.name}")
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(2048)
                .setMaxTopK(40)
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            currentModelPath = modelFile.absolutePath
            Log.d(TAG, "Engine initialized successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize engine", e)
            throw e
        }
    }

    /**
     * Non-streaming response generation.
     */
    fun generateResponse(prompt: String): Flow<String> = flow {
        mutex.withLock {
            val inference = llmInference
            if (inference == null) {
                emit("Error: Engine not initialized")
                return@withLock
            }
            try {
                val rawResult = inference.generateResponse(prompt)
                emit(filterModelOutput(rawResult))
            } catch (e: Exception) {
                Log.e(TAG, "Inference error", e)
                emit("Error: ${e.message}")
            }
        }
    }.flowOn(Dispatchers.Default)

    /**
     * True streaming response generation using MediaPipe's ProgressListener.
     */
    fun generateStreamingResponse(prompt: String): Flow<String> = callbackFlow {
        // We use withLock inside the flow to ensure thread safety
        mutex.withLock {
            val inference = llmInference
            if (inference == null) {
                trySend("Error: Engine not initialized")
                channel.close()
                return@withLock
            }

            var fullResponse = ""
            var lastEmittedLength = 0

            val listener = ProgressListener<String> { partialResult, done ->
                fullResponse = partialResult
                val filtered = filterModelOutput(fullResponse)
                
                if (filtered.length > lastEmittedLength) {
                    val delta = filtered.substring(lastEmittedLength)
                    if (delta.isNotEmpty()) {
                        trySend(delta)
                    }
                    lastEmittedLength = filtered.length
                }
                
                if (done) {
                    channel.close()
                }
            }

            try {
                inference.generateResponseAsync(prompt, listener)
            } catch (e: Exception) {
                Log.e(TAG, "Streaming inference error", e)
                trySend("Error: ${e.message}")
                channel.close()
            }
        }
        awaitClose { }
    }.flowOn(Dispatchers.Default)

    private fun filterModelOutput(output: String): String {
        if (output.isBlank()) return output
        var filtered = output
        
        // Remove everything after channel markers
        val channelMarkers = listOf("<|channel>thought", "<|channel>judge", "<|channel>turn")
        for (marker in channelMarkers) {
            val index = filtered.indexOf(marker)
            if (index >= 0) filtered = filtered.substring(0, index)
        }

        filtered = filtered.replace("<turn|>", "").replace("<eos>", "")
        
        // Remove thinking/reasoning blocks (handles both complete and partial blocks)
        val patterns = listOf("<thinking>", "<reasoning>", "<thought>")
        val closings = listOf("</thinking>", "</reasoning>", "</thought>")
        for (i in patterns.indices) {
            filtered = filtered.replace(Regex("${patterns[i]}.*?(${closings[i]}|$)", RegexOption.DOT_MATCHES_ALL), "")
        }

        // Strip structural wrapper tags
        val wrappers = listOf("response", "output", "answer")
        for (tag in wrappers) {
            filtered = filtered.replace("<$tag>", "").replace("</$tag>", "")
        }

        return filtered.replace(Regex("""\n\s*\n"""), "\n").trim()
    }

    suspend fun close() = mutex.withLock {
        cleanupInternal()
    }

    private fun cleanupInternal() {
        try {
            llmInference?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing engine", e)
        }
        llmInference = null
        currentModelPath = null
    }
}
