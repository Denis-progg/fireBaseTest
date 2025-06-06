package com.example.firebasetest.loginViewModel

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.firebasetest.auth.UserRoleManager
import com.example.firebasetest.worktimetracker.TrackingService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "LoginViewModel"
private const val PREFS_NAME = "login_prefs"
private const val KEY_EMAIL = "saved_email"
private const val KEY_PASSWORD = "saved_password"

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val message: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class LoginViewModel(private val context: Context) : ViewModel() {

    private val _email = MutableStateFlow("")
    val email = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password = _password.asStateFlow()

    private val _resetEmail = MutableStateFlow("")
    val resetEmail = _resetEmail.asStateFlow()

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _showForgotPassword = MutableStateFlow(false)
    val showForgotPassword = _showForgotPassword.asStateFlow()

    private val _emailError = MutableStateFlow<String?>(null)
    val emailError = _emailError.asStateFlow()

    private val _passwordError = MutableStateFlow<String?>(null)
    val passwordError = _passwordError.asStateFlow()

    private val _resetEmailError = MutableStateFlow<String?>(null)
    val resetEmailError = _resetEmailError.asStateFlow()

    private val auth = FirebaseAuth.getInstance()

    init {
        loadSavedCredentials()
    }

    private fun loadSavedCredentials() {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedEmail = sharedPrefs.getString(KEY_EMAIL, "") ?: ""
        val savedPassword = sharedPrefs.getString(KEY_PASSWORD, "") ?: ""

        if (savedEmail.isNotEmpty()) {
            _email.value = savedEmail
        }
        if (savedPassword.isNotEmpty()) {
            _password.value = savedPassword
        }
    }

    private fun saveCredentials(email: String, password: String) {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .putString(KEY_EMAIL, email)
            .putString(KEY_PASSWORD, password)
            .apply()
    }

    fun onEmailChange(newEmail: String) {
        _email.value = newEmail
        _emailError.value = null
        if (showForgotPassword.value) {
            _resetEmailError.value = null
        }
    }

    fun onPasswordChange(newPassword: String) {
        _password.value = newPassword
        _passwordError.value = null
    }

    fun onResetEmailChange(newEmail: String) {
        _resetEmail.value = newEmail
        _resetEmailError.value = null
    }

    fun toggleForgotPasswordForm(show: Boolean) {
        _showForgotPassword.value = show
        if (!show) {
            _uiState.value = AuthUiState.Idle
            _emailError.value = null
            _passwordError.value = null
            _resetEmailError.value = null
        }
    }

    fun resetUiState() {
        _uiState.value = AuthUiState.Idle
    }

    private fun validateEmail(email: String): Boolean {
        if (email.isBlank()) {
            _emailError.value = "Email не может быть пустым."
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailError.value = "Некорректный формат Email."
            return false
        }
        _emailError.value = null
        return true
    }

    private fun validatePassword(password: String): Boolean {
        if (password.isBlank()) {
            _passwordError.value = "Пароль не может быть пустым."
            return false
        }
        if (password.length < 6) {
            _passwordError.value = "Пароль должен быть не менее 6 символов."
            return false
        }
        _passwordError.value = null
        return true
    }

    private fun validateResetEmail(email: String): Boolean {
        if (email.isBlank()) {
            _resetEmailError.value = "Email не может быть пустым."
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _resetEmailError.value = "Некорректный формат Email."
            return false
        }
        _resetEmailError.value = null
        return true
    }

    fun login() {
        val isEmailValid = validateEmail(email.value)
        val isPasswordValid = validatePassword(password.value)

        if (!isEmailValid || !isPasswordValid) {
            _uiState.value = AuthUiState.Error("Пожалуйста, исправьте ошибки ввода.")
            return
        }

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email.value, password.value).await()
                UserRoleManager.fetchUserRole()
                _uiState.value = AuthUiState.Success("Вход выполнен успешно!")
                saveCredentials(email.value, password.value)
                Log.d(TAG, "signInWithEmail:success")
            } catch (e: Exception) {
                Log.e(TAG, "signInWithEmail:failure", e)
                _uiState.value = AuthUiState.Error(
                    when (e) {
                        is FirebaseAuthInvalidCredentialsException -> "Неверный Email или Пароль."
                        else -> "Ошибка входа: ${e.localizedMessage ?: "Неизвестная ошибка"}"
                    }
                )
            }
        }
    }

    fun register() {
        val isEmailValid = validateEmail(email.value)
        val isPasswordValid = validatePassword(password.value)

        if (!isEmailValid || !isPasswordValid) {
            _uiState.value = AuthUiState.Error("Пожалуйста, исправьте ошибки ввода.")
            return
        }

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email.value, password.value).await()
                val user = result.user
                if (user != null) {
                    FirebaseFirestore.getInstance().collection("users").document(user.uid).set(
                        mapOf("role" to "user")
                    ).await()
                }
                _uiState.value = AuthUiState.Success("Регистрация успешна! Теперь вы можете войти.")
                saveCredentials(email.value, password.value)
                Log.d(TAG, "createUserWithEmail:success")
            } catch (e: Exception) {
                Log.e(TAG, "createUserWithEmail:failure", e)
                _uiState.value = AuthUiState.Error(
                    when (e) {
                        is FirebaseAuthWeakPasswordException -> "Пароль слишком слабый."
                        is FirebaseAuthInvalidCredentialsException -> "Неверный формат Email."
                        is FirebaseAuthUserCollisionException -> "Пользователь с таким Email уже существует."
                        else -> "Ошибка регистрации: ${e.localizedMessage ?: "Неизвестная ошибка"}"
                    }
                )
            }
        }
    }

    fun sendPasswordResetEmail() {
        val isResetEmailValid = validateResetEmail(resetEmail.value)
        if (!isResetEmailValid) {
            _uiState.value = AuthUiState.Error("Пожалуйста, исправьте ошибку Email для сброса.")
            return
        }

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                auth.sendPasswordResetEmail(resetEmail.value).await()
                _uiState.value = AuthUiState.Success("Письмо для сброса пароля отправлено на ${resetEmail.value}.")
                Log.d(TAG, "Password reset email sent.")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending password reset email", e)
                _uiState.value = AuthUiState.Error("Ошибка отправки письма: ${e.localizedMessage ?: "Неизвестная ошибка"}")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                // Останавливаем TrackingService
                val intent = Intent(context, TrackingService::class.java).apply {
                    action = TrackingService.ACTION_STOP
                }
                context.stopService(intent)
                // Выход из Firebase Auth
                auth.signOut()
                // Очищаем сохраненные учетные данные
                val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                sharedPrefs.edit()
                    .remove(KEY_EMAIL)
                    .remove(KEY_PASSWORD)
                    .apply()
                // Очищаем поля ввода
                _email.value = ""
                _password.value = ""
                _resetEmail.value = ""
                // Обновляем роль пользователя
                UserRoleManager.fetchUserRole()
                _uiState.value = AuthUiState.Success("Выход выполнен успешно.")
                Log.d(TAG, "signOut:success")
            } catch (e: Exception) {
                Log.e(TAG, "signOut:failure", e)
                _uiState.value = AuthUiState.Error("Ошибка выхода: ${e.localizedMessage ?: "Неизвестная ошибка"}")
            }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
                return LoginViewModel(context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
