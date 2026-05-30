package com.example.myai.data.ondevice

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.StatFs
import android.util.Log
import androidx.work.*
import com.example.myai.domain.ondevice.ModelDownloadState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

class ModelDownloadManager private constructor(private val context: Context) {
    private val _downloadState = MutableStateFlow<ModelDownloadState>(ModelDownloadState.Idle)
    val downloadState: StateFlow<ModelDownloadState> = _downloadState.asStateFlow()

    private val requiredSpace = 4_500_000_000L // 4.5GB
    private val workManager = WorkManager.getInstance(context)
    private val managerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        const val MODEL_FILE_NAME = "gemma-4-E2B-it.litertlm"

        @Volatile
        private var INSTANCE: ModelDownloadManager? = null

        fun getInstance(context: Context): ModelDownloadManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ModelDownloadManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun getModelFile(context: Context): File {
            val directory = File(context.noBackupFilesDir, "models")
            if (!directory.exists()) directory.mkdirs()
            return File(directory, MODEL_FILE_NAME)
        }
    }

    init {
        observeWorkProgress()
    }

    fun getModelFile(): File = getModelFile(context)

    fun isModelDownloaded(): Boolean = getModelFile().exists()

    fun startDownload() {
        if (isModelDownloaded()) {
            _downloadState.value = ModelDownloadState.Ready
            return
        }

        _downloadState.value = ModelDownloadState.CheckingStorage
        
        if (!hasEnoughSpace()) {
            _downloadState.value = ModelDownloadState.Error("Insufficient storage. 4.5GB required.")
            return
        }

        if (!isWifiConnected()) {
            _downloadState.value = ModelDownloadState.Error("Wi-Fi connection required for large downloads.")
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresStorageNotLow(true)
            .build()

        val downloadRequest = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()

        workManager.enqueueUniqueWork(
            "model_download",
            ExistingWorkPolicy.KEEP,
            downloadRequest
        )
    }

    private fun observeWorkProgress() {
        managerScope.launch {
            workManager.getWorkInfosForUniqueWorkFlow("model_download")
                .collect { workInfos ->
                    val workInfo = workInfos.firstOrNull() ?: return@collect
                    when (workInfo.state) {
                        WorkInfo.State.ENQUEUED -> {
                            if (_downloadState.value !is ModelDownloadState.Downloading) {
                                _downloadState.value = ModelDownloadState.Downloading(0f)
                            }
                        }
                        WorkInfo.State.RUNNING -> {
                            val progress = workInfo.progress.getFloat("progress", 0f)
                            val totalBytes = workInfo.progress.getLong("totalBytes", 0L)
                            val contentLength = workInfo.progress.getLong("contentLength", -1L)
                            _downloadState.value = ModelDownloadState.Downloading(progress, totalBytes, contentLength)
                        }
                        WorkInfo.State.SUCCEEDED -> {
                            _downloadState.value = ModelDownloadState.Ready
                        }
                        WorkInfo.State.FAILED -> {
                            val error = workInfo.outputData.getString("error") ?: "Download failed"
                            _downloadState.value = ModelDownloadState.Error(error)
                        }
                        WorkInfo.State.CANCELLED -> {
                            _downloadState.value = ModelDownloadState.Idle
                        }
                        else -> {}
                    }
                }
        }
    }

    private fun hasEnoughSpace(): Boolean {
        return try {
            val stat = StatFs(context.noBackupFilesDir.path)
            val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
            availableBytes >= requiredSpace
        } catch (e: Exception) {
            false
        }
    }

    private fun isWifiConnected(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
}

class ModelDownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val hfToken = com.example.myai.BuildConfig.HF_TOKEN
    private val modelUrl = "https://huggingface.co/litert-community/gemma-2-2b-it-gecko-litert/resolve/main/gemma-2-2b-it-gecko.bin"

    override suspend fun doWork(): Result {
        val destinationFile = ModelDownloadManager.getModelFile(applicationContext)
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url(modelUrl)
            .header("Authorization", "Bearer $hfToken")
            .build()

        return withContext(Dispatchers.IO) {
            try {
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext when (response.code) {
                            401 -> Result.failure(workDataOf("error" to "Invalid Hugging Face token."))
                            403 -> Result.failure(workDataOf("error" to "Access denied. Accept license on Hugging Face."))
                            else -> Result.failure(workDataOf("error" to "Server returned ${response.code}"))
                        }
                    }

                    val body = response.body ?: return@withContext Result.failure(workDataOf("error" to "Empty response body"))
                    val contentLength = body.contentLength()
                    val inputStream = body.byteStream()

                    var totalBytesRead = 0L
                    var lastUpdate = 0L

                    destinationFile.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var bytesRead: Int
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead

                            val now = System.currentTimeMillis()
                            if (now - lastUpdate > 1000) {
                                val progress = if (contentLength > 0) (totalBytesRead.toDouble() / contentLength).toFloat() else -1f
                                Log.d("ModelDownloadWorker", "Progress: ${(progress * 100).toInt()}% ($totalBytesRead/$contentLength)")
                                setProgress(workDataOf(
                                    "progress" to progress,
                                    "totalBytes" to totalBytesRead,
                                    "contentLength" to contentLength
                                ))
                                lastUpdate = now
                            }
                        }
                    }
                    Log.d("ModelDownloadWorker", "Stream complete. Total bytes: $totalBytesRead")
                    Result.success()
                }
            } catch (e: Exception) {
                Log.e("ModelDownloadWorker", "Download error", e)
                Result.retry()
            }
        }
    }
}
