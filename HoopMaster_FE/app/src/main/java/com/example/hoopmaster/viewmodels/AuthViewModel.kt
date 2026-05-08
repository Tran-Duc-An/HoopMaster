package com.example.hoopmaster.viewmodels

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hoopmaster.network.AuthRequest
import kotlinx.coroutines.launch
import com.example.hoopmaster.network.RetrofitClient


class AuthViewModel : ViewModel() {
    var email = mutableStateOf("")
    var password = mutableStateOf("")

    var isLoading = mutableStateOf(false)
    var errorMessage = mutableStateOf<String?>(null)

    // Lưu ID user sau khi login thành công để dùng cho các màn hình khác
    var currentUserId = mutableStateOf<String?>(null)

    fun login(onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            try {
                val request = AuthRequest(email.value, password.value)
                val response = RetrofitClient.apiService.login(request)

                if (response.isSuccessful && response.body() != null) {
                    currentUserId.value = response.body()?.user?._id
                    onSuccess() // Chuyển trang khi thành công
                } else {
                    errorMessage.value = "Sai email hoặc mật khẩu!"
                }
            } catch (e: Exception) {
                errorMessage.value = "Lỗi kết nối: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun signup(onSuccess: () -> Unit) {
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            try {
                val request = AuthRequest(email.value, password.value)
                val response = RetrofitClient.apiService.signup(request)

                if (response.isSuccessful && response.body() != null) {
                    currentUserId.value = response.body()?.user?._id
                    onSuccess()
                } else {
                    errorMessage.value = "Đăng ký thất bại, email có thể đã tồn tại!"
                }
            } catch (e: Exception) {
                errorMessage.value = "Lỗi kết nối: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }
}