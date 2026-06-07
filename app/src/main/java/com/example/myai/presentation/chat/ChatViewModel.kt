package com.example.myai.presentation.chat

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myai.data.remote.NvidiaApiService
import com.example.myai.data.remote.GoogleApiService
import com.example.myai.data.remote.OpenRouterApiService
import com.example.myai.data.source.ChatDataSource
import com.example.myai.data.repository.ChatRepositoryImpl
import com.example.myai.domain.model.AiServiceType
import com.example.myai.domain.model.ChatMessage
import com.example.myai.domain.model.FileAttachment
import com.example.myai.domain.usecase.SendMessageUseCase
import com.example.myai.domain.util.FileProcessor
import com.example.myai.data.ondevice.LiteRTLMEngine
import com.example.myai.data.ondevice.ModelDownloadManager
import com.example.myai.data.ondevice.AiCoreManager
import com.example.myai.data.ondevice.AiCoreStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(
    private val ollamaDataSource: ChatDataSource,
    private val nvidiaApiService: NvidiaApiService,
    private val googleApiService: GoogleApiService,
    private val openRouterApiService: OpenRouterApiService,
    private val context: Context
) : ViewModel() {
    private val repository = ChatRepositoryImpl(ollamaDataSource, nvidiaApiService, googleApiService, openRouterApiService)
    private val sendMessageUseCase = SendMessageUseCase(repository)
    private val litertEngine = LiteRTLMEngine.getInstance(context)
    private val downloadManager = ModelDownloadManager.getInstance(context)
    private val aiCoreManager = AiCoreManager.getInstance(context)

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Idle)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _selectedModel = MutableStateFlow("")
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _selectedService = MutableStateFlow(AiServiceType.OLLAMA)
    val selectedService: StateFlow<AiServiceType> = _selectedService.asStateFlow()

    private val _selectedAttachments = MutableStateFlow<List<FileAttachment>>(emptyList())
    val selectedAttachments: StateFlow<List<FileAttachment>> = _selectedAttachments.asStateFlow()

    private val _newMessageId = MutableStateFlow<String?>(null)
    val newMessageId: StateFlow<String?> = _newMessageId.asStateFlow()

    private val pendingRequests = MutableStateFlow(0)

    fun setSelectedModel(model: String) {
        _selectedModel.value = model
    }

    fun setSelectedService(service: AiServiceType) {
        _selectedService.value = service
    }

    fun addAttachment(attachment: FileAttachment) {
        _selectedAttachments.value = _selectedAttachments.value + attachment
    }

    fun removeAttachment(attachment: FileAttachment) {
        _selectedAttachments.value = _selectedAttachments.value.filter { it.id != attachment.id }
    }

    fun sendMessage(message: String, context: Context? = null) {
        if (message.isBlank() && _selectedAttachments.value.isEmpty()) return

        viewModelScope.launch {
            // Convert attachments to base64
            val attachmentsWithBase64 = _selectedAttachments.value.map { attachment ->
                if (context != null && FileProcessor.isImage(attachment.mimeType)) {
                    val uri = Uri.parse(attachment.uri)
                    val base64Data = FileProcessor.fileToBase64(context, uri)
                    android.util.Log.d("ChatViewModel", "Attachment: ${attachment.name}, mimeType: ${attachment.mimeType}, base64Data: ${base64Data?.take(50)}...")
                    attachment.copy(base64Data = base64Data)
                } else {
                    android.util.Log.d("ChatViewModel", "Attachment (not image or no context): ${attachment.name}, mimeType: ${attachment.mimeType}")
                    attachment
                }
            }

            android.util.Log.d("ChatViewModel", "Sending message: $message, attachments count: ${attachmentsWithBase64.size}")

            // Add user message with attachments
            val userMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                content = message,
                isUser = true,
                attachments = attachmentsWithBase64.takeIf { it.isNotEmpty() }
            )
            _messages.update { it + userMessage }

            // Notify that a new message was added
            _newMessageId.value = userMessage.id

            // Clear selected attachments
            _selectedAttachments.value = emptyList()

            // Set loading state and add typing indicator
            pendingRequests.update { it + 1 }
            _uiState.value = ChatUiState.Loading
            val assistantMessageId = UUID.randomUUID().toString()
            val typingMessage = ChatMessage(
                id = assistantMessageId,
                content = "",
                isUser = false,
                isTyping = true
            )
            _messages.update { it + typingMessage }

            val currentModel = _selectedModel.value
            try {
                if (_selectedService.value == AiServiceType.ON_DEVICE) {
                    if (!downloadManager.isModelDownloaded()) {
                        _messages.update { list -> list.filter { it.id != assistantMessageId } }
                        _uiState.value = ChatUiState.Error("Model not downloaded. Go to Profile to download.", currentModel)
                        return@launch
                    }

                    try {
                        litertEngine.initialize(downloadManager.getModelFile())
                        litertEngine.generateStreamingResponse(message)
                            .onCompletion {
                                _newMessageId.value = assistantMessageId
                            }
                            .collect { chunk ->
                                _messages.update { list ->
                                    list.map { msg ->
                                        if (msg.id == assistantMessageId) {
                                            val newContent = msg.content + chunk
                                            msg.copy(
                                                content = newContent,
                                                isTyping = newContent.isEmpty()
                                            )
                                        } else msg
                                    }
                                }
                            }
                    } catch (e: Exception) {
                        _messages.update { list -> list.filter { it.id != assistantMessageId } }
                        _uiState.value = ChatUiState.Error("Inference failed: ${e.message}", currentModel)
                    }
                } else if (_selectedService.value == AiServiceType.AICORE) {
                    val status = aiCoreManager.checkStatus()
                    if (status !is AiCoreStatus.Available) {
                        _messages.update { list -> list.filter { it.id != assistantMessageId } }
                        val errorMsg = when(status) {
                            is AiCoreStatus.AiCoreMissing -> "AI Core not installed. Go to Profile."
                            is AiCoreStatus.ModelDownloading -> "Model downloading. Please wait."
                            is AiCoreStatus.WaitingForWifi -> "Waiting for Wi-Fi to download model."
                            is AiCoreStatus.Unavailable -> "AI Core unavailable: ${status.message}"
                            else -> "AI Core not ready."
                        }
                        _uiState.value = ChatUiState.Error(errorMsg, currentModel)
                        return@launch
                    }

                    try {
                        aiCoreManager.generateStreamingResponse(message)
                            .onCompletion {
                                _newMessageId.value = assistantMessageId
                            }
                            .collect { chunk ->
                                _messages.update { list ->
                                    list.map { msg ->
                                        if (msg.id == assistantMessageId) {
                                            val newContent = msg.content + chunk
                                            msg.copy(
                                                content = newContent,
                                                isTyping = newContent.isEmpty()
                                            )
                                        } else msg
                                    }
                                }
                            }
                    } catch (e: Exception) {
                        _messages.update { list -> list.filter { it.id != assistantMessageId } }
                        _uiState.value = ChatUiState.Error("AI Core error: ${e.message}", currentModel)
                    }
                } else if (_selectedService.value == AiServiceType.NVIDIA) {
                    sendMessageUseCase.invokeStream(
                        _messages.value.filter { it.id != assistantMessageId },
                        message,
                        attachmentsWithBase64,
                        currentModel,
                        _selectedService.value
                    )
                        .onStart {
                            // Start with empty content
                        }
                        .catch { error ->
                            android.util.Log.e("ChatViewModel", "Error in stream", error)
                            _messages.update { list -> list.filter { it.id != assistantMessageId } }
                            val errorMessage = if (error is java.net.SocketTimeoutException) {
                                "Timeout, please try again later."
                            } else {
                                error.message ?: "Unknown error"
                            }
                            _uiState.value = ChatUiState.Error(errorMessage, currentModel)
                        }
                        .onCompletion {
                            _newMessageId.value = assistantMessageId
                        }
                        .collect { chunk ->
                            _messages.update { list ->
                                list.map { msg ->
                                    if (msg.id == assistantMessageId) {
                                        val newContent = msg.content + chunk
                                        msg.copy(
                                            content = newContent,
                                            isTyping = newContent.isEmpty()
                                        )
                                    } else msg
                                }
                            }
                        }
                } else {
                    val result = sendMessageUseCase(
                        _messages.value.filter { it.id != assistantMessageId },
                        message,
                        attachmentsWithBase64,
                        currentModel,
                        _selectedService.value
                    )

                    result.fold(
                        onSuccess = { response ->
                            android.util.Log.d("ChatViewModel", "Response received: ${response}...")
                            // Remove typing indicator and add actual response
                            _messages.update { list ->
                                list.map { msg ->
                                    if (msg.id == assistantMessageId) {
                                        msg.copy(content = response, isTyping = false)
                                    } else msg
                                }
                            }

                            // Notify that a new message was added
                            _newMessageId.value = assistantMessageId
                        },
                        onFailure = { error ->
                            android.util.Log.e("ChatViewModel", "Error sending message", error)
                            // Remove typing indicator on error
                            _messages.update { list -> list.filter { it.id != assistantMessageId } }
                            val errorMessage = if (error is java.net.SocketTimeoutException) {
                                "Timeout, please try again later."
                            } else {
                                error.message ?: "Unknown error"
                            }
                            _uiState.value = ChatUiState.Error(errorMessage, currentModel)
                        }
                    )
                }
            } finally {
                pendingRequests.update { it - 1 }
                if (pendingRequests.value == 0 && _uiState.value is ChatUiState.Loading) {
                    _uiState.value = ChatUiState.Success
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = ChatUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            litertEngine.close()
        }
    }
}

sealed class ChatUiState {
    object Idle : ChatUiState()
    object Loading : ChatUiState()
    object Success : ChatUiState()
    data class Error(val message: String, val model: String? = null) : ChatUiState()
}
