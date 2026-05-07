package com.example.hoopmaster.viewmodels

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.hoopmaster.core.di.AppContainer
import com.example.hoopmaster.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentUserId: String? = null
)

sealed interface AuthAction {
    data class EmailChanged(val value: String) : AuthAction
    data class PasswordChanged(val value: String) : AuthAction
    data object LoginClicked : AuthAction
    data object SignupClicked : AuthAction
    data object ClearError : AuthAction
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val container = AppContainer(application)
    private val authRepository: AuthRepository = container.authRepository

    private val _uiState = MutableStateFlow(
        AuthUiState(currentUserId = container.sessionStore.getUserId())
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    var email = mutableStateOf(_uiState.value.email)
    var password = mutableStateOf(_uiState.value.password)
    var isLoading = mutableStateOf(_uiState.value.isLoading)
    var errorMessage = mutableStateOf(_uiState.value.errorMessage)
    var currentUserId = mutableStateOf(_uiState.value.currentUserId)

    fun onAction(action: AuthAction) {
        when (action) {
            is AuthAction.EmailChanged -> {
                email.value = action.value
                syncState { it.copy(email = action.value) }
            }
            is AuthAction.PasswordChanged -> {
                password.value = action.value
                syncState { it.copy(password = action.value) }
            }
            AuthAction.LoginClicked -> login {}
            AuthAction.SignupClicked -> signup {}
            AuthAction.ClearError -> syncState { it.copy(errorMessage = null) }
        }
    }

    fun login(onSuccess: () -> Unit) {
        submitAuth(isSignup = false, onSuccess = onSuccess)
    }

    fun signup(onSuccess: () -> Unit) {
        submitAuth(isSignup = true, onSuccess = onSuccess)
    }

    private fun submitAuth(isSignup: Boolean, onSuccess: () -> Unit) {
        viewModelScope.launch {
            syncState {
                it.copy(
                    email = email.value,
                    password = password.value,
                    isLoading = true,
                    errorMessage = null
                )
            }

            val result = if (isSignup) {
                authRepository.signup(
                    username = email.value.substringBefore("@").ifBlank { email.value },
                    email = email.value,
                    password = password.value,
                    name = null
                )
            } else {
                authRepository.login(
                    usernameOrEmail = email.value,
                    password = password.value
                )
            }

            result.fold(
                onSuccess = { user ->
                    syncState {
                        it.copy(
                            isLoading = false,
                            currentUserId = user.id,
                            errorMessage = null
                        )
                    }
                    onSuccess()
                },
                onFailure = {
                    syncState {
                        it.copy(
                            isLoading = false,
                            errorMessage = if (isSignup) {
                                "Đăng ký thất bại, email có thể đã tồn tại!"
                            } else {
                                "Sai email hoặc mật khẩu!"
                            }
                        )
                    }
                }
            )
        }
    }

    private fun syncState(transform: (AuthUiState) -> AuthUiState) {
        val next = transform(_uiState.value)
        _uiState.value = next
        email.value = next.email
        password.value = next.password
        isLoading.value = next.isLoading
        errorMessage.value = next.errorMessage
        currentUserId.value = next.currentUserId
    }
}
