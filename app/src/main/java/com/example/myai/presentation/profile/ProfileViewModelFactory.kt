package com.example.myai.presentation.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myai.data.repository.ModelsRepositoryImpl
import com.example.myai.data.remote.OllamaModelsService
import com.example.myai.domain.usecase.GetModelsUseCase

class ProfileViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            val modelsRepository = ModelsRepositoryImpl(OllamaModelsService())
            val getModelsUseCase = GetModelsUseCase(modelsRepository)
            return ProfileViewModel(context, getModelsUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
