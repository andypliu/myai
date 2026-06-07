package com.example.myai.presentation.profile

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myai.domain.model.AiServiceType
import com.example.myai.domain.usecase.GetModelsUseCase
import com.example.myai.data.ondevice.LiteRTLMEngine
import com.example.myai.data.ondevice.ModelDownloadManager
import com.example.myai.data.ondevice.AiCoreManager
import com.example.myai.data.ondevice.AiCoreStatus
import com.example.myai.domain.ondevice.ModelDownloadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val context: Context,
    private val getModelsUseCase: GetModelsUseCase
) : ViewModel() {

    private val downloadManager = ModelDownloadManager.getInstance(context)
    private val litertEngine = LiteRTLMEngine.getInstance(context)
    private val aiCoreManager = AiCoreManager.getInstance(context)

    private val _onDeviceDownloadState = MutableStateFlow<ModelDownloadState>(ModelDownloadState.Idle)
    val onDeviceDownloadState: StateFlow<ModelDownloadState> = _onDeviceDownloadState.asStateFlow()

    private val _aiCoreStatus = MutableStateFlow<AiCoreStatus>(AiCoreStatus.Unavailable("Checking..."))
    val aiCoreStatus: StateFlow<AiCoreStatus> = _aiCoreStatus.asStateFlow()

    val aiCoreDownloadProgress = aiCoreManager.downloadProgress

    private val prefs: SharedPreferences = context.getSharedPreferences("MyAIPrefs", Context.MODE_PRIVATE)
    private val PREF_SELECTED_MODEL = "selected_model"
    private val PREF_SELECTED_SERVICE = "selected_service"
    private val PREF_USE_LOCAL = "use_local_host"
    private val PREF_USE_SECURITY = "use_security"
    private val PREF_IS_DARK_MODE = "is_dark_mode"

    private val _useLocalHost = MutableStateFlow(prefs.getBoolean(PREF_USE_LOCAL, false))
    val useLocalHost: StateFlow<Boolean> = _useLocalHost.asStateFlow()

    private val _useSecurity = MutableStateFlow(prefs.getBoolean(PREF_USE_SECURITY, true))
    val useSecurity: StateFlow<Boolean> = _useSecurity.asStateFlow()

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean(PREF_IS_DARK_MODE, false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

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
        observeDownloadState()
        checkAiCoreStatus()
    }

    fun checkAiCoreStatus() {
        viewModelScope.launch {
            val status = aiCoreManager.checkStatus()
            _aiCoreStatus.value = status
            if (status is AiCoreStatus.Available && _selectedService.value == AiServiceType.AICORE) {
                _selectedModel.value = "Gemini Nano (AI Core)"
                prefs.edit().putString(PREF_SELECTED_MODEL, _selectedModel.value).apply()
                // Warm up the model in background to reduce first-token latency
                aiCoreManager.warmup()
            }
        }
    }

    fun openAiCorePlayStore() {
        AiCoreManager.openAiCoreOnPlayStore(context)
    }

    private fun observeDownloadState() {
        viewModelScope.launch {
            downloadManager.downloadState.collectLatest { state ->
                _onDeviceDownloadState.value = state
                if (state is ModelDownloadState.Ready) {
                    initializeOnDeviceEngine()
                }
            }
        }
    }

    private fun initializeOnDeviceEngine() {
        viewModelScope.launch {
            _onDeviceDownloadState.value = ModelDownloadState.InitializingEngine
            try {
                litertEngine.initialize(downloadManager.getModelFile())
                _onDeviceDownloadState.value = ModelDownloadState.Ready
                if (_selectedService.value == AiServiceType.ON_DEVICE) {
                    _selectedModel.value = "Gemma 2B (On-Device)"
                }
            } catch (e: Exception) {
                _onDeviceDownloadState.value = ModelDownloadState.Error("Init failed: ${e.message}")
            }
        }
    }

    fun startOnDeviceDownload() {
        downloadManager.startDownload()
    }

    fun startAiCoreDownload() {
        viewModelScope.launch {
            aiCoreManager.downloadModel()
            checkAiCoreStatus()
        }
    }

    fun fetchModels(serviceType: AiServiceType = _selectedService.value, refresh: Boolean = false) {
        if (serviceType == AiServiceType.ON_DEVICE) {
            _availableModels.value = listOf("Gemma 2B (On-Device)")
            if (downloadManager.isModelDownloaded()) {
                _selectedModel.value = "Gemma 2B (On-Device)"
            }
            return
        }
        if (serviceType == AiServiceType.AICORE) {
            _availableModels.value = listOf("Gemma 4 (AI Core)")
            checkAiCoreStatus()
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            getModelsUseCase(serviceType, refresh)
                .onSuccess { models ->
                    _availableModels.value = models.map { it.name }
                    // For non-NVIDIA services, if selected model is empty or not in the list, select the first one
                    if (_selectedService.value != AiServiceType.NVIDIA) {
                        if ((_selectedModel.value.isEmpty() || !_availableModels.value.contains(_selectedModel.value)) && _availableModels.value.isNotEmpty()) {
                            _selectedModel.value = _availableModels.value.first()
                            prefs.edit().putString(PREF_SELECTED_MODEL, _selectedModel.value).apply()
                        }
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

    fun toggleLocalHost(enabled: Boolean) {
        _useLocalHost.value = enabled
        prefs.edit().putBoolean(PREF_USE_LOCAL, enabled).apply()
        fetchModels(_selectedService.value, refresh = true)
    }

    fun toggleSecurity(enabled: Boolean) {
        _useSecurity.value = enabled
        prefs.edit().putBoolean(PREF_USE_SECURITY, enabled).apply()
        fetchModels(_selectedService.value, refresh = true)
    }

    fun toggleDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        prefs.edit().putBoolean(PREF_IS_DARK_MODE, enabled).apply()
    }

    fun markModelAsUnauthorized(model: String) {
        _unauthorizedModels.value = _unauthorizedModels.value + model
    }

    fun clearError() {
        _error.value = null
    }
}
