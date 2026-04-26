package com.example.myai.presentation.profile

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myai.domain.model.AiServiceType
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
    private val PREF_SELECTED_SERVICE = "selected_service"

    private val _selectedService = MutableStateFlow(
        AiServiceType.valueOf(prefs.getString(PREF_SELECTED_SERVICE, AiServiceType.OLLAMA.name) ?: AiServiceType.OLLAMA.name)
    )
    val selectedService: StateFlow<AiServiceType> = _selectedService.asStateFlow()

    private val _selectedModel = MutableStateFlow(prefs.getString(PREF_SELECTED_MODEL, "") ?: "")
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _availableModels = MutableStateFlow<List<String>>(emptyList())
    val availableModels: StateFlow<List<String>> = _availableModels.asStateFlow()

    private val _unauthorizedModels = MutableStateFlow<Set<String>>(emptySet())
    val unauthorizedModels: StateFlow<Set<String>> = _unauthorizedModels.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        fetchModels()
    }

    fun fetchModels(serviceType: AiServiceType = _selectedService.value) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            getModelsUseCase(serviceType)
                .onSuccess { models ->
                    _availableModels.value = models.map { it.name }
                    // If selected model is empty or not in the list, select the first one
                    if ((_selectedModel.value.isEmpty() || !_availableModels.value.contains(_selectedModel.value)) && _availableModels.value.isNotEmpty()) {
                        _selectedModel.value = _availableModels.value.first()
                        prefs.edit().putString(PREF_SELECTED_MODEL, _selectedModel.value).apply()
                    }
                }
                .onFailure { error ->
                    _error.value = error.message ?: "Failed to fetch models"
                    _availableModels.value = emptyList()
                }
            _isLoading.value = false
        }
    }

    fun selectService(serviceType: AiServiceType) {
        if (_selectedService.value != serviceType) {
            _selectedService.value = serviceType
            prefs.edit().putString(PREF_SELECTED_SERVICE, serviceType.name).apply()
            // Clear selected model as it belongs to the previous service
            _selectedModel.value = ""
            prefs.edit().putString(PREF_SELECTED_MODEL, "").apply()
            fetchModels(serviceType)
        }
    }

    fun selectModel(model: String) {
        viewModelScope.launch {
            _selectedModel.value = model
            // Clear from unauthorized list when selected, so it's no longer grayed out
            _unauthorizedModels.value = _unauthorizedModels.value - model
            prefs.edit().putString(PREF_SELECTED_MODEL, model).apply()
        }
    }

    fun markModelAsUnauthorized(model: String) {
        _unauthorizedModels.value = _unauthorizedModels.value + model
    }

    fun clearError() {
        _error.value = null
    }
}
