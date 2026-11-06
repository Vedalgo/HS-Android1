package by.grinich.hscontrol.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
    object Processing : AuthState()
    data class Authenticated(val profile: UserProfile) : AuthState()
}

class AuthViewModel : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    init {
        checkSavedSession()
    }

    private fun checkSavedSession() {
        viewModelScope.launch {
            try {
                kotlinx.coroutines.delay(150)
                val profile = AuthRepository.getCurrentProfileOrNull()
                _state.value = if (profile != null) {
                    AuthState.Authenticated(profile)
                } else {
                    AuthState.Unauthenticated
                }
            } catch (e: Exception) {
                _state.value = AuthState.Unauthenticated
            }
        }
    }

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = AuthState.Error("Заполните все поля")
            return
        }
        viewModelScope.launch {
            _state.value = AuthState.Processing
            try {
                val profile = AuthRepository.signIn(email.trim(), password)
                _state.value = AuthState.Authenticated(profile)
            } catch (e: Exception) {
                _state.value = AuthState.Error(mapError(e.message ?: "Ошибка входа"))
            }
        }
    }

    fun signUp(email: String, password: String, fullName: String) {
        if (email.isBlank() || password.isBlank() || fullName.isBlank()) {
            _state.value = AuthState.Error("Заполните все поля")
            return
        }
        if (password.length < 6) {
            _state.value = AuthState.Error("Пароль должен быть не менее 6 символов")
            return
        }
        viewModelScope.launch {
            _state.value = AuthState.Processing
            try {
                val profile = AuthRepository.signUp(email.trim(), password, fullName.trim())
                _state.value = AuthState.Authenticated(profile)
            } catch (e: Exception) {
                _state.value = AuthState.Error(mapError(e.message ?: "Ошибка регистрации"))
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                AuthRepository.signOut()
            } catch (_: Exception) {}
            _state.value = AuthState.Unauthenticated
        }
    }

    fun clearError() {
        if (_state.value is AuthState.Error) {
            _state.value = AuthState.Unauthenticated
        }
    }

    private fun mapError(msg: String): String = when {
        msg.contains("Invalid login credentials", true) -> "Неверный email или пароль"
        msg.contains("User already registered", true) -> "Пользователь с таким email уже зарегистрирован"
        msg.contains("Email not confirmed", true) -> "Email не подтверждён. Проверьте почту"
        msg.contains("Password should be at least", true) -> "Пароль слишком короткий (мин. 6 символов)"
        msg.contains("Unable to validate email", true) -> "Некорректный email"
        msg.contains("network", true) -> "Нет подключения к интернету"
        else -> msg
    }
}