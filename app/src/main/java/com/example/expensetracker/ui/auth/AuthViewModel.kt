package com.example.expensetracker.ui.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.expensetracker.ExpenseApp
import com.example.expensetracker.data.repository.AuthRepository
import com.example.expensetracker.data.repository.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Единое состояние экрана: UI только рисует то, что здесь лежит. */
data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isRegisterMode: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) =
        _uiState.update { it.copy(email = value.trim(), errorMessage = null) }

    fun onPasswordChange(value: String) =
        _uiState.update { it.copy(password = value, errorMessage = null) }

    fun togglePasswordVisibility() =
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }

    fun toggleMode() =
        _uiState.update { it.copy(isRegisterMode = !it.isRegisterMode, errorMessage = null) }

    /** Нажатие на главную кнопку: валидация, затем вход или регистрация. */
    fun submit() {
        val state = _uiState.value
        if (state.isLoading) return

        val error = when {
            !Patterns.EMAIL_ADDRESS.matcher(state.email).matches() -> "Введите корректную почту"
            state.password.length < 6 -> "Пароль должен быть не короче 6 символов"
            else -> null
        }
        if (error != null) {
            _uiState.update { it.copy(errorMessage = error) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = if (state.isRegisterMode) {
                repository.register(state.email, state.password)
            } else {
                repository.login(state.email, state.password)
            }
            _uiState.update {
                when (result) {
                    AuthResult.Success -> it.copy(isLoading = false, isSuccess = true)
                    is AuthResult.Error -> it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    companion object {
        /** Фабрика: достаёт репозиторий из AppContainer. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ExpenseApp
                AuthViewModel(app.container.authRepository)
            }
        }
    }
}