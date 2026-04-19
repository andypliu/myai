package com.example.myai.data.mapper

import com.example.myai.data.model.ChatMessageDTO
import com.example.myai.data.model.FileAttachmentDTO
import com.example.myai.domain.model.ChatMessage
import com.example.myai.domain.model.FileAttachment

/**
 * Mapper for converting ChatMessage DTOs to domain models.
 */
object ChatMessageMapper {

    /**
     * Convert ChatMessageDTO to ChatMessage domain model.
     */
    fun toDomainModel(dto: ChatMessageDTO): ChatMessage {
        return ChatMessage(
            id = dto.id,
            content = dto.content,
            isUser = dto.isUser,
            isTyping = dto.isTyping,
            attachments = dto.attachments?.map { toDomainModel(it) },
            timestamp = dto.timestamp
        )
    }

    /**
     * Convert ChatMessage domain model to ChatMessageDTO.
     */
    fun toDTO(domain: ChatMessage): ChatMessageDTO {
        return ChatMessageDTO(
            id = domain.id,
            content = domain.content,
            isUser = domain.isUser,
            isTyping = domain.isTyping,
            attachments = domain.attachments?.map { toDTO(it) },
            timestamp = domain.timestamp
        )
    }

    /**
     * Convert FileAttachmentDTO to FileAttachment domain model.
     */
    fun toDomainModel(dto: FileAttachmentDTO): FileAttachment {
        return FileAttachment(
            id = dto.id,
            uri = dto.uri,
            name = dto.name,
            mimeType = dto.mimeType,
            size = dto.size,
            base64Data = dto.base64Data
        )
    }

    /**
     * Convert FileAttachment domain model to FileAttachmentDTO.
     */
    fun toDTO(domain: FileAttachment): FileAttachmentDTO {
        return FileAttachmentDTO(
            id = domain.id,
            uri = domain.uri,
            name = domain.name,
            mimeType = domain.mimeType,
            size = domain.size,
            base64Data = domain.base64Data
        )
    }
}
