package com.example.myai.presentation.login

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myai.data.config.ApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import java.util.concurrent.TimeUnit

class LoginViewModel(
    private val context: Context
) : ViewModel() {

    private val prefs: SharedPreferences = context.getSharedPreferences("MyAIPrefs", Context.MODE_PRIVATE)
    private val PREF_IS_LOGGED_IN = "is_logged_in"
    private val PREF_USERNAME = "saved_username"
    private val PREF_PASSWORD = "saved_password"

    private val _username = MutableStateFlow(prefs.getString(PREF_USERNAME, "") ?: "")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _password = MutableStateFlow(prefs.getString(PREF_PASSWORD, "") ?: "")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(prefs.getBoolean(PREF_IS_LOGGED_IN, false))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .protocols(listOf(Protocol.HTTP_1_1))
        .build()

    init {
        Log.d("LoginViewModel", "Initialized - isLoggedIn: ${_isLoggedIn.value}, savedUsername: ${_username.value}")
    }

    fun onUsernameChange(newUsername: String) {
        _username.value = newUsername
        _loginError.value = null
    }

    fun onPasswordChange(newPassword: String) {
        _password.value = newPassword
        _loginError.value = null
    }

    fun login() {
        viewModelScope.launch {
            _isLoading.value = true
            _loginError.value = null

            Log.d("LoginViewModel", "Starting login with username: ${_username.value}")

            // Validate credentials by making an API call to Caddy
            val result = validateCredentials(_username.value, _password.value)

            result.onSuccess {
                Log.d("LoginViewModel", "Login successful, saving credentials")
                // Save credentials and login state
                prefs.edit()
                    .putBoolean(PREF_IS_LOGGED_IN, true)
                    .putString(PREF_USERNAME, _username.value)
                    .putString(PREF_PASSWORD, _password.value)
                    .apply()
                _isLoggedIn.value = true
                Log.d("LoginViewModel", "Credentials saved, isLoggedIn set to true")
            }.onFailure { error ->
                Log.e("LoginViewModel", "Login failed: ${error.message}")
                _loginError.value = error.message ?: "Login failed"
            }

            _isLoading.value = false
        }
    }

    private suspend fun validateCredentials(username: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val credentials = "$username:$password"
            val auth = "Basic ${Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)}"

            Log.d("LoginViewModel", "Attempting login to: ${ApiConfig.MODELS_ENDPOINT}")
            Log.d("LoginViewModel", "Authorization header: Basic ${Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP).take(20)}...")

            val request = Request.Builder()
                .url(ApiConfig.MODELS_ENDPOINT)
                .header("Authorization", auth)
                .get()
                .build()

            Log.d("LoginViewModel", "Sending request...")
            val response = client.newCall(request).execute()
            Log.d("LoginViewModel", "Response received: ${response.code}")

            when (response.code) {
                200 -> {
                    Log.d("LoginViewModel", "Login successful (HTTP 200)")
                    Result.success(Unit)
                }
                401 -> {
                    Log.w("LoginViewModel", "Login failed: HTTP 401 Unauthorized")
                    Result.failure(Exception("Invalid username or password"))
                }
                else -> {
                    val errorBody = response.body?.string()
                    Log.e("LoginViewModel", "Server error: ${response.code}, body: $errorBody")
                    Result.failure(Exception("Server error: ${response.code}"))
                }
            }
        } catch (e: javax.net.ssl.SSLHandshakeException) {
            Log.e("LoginViewModel", "SSL/HTTPS certificate error", e)
            Result.failure(Exception("SSL error: Certificate not trusted. Check if using HTTPS with valid cert."))
        } catch (e: java.net.ConnectException) {
            Log.e("LoginViewModel", "Connection error - server not reachable", e)
            Result.failure(Exception("Connection failed: Server not reachable at ${ApiConfig.HOST}"))
        } catch (e: Exception) {
            Log.e("LoginViewModel", "Network/connection error: ${e::class.java.simpleName}", e)
            Result.failure(Exception("Network error: ${e.message} (${e::class.java.simpleName})"))
        }
    }

    fun logout() {
        _isLoggedIn.value = false
        _username.value = ""
        _password.value = ""
        prefs.edit()
            .putBoolean(PREF_IS_LOGGED_IN, false)
            .putString(PREF_USERNAME, "")
            .putString(PREF_PASSWORD, "")
            .apply()
    }

    fun clearError() {
        _loginError.value = null
    }
}
