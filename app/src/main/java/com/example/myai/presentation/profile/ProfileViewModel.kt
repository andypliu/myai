package com.example.myai.presentation.profile

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myai.domain.usecase.GetModelsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val context: Context,
    private val getModelsUseCase: GetModelsUseCase
) : ViewModel() {

    private val prefs: SharedPreferences = context.getSharedPreferences("MyAIPrefs", Context.MODE_PRIVATE)
    private val PREF_SELECTED_MODEL = "selected_model"

    private val _selectedModel = MutableStateFlow(prefs.getString(PREF_SELECTED_MODEL, "gemma4:e2b") ?: "gemma4:e2b")
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _availableModels = MutableStateFlow<List<String>>(emptyList())
    val availableModels: StateFlow<List<String>> = _availableModels.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        fetchModels()
    }

    fun fetchModels() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            getModelsUseCase()
                .onSuccess { models ->
                    _availableModels.value = models.map { it.name }
                    // If selected model is not in the list, select the first one
                    if (!_availableModels.value.contains(_selectedModel.value) && _availableModels.value.isNotEmpty()) {
                        _selectedModel.value = _availableModels.value.first()
                        prefs.edit().putString(PREF_SELECTED_MODEL, _selectedModel.value).apply()
                    }
                }
                .onFailure { error ->
                    _error.value = error.message ?: "Failed to fetch models"
                    // Fallback to default models if fetch fails
                    _availableModels.value = listOf(
                        "gemma4:e2b",
                        "gemma4:26b",
                        "glm-5:cloud",
                        "minimax-m2.5:cloud",
                        "deepseek-coder:1.3b",
                        "qwen2.5:32b",
                        "ds-reasoner:latest",
                        "deepseek-r1:latest",
                        "glm-4.7-flash:latest",
                        "kimi-k2.5:cloud",
                        "deepseek-r1:14b",
                        "phi3:latest",
                        "gemma3:270m",
                        "deepseek-r1:32b",
                        "deepseek-r1:7b"
                    )
                }
            _isLoading.value = false
        }
    }

    fun selectModel(model: String) {
        viewModelScope.launch {
            _selectedModel.value = model
            prefs.edit().putString(PREF_SELECTED_MODEL, model).apply()
        }
    }

    fun clearError() {
        _error.value = null
    }
}
