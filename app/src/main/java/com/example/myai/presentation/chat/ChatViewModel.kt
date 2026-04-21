package com.example.myai.presentation.chat

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myai.data.source.ChatDataSource
import com.example.myai.data.repository.ChatRepositoryImpl
import com.example.myai.domain.model.ChatMessage
import com.example.myai.domain.model.FileAttachment
import com.example.myai.domain.usecase.SendMessageUseCase
import com.example.myai.domain.util.FileProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(
    private val dataSource: ChatDataSource
) : ViewModel() {
    private val repository = ChatRepositoryImpl(dataSource)
    private val sendMessageUseCase = SendMessageUseCase(repository)

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Idle)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _selectedModel = MutableStateFlow("")
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _selectedAttachments = MutableStateFlow<List<FileAttachment>>(emptyList())
    val selectedAttachments: StateFlow<List<FileAttachment>> = _selectedAttachments.asStateFlow()

    private val _newMessageId = MutableStateFlow<String?>(null)
    val newMessageId: StateFlow<String?> = _newMessageId.asStateFlow()

    fun setSelectedModel(model: String) {
        _selectedModel.value = model
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
            _messages.value = _messages.value + userMessage

            // Notify that a new message was added
            _newMessageId.value = userMessage.id

            // Clear selected attachments
            _selectedAttachments.value = emptyList()

            // Set loading state and add typing indicator
            _uiState.value = ChatUiState.Loading
            val typingMessage = ChatMessage(
                id = "typing",
                content = "...",
                isUser = false,
                isTyping = true
            )
            _messages.value = _messages.value + typingMessage

            // Send to Ollama with attachments
            val result = sendMessageUseCase(
                _messages.value.filter { !it.isTyping },
                message,
                attachmentsWithBase64,
                _selectedModel.value
            )

            result.fold(
                onSuccess = { response ->
                    android.util.Log.d("ChatViewModel", "Response received: ${response.take(100)}...")
                    // Remove typing indicator and add actual response
                    _messages.value = _messages.value.filter { !it.isTyping }
                    val assistantMessage = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        content = response,
                        isUser = false
                    )
                    _messages.value = _messages.value + assistantMessage

                    // Notify that a new message was added
                    _newMessageId.value = assistantMessage.id

                    _uiState.value = ChatUiState.Success
                },
                onFailure = { error ->
                    android.util.Log.e("ChatViewModel", "Error sending message", error)
                    // Remove typing indicator on error
                    _messages.value = _messages.value.filter { !it.isTyping }
                    _uiState.value = ChatUiState.Error(error.message ?: "Unknown error")
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = ChatUiState.Idle
    }
}

sealed class ChatUiState {
    object Idle : ChatUiState()
    object Loading : ChatUiState()
    object Success : ChatUiState()
    data class Error(val message: String) : ChatUiState()
}
