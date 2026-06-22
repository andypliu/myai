package com.example.myai.domain.usecase

import com.example.myai.domain.repository.MessageRepository

class DeleteMessageUseCase(
    private val repository: MessageRepository
) {
    suspend operator fun invoke(messageId: String) {
        repository.deleteMessage(messageId)
    }
}