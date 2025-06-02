package com.example.firebasetest.loginViewModel



import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "LoginViewModel"
private const val PREFS_NAME = "login_prefs"
private const val KEY_EMAIL = "saved_email"
private const val KEY_PASSWORD = "saved_password"

// Sealed class для представления различных состояний UI
sealed class AuthUiState {
    object Idle : AuthUiState() // Начальное состояние, ничего не происходит
    object Loading : AuthUiState() // Идет загрузка (вход, регистрация, сброс пароля)
    data class Success(val message: String) : AuthUiState() // Успех операции
    data class Error(val message: String) : AuthUiState() // Ошибка операции
}

class LoginViewModel(private val context: Context) : ViewModel() {

    // Внутреннее изменяемое состояние email и пароля
    private val _email = MutableStateFlow("")
    val email = _email.asStateFlow() // Открываем только для чтения

    private val _password = MutableStateFlow("")
    val password = _password.asStateFlow()

    private val _resetEmail = MutableStateFlow("")
    val resetEmail = _resetEmail.asStateFlow()

    // Состояние UI, которое будет наблюдать LoginScreen
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState = _uiState.asStateFlow()

    // Состояние для показа/скрытия формы сброса пароля
    private val _showForgotPassword = MutableStateFlow(false)
    val showForgotPassword = _showForgotPassword.asStateFlow()

    // Состояния для ошибок валидации
    private val _emailError = MutableStateFlow<String?>(null)
    val emailError = _emailError.asStateFlow()

    private val _passwordError = MutableStateFlow<String?>(null)
    val passwordError = _passwordError.asStateFlow()

    private val _resetEmailError = MutableStateFlow<String?>(null)
    val resetEmailError = _resetEmail.asStateFlow()

    // Firebase Auth инстанс
    private val auth = FirebaseAuth.getInstance()

    init {
        loadSavedCredentials()
    }

    // Load saved email and password from SharedPreferences
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

    // Save email and password to SharedPreferences
    private fun saveCredentials(email: String, password: String) {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .putString(KEY_EMAIL, email)
            .putString(KEY_PASSWORD, password)
            .apply()
    }

    // Обновление email в ViewModel
    fun onEmailChange(newEmail: String) {
        _email.value = newEmail
        // Очищаем ошибку при изменении текста
        _emailError.value = null
        // Если форма сброса пароля активна, очищаем ошибку сброса email
        if (showForgotPassword.value) {
            _resetEmailError.value = null
        }
    }

    // Обновление пароля в ViewModel
    fun onPasswordChange(newPassword: String) {
        _password.value = newPassword
        // Очищаем ошибку при изменении текста
        _passwordError.value = null
    }

    // Обновление email для сброса пароля
    fun onResetEmailChange(newEmail: String) {
        _resetEmail.value = newEmail
        // Очищаем ошибку при изменении текста
        _resetEmailError.value = null
    }

    // Переключение видимости формы сброса пароля
    fun toggleForgotPasswordForm(show: Boolean) {
        _showForgotPassword.value = show
        // Сброс сообщений при переключении формы
        if (!show) {
            _uiState.value = AuthUiState.Idle
            // Также очищаем ошибки валидации, если пользователь переключает форму
            _emailError.value = null
            _passwordError.value = null
            _resetEmailError.value = null
        }
    }

    // Сброс состояния UI (например, после показа ошибки)
    fun resetUiState() {
        _uiState.value = AuthUiState.Idle
    }

    // Функции валидации
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
        if (password.length < 6) { // Firebase по умолчанию требует минимум 6 символов
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

    // Логика входа
    fun login() {
        // Выполняем валидацию перед попыткой входа
        val isEmailValid = validateEmail(email.value)
        val isPasswordValid = validatePassword(password.value)

        if (!isEmailValid || !isPasswordValid) {
            _uiState.value = AuthUiState.Error("Пожалуйста, исправьте ошибки ввода.")
            return
        }

        _uiState.value = AuthUiState.Loading // Устанавливаем состояние загрузки
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email.value, password.value).await()
                _uiState.value = AuthUiState.Success("Вход выполнен успешно!")
                // Save credentials after successful login
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

    // Логика регистрации
    fun register() {
        // Выполняем валидацию перед попыткой регистрации
        val isEmailValid = validateEmail(email.value)
        val isPasswordValid = validatePassword(password.value)

        if (!isEmailValid || !isPasswordValid) {
            _uiState.value = AuthUiState.Error("Пожалуйста, исправьте ошибки ввода.")
            return
        }

        _uiState.value = AuthUiState.Loading // Устанавливаем состояние загрузки
        viewModelScope.launch {
            try {
                auth.createUserWithEmailAndPassword(email.value, password.value).await()
                _uiState.value = AuthUiState.Success("Регистрация успешна! Теперь вы можете войти.")
                // Save credentials after successful registration
                saveCredentials(email.value, password.value)
                Log.d(TAG, "createUserWithEmail:success")
            } catch (e: Exception) {
                Log.e(TAG, "createUserWithEmail:failure", e)
                _uiState.value = AuthUiState.Error(
                    when (e) {
                        is FirebaseAuthWeakPasswordException -> "Пароль слишком слабый. " + (_passwordError.value ?: "")
                        is FirebaseAuthInvalidCredentialsException -> "Неверный формат Email. " + (_emailError.value ?: "")
                        is FirebaseAuthUserCollisionException -> "Пользователь с таким Email уже существует."
                        else -> "Ошибка регистрации: ${e.localizedMessage ?: "Неизвестная ошибка"}"
                    }
                )
            }
        }
    }

    // Логика сброса пароля
    fun sendPasswordResetEmail() {
        // Выполняем валидацию перед отправкой письма
        val isResetEmailValid = validateResetEmail(resetEmail.value) // Валидируем resetEmail
        if (!isResetEmailValid) {
            _uiState.value = AuthUiState.Error("Пожалуйста, исправьте ошибку Email для сброса.")
            return
        }

        _uiState.value = AuthUiState.Loading // Устанавливаем состояние загрузки
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

    // Фабрика для создания LoginViewModel
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
                return LoginViewModel(context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

