package com.example.myai.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myai.data.remote.OllamaApiService
import com.example.myai.data.source.ChatDataSource
import com.example.myai.data.source.OllamaDataSource

class ChatViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            val apiService = OllamaApiService()
            val dataSource: ChatDataSource = OllamaDataSource(apiService)
            return ChatViewModel(dataSource) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
