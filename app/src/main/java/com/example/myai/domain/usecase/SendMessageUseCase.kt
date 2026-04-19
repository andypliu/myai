package com.example.myai.domain.usecase

import android.util.Log
import com.example.myai.data.model.ChatRequest
import com.example.myai.data.model.ChatRequestMessage
import com.example.myai.domain.model.ChatMessage
import com.example.myai.domain.model.FileAttachment
import com.example.myai.domain.repository.ChatRepository

class SendMessageUseCase(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(
        messages: List<ChatMessage>,
        newMessage: String,
        attachments: List<FileAttachment> = emptyList(),
        model: String = "gemma4:e2b"
    ): Result<String> {
        val requestMessages = messages.map {
            ChatRequestMessage(
                role = if (it.isUser) "user" else "assistant",
                content = it.content
            )
        } + ChatRequestMessage(role = "user", content = newMessage)

        // Extract base64 images from attachments
        val images = attachments
            .filter { it.base64Data != null }
            .mapNotNull { it.base64Data }

        Log.d("SendMessageUseCase", "Images count: ${images.size}, first image length: ${images.firstOrNull()?.length ?: 0}")

        val request = ChatRequest(
            model = model,
            messages = requestMessages,
            stream = false,
            images = images.takeIf { it.isNotEmpty() }
        )

        Log.d("SendMessageUseCase", "Request: model=${request.model}, messages count=${request.messages.size}, images count=${request.images?.size ?: 0}")

        return repository.sendMessage(request).map { it.message.content }
    }
}
