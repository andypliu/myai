package com.example.myai.presentation.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myai.data.remote.NvidiaApiService
import com.example.myai.data.remote.OllamaApiService
import com.example.myai.data.source.ChatDataSource
import com.example.myai.data.source.OllamaDataSource

class ChatViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            val apiService = OllamaApiService(context)
            val dataSource: ChatDataSource = OllamaDataSource(apiService)
            val nvidiaApiService = NvidiaApiService(context)
            return ChatViewModel(dataSource, nvidiaApiService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
