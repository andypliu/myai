package com.example.myai.data.source

import com.example.myai.data.local.MessageDao
import com.example.myai.data.local.MessageEntity
import com.example.myai.domain.model.ChatMessage
import com.example.myai.domain.model.FileAttachment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomChatDataSource(
    private val messageDao: MessageDao,
    private val gson: Gson = Gson()
) : LocalChatDataSource {

    override fun getMessages(): Flow<List<ChatMessage>> {
        return messageDao.getAllMessages().map { entities ->
            entities.map { entity ->
                ChatMessage(
                    id = entity.id,
                    content = entity.content,
                    isUser = entity.isUser,
                    isTyping = entity.isTyping,
                    timestamp = entity.timestamp,
                    attachments = entity.attachmentsJson?.let {
                        val type = object : TypeToken<List<FileAttachment>>() {}.type
                        gson.fromJson<List<FileAttachment>>(it, type)
                    }
                )
            }
        }
    }

    override suspend fun saveMessage(message: ChatMessage) {
        val entity = MessageEntity(
            id = message.id,
            content = message.content,
            isUser = message.isUser,
            isTyping = message.isTyping,
            timestamp = message.timestamp,
            attachmentsJson = message.attachments?.let { gson.toJson(it) }
        )
        messageDao.insertMessage(entity)
    }

    override suspend fun clearMessages() {
        messageDao.deleteAllMessages()
    }

    override suspend fun deleteMessage(id: String) {
        messageDao.deleteMessage(id)
    }
}
