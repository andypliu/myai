package com.example.myai.presentation.favorites

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myai.data.local.AppDatabase
import com.example.myai.data.local.MessageFeedback
import com.example.myai.data.repository.MessageRepositoryImpl
import com.example.myai.data.source.RoomChatDataSource
import com.example.myai.domain.repository.MessageRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FavoritesViewModel(context: Context) : ViewModel() {
    private val database = AppDatabase.getDatabase(context)
    private val feedbackDao = database.messageFeedbackDao()
    private val messageRepository: MessageRepository = MessageRepositoryImpl(
        RoomChatDataSource(database.messageDao())
    )

    val lovedMessages: StateFlow<List<MessageFeedback>> = feedbackDao.getAllFeedback()
        .map { feedbacks -> feedbacks.filter { it.isLiked }.sortedByDescending { it.timestamp } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun removeFavorite(messageId: String) {
        viewModelScope.launch {
            feedbackDao.deleteFeedback(messageId)
        }
    }

    fun toggleFeedback(messageId: String, isLiked: Boolean) {
        viewModelScope.launch {
            val current = feedbackDao.getFeedbackForMessage(messageId)
            current?.let {
                feedbackDao.insertFeedback(it.copy(isLiked = isLiked))
            }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            messageRepository.deleteMessage(messageId)
            feedbackDao.deleteFeedback(messageId)
        }
    }
}
