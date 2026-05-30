package com.example.myai.domain.ondevice

sealed class ModelDownloadState {
    object Idle : ModelDownloadState()
    object CheckingStorage : ModelDownloadState()
    data class Downloading(
        val progress: Float,
        val totalBytes: Long = 0L,
        val contentLength: Long = -1L
    ) : ModelDownloadState()
    object InitializingEngine : ModelDownloadState()
    object Ready : ModelDownloadState()
    data class Error(val message: String) : ModelDownloadState()
}
