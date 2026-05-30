package com.example.myai.data.ondevice

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.InferenceMode
import com.google.firebase.ai.OnDeviceConfig
import com.google.firebase.ai.ai
import com.google.firebase.ai.ondevice.FirebaseAIOnDevice
import com.google.firebase.ai.ondevice.OnDeviceModelStatus
import com.google.firebase.ai.ondevice.DownloadStatus
import com.google.firebase.ai.type.PublicPreviewAPI
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Manager for Android AI Core via Firebase AI Logic (On-device SDK).
 * This leverages system-managed models like Gemini Nano.
 */
class AiCoreManager private constructor(private val context: Context) {
    private var generativeModel: GenerativeModel? = null

    companion object {
        private const val TAG = "AiCoreManager"

        @Volatile
        private var INSTANCE: AiCoreManager? = null

        fun getInstance(context: Context): AiCoreManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AiCoreManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        /**
         * Launches the Play Store page for Android AI Core.
         */
        fun openAiCoreOnPlayStore(context: Context) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=com.google.android.aicore")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                val webIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.aicore")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            }
        }
    }

    private val _downloadProgress = MutableStateFlow<DownloadStatus?>(null)
    val downloadProgress: StateFlow<DownloadStatus?> = _downloadProgress.asStateFlow()

    /**
     * Initializes the GenerativeModel client with ONLY_ON_DEVICE inference mode.
     */
    @OptIn(PublicPreviewAPI::class)
    private fun ensureInitialized() {
        if (generativeModel != null) return

        synchronized(this) {
            if (generativeModel != null) return

            // Fallback initialization if FirebaseApp is not ready
            try {
                FirebaseApp.getInstance()
                Log.d(TAG, "FirebaseApp already initialized")
            } catch (e: IllegalStateException) {
                Log.w(TAG, "FirebaseApp not initialized, performing manual initialization")
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:1234567890:android:abc1234567890")
                    .setProjectId("myai-local")
                    .setApiKey("unused_but_required_format_key")
                    .setGcmSenderId("1234567890")
                    .build()
                FirebaseApp.initializeApp(context, options)
            }

            // For Gemini Nano via AI Core, use "gemini-nano" model name.
            // gemini-1.5-flash is a cloud model name and may cause UNAVAILABLE status in ONLY_ON_DEVICE mode.
            try {
                generativeModel = Firebase.ai.generativeModel(
                    modelName = "gemini-nano",
                    onDeviceConfig = OnDeviceConfig(mode = InferenceMode.ONLY_ON_DEVICE)
                )
                Log.d(TAG, "GenerativeModel initialized with gemini-nano")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize GenerativeModel", e)
            }
        }
    }

    /**
     * Checks the status of the on-device model.
     */
    @OptIn(PublicPreviewAPI::class)
    suspend fun checkStatus(): AiCoreStatus {
        try {
            ensureInitialized()
            val model = generativeModel ?: return AiCoreStatus.Unavailable("Model not initialized")
            val onDeviceExtension = model.onDeviceExtension ?: return AiCoreStatus.Unavailable("On-device extension missing")
            
            val status = onDeviceExtension.checkStatus()
            Log.d(TAG, "On-device model status result: $status")
            
            return when (status.toString()) {
                "AVAILABLE" -> AiCoreStatus.Available
                "DOWNLOADABLE" -> AiCoreStatus.Downloadable
                "DOWNLOADING" -> AiCoreStatus.ModelDownloading
                "UNAVAILABLE" -> AiCoreStatus.Unavailable("AI Core reports UNAVAILABLE. Check Developer Options.")
                else -> AiCoreStatus.Unavailable("Status: $status")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during status check", e)
            return AiCoreStatus.Unavailable("Error: ${e.message}")
        }
    }

    /**
     * Triggers the download/initialization of the on-device model.
     */
    @OptIn(PublicPreviewAPI::class)
    suspend fun downloadModel() {
        try {
            ensureInitialized()
            FirebaseAIOnDevice.download().collect { status ->
                _downloadProgress.value = status
                Log.d(TAG, "Download status: $status")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting model preparation", e)
        }
    }

    /**
     * Warms up the model to reduce latency for the first inference.
     */
    @OptIn(PublicPreviewAPI::class)
    suspend fun warmup() {
        try {
            ensureInitialized()
            generativeModel?.onDeviceExtension?.warmUp()
            Log.d(TAG, "Model warmup complete")
        } catch (e: Exception) {
            Log.e(TAG, "Error during model warmup", e)
        }
    }

    /**
     * Generates a streaming response using on-device inference.
     */
    @OptIn(PublicPreviewAPI::class)
    fun generateStreamingResponse(prompt: String): Flow<String> = callbackFlow {
        ensureInitialized()
        val model = generativeModel ?: run {
            trySend("Error: AI Model not initialized")
            close()
            return@callbackFlow
        }

        try {
            model.generateContentStream(prompt).collect { chunk ->
                trySend(chunk.text ?: "")
            }
        } catch (e: Exception) {
            Log.e(TAG, "On-device inference error", e)
            trySend("Error: ${e.message}")
        } finally {
            close()
        }
        awaitClose { }
    }
}

sealed class AiCoreStatus {
    object Available : AiCoreStatus()
    object Downloadable : AiCoreStatus()
    object AiCoreMissing : AiCoreStatus()
    object ModelDownloading : AiCoreStatus()
    object WaitingForWifi : AiCoreStatus()
    data class Unavailable(val message: String) : AiCoreStatus()
}
