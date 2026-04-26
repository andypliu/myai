package com.example.myai.domain.usecase

import android.util.Log
import com.example.myai.data.model.ChatRequest
import com.example.myai.data.model.ChatRequestMessage
import com.example.myai.domain.model.AiServiceType
import com.example.myai.domain.model.ChatMessage
import com.example.myai.domain.model.FileAttachment
import com.example.myai.domain.repository.ChatRepository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SendMessageUseCase(
    private val repository: ChatRepository
) {
    fun invokeStream(
        messages: List<ChatMessage>,
        newMessage: String,
        attachments: List<FileAttachment> = emptyList(),
        model: String = "gemma4:e2b",
        serviceType: AiServiceType = AiServiceType.OLLAMA
    ): Flow<String> {
        val currentImages = attachments
            .filter { it.base64Data != null }
            .mapNotNull { it.base64Data }

        val requestMessages = messages.map {
            ChatRequestMessage(
                role = if (it.isUser) "user" else "assistant",
                content = it.content,
                images = if (it.isUser) {
                    it.attachments?.filter { att -> att.base64Data != null }?.mapNotNull { att -> att.base64Data }
                } else null
            )
        } + ChatRequestMessage(
            role = "user",
            content = newMessage,
            images = currentImages.takeIf { it.isNotEmpty() }
        )

        val request = ChatRequest(
            model = model,
            messages = requestMessages,
            stream = true,
            images = currentImages.takeIf { it.isNotEmpty() }
        )

        return repository.sendMessageStream(request, serviceType)
    }

    suspend operator fun invoke(
        messages: List<ChatMessage>,
        newMessage: String,
        attachments: List<FileAttachment> = emptyList(),
        model: String = "gemma4:e2b",
        serviceType: AiServiceType = AiServiceType.OLLAMA
    ): Result<String> {
        // Extract base64 images from attachments
        val currentImages = attachments
            .filter { it.base64Data != null }
            .mapNotNull { it.base64Data }

        Log.d("SendMessageUseCase", "Images count: ${currentImages.size}, first image length: ${currentImages.firstOrNull()?.length ?: 0}")

        val requestMessages = messages.map {
            ChatRequestMessage(
                role = if (it.isUser) "user" else "assistant",
                content = it.content,
                // If the message had attachments when it was created, we should ideally include them
                // but for now we focus on the current message's attachments
                images = if (it.isUser) {
                    it.attachments?.filter { att -> att.base64Data != null }?.mapNotNull { att -> att.base64Data }
                } else null
            )
        } + ChatRequestMessage(
            role = "user", 
            content = newMessage,
            images = currentImages.takeIf { it.isNotEmpty() }
        )

        val request = ChatRequest(
            model = model,
            messages = requestMessages,
            stream = false,
            // Keep top-level images for backward compatibility or models that expect it there
            images = currentImages.takeIf { it.isNotEmpty() }
        )

        Log.d("SendMessageUseCase", "Request: model=${request.model}, messages count=${request.messages.size}, images in last message=${currentImages.size}")

        return repository.sendMessage(request, serviceType).map { it.message.content }
    }
}
