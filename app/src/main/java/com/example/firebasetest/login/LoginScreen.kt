package com.example.firebasetest.login

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.firebasetest.loginViewModel.AuthUiState
import com.example.firebasetest.loginViewModel.LoginViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = viewModel(factory = LoginViewModel.Factory(LocalContext.current))
) {
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val resetEmail by viewModel.resetEmail.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val showForgotPassword by viewModel.showForgotPassword.collectAsState()
    var showRegisterForm by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Анимационные параметры
    val animationDuration = 400
    val slideDirection = -200 // Направление сдвига (сверху вниз)

    // Handle UI state changes and show Snackbar
    LaunchedEffect(uiState) {
        when (uiState) {
            is AuthUiState.Success -> {
                snackbarHostState.showSnackbar((uiState as AuthUiState.Success).message)
                if (uiState is AuthUiState.Success && (uiState as AuthUiState.Success).message.contains("Вход выполнен успешно")) {
                    onLoginSuccess()
                }
                viewModel.resetUiState()
            }
            is AuthUiState.Error -> {
                snackbarHostState.showSnackbar((uiState as AuthUiState.Error).message)
                viewModel.resetUiState()
            }
            AuthUiState.Idle -> { /* Do nothing */ }
            AuthUiState.Loading -> { /* Handled by button enabled state */ }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Вход") },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.toggleForgotPasswordForm(false)
                            viewModel.onResetEmailChange("")
                            viewModel.onEmailChange("")
                            viewModel.onPasswordChange("")
                            showRegisterForm = true
                        }
                    ) {
                        Text(
                            text = "Регистрация",
                            style = MaterialTheme.typography.bodyLarge,
                            textDecoration = TextDecoration.Underline
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Анимированный контейнер для форм
            AnimatedContent(
                targetState = Triple(showRegisterForm, showForgotPassword, uiState),
                transitionSpec = {
                    // Комбинированная анимация: сдвиг + затухание
                    (slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Up,
                        animationSpec = tween(animationDuration)
                    ) + fadeIn(
                        animationSpec = tween(animationDuration)
                    )).togetherWith(
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Down,
                            animationSpec = tween(animationDuration)
                        ) + fadeOut(animationSpec = tween(animationDuration)))
                },
                label = "form_transition"
            ) { (register, forgot, state) ->
                when {
                    register -> {
                        // Форма регистрации с анимацией
                        AuthForm(
                            title = "Регистрация",
                            fields = listOf(
                                InputField("Email", email, Icons.Default.Email, KeyboardType.Email) { viewModel.onEmailChange(it) },
                                InputField("Пароль", password, Icons.Default.Lock, KeyboardType.Password) { viewModel.onPasswordChange(it) }
                            ),
                            buttonText = "Зарегистрироваться",
                            loading = state == AuthUiState.Loading,
                            onSubmit = { viewModel.register() },
                            secondaryAction = {
                                TextButton(onClick = { showRegisterForm = false }) {
                                    Text("Вернуться к входу")
                                }
                            }
                        )
                    }

                    forgot -> {
                        // Форма сброса пароля с анимацией
                        AuthForm(
                            title = "Сброс пароля",
                            fields = listOf(
                                InputField("Ваш Email", resetEmail, Icons.Default.Email, KeyboardType.Email) { viewModel.onResetEmailChange(it) }
                            ),
                            buttonText = "Сбросить пароль",
                            loading = state == AuthUiState.Loading,
                            onSubmit = { viewModel.sendPasswordResetEmail() },
                            secondaryAction = {
                                TextButton(onClick = { viewModel.toggleForgotPasswordForm(false) }) {
                                    Text("Вернуться к входу")
                                }
                            }
                        )
                    }

                    else -> {
                        // Форма входа с анимацией
                        AuthForm(
                            title = "Вход в систему",
                            fields = listOf(
                                InputField("Email", email, Icons.Default.Email, KeyboardType.Email) { viewModel.onEmailChange(it) },
                                InputField("Пароль", password, Icons.Default.Lock, KeyboardType.Password) { viewModel.onPasswordChange(it) }
                            ),
                            buttonText = "Войти",
                            loading = state == AuthUiState.Loading,
                            onSubmit = { viewModel.login() },
                            secondaryAction = {
                                TextButton(
                                    onClick = {
                                        viewModel.toggleForgotPasswordForm(true)
                                        viewModel.onResetEmailChange(email)
                                    },
                                    modifier = Modifier.align(Alignment.Center)
                                ) {
                                    Text("Забыли пароль?")
                                }
                            }
                        )
                    }
                }
            }

            // Анимация загрузки поверх всего
            if (uiState == AuthUiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(64.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 6.dp
                )
            }
        }
    }
}

// Модель для поля ввода
data class InputField(
    val label: String,
    val value: String,
    val icon: ImageVector,
    val keyboardType: KeyboardType,
    val onValueChange: (String) -> Unit
)

// Универсальный компонент формы
@Composable
fun AuthForm(
    title: String,
    fields: List<InputField>,
    buttonText: String,
    loading: Boolean,
    onSubmit: () -> Unit,
    secondaryAction: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Анимация появления заголовка
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(500)) + slideInVertically(
                initialOffsetY = { -it / 2 },
                animationSpec = tween(500)
            ),
            exit = fadeOut() + slideOutVertically()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }

        // Анимация появления полей ввода
        fields.forEachIndexed { index, field ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(delayMillis = 100 * index, durationMillis = 500)) +
                        slideInVertically(
                            initialOffsetY = { 50 },
                            animationSpec = tween(delayMillis = 100 * index, durationMillis = 500)
                        ),
                exit = fadeOut() + slideOutVertically()
            ) {
                OutlinedTextField(
                    value = field.value,
                    onValueChange = field.onValueChange,
                    label = { Text(field.label) },
                    leadingIcon = { Icon(field.icon, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = field.keyboardType),
                    visualTransformation = if (field.keyboardType == KeyboardType.Password)
                        PasswordVisualTransformation()
                    else
                        androidx.compose.ui.text.input.VisualTransformation.None,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    enabled = !loading
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Анимация кнопки
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(delayMillis = 300, durationMillis = 500)) +
                    scaleIn(animationSpec = tween(500)),
            exit = fadeOut() + scaleOut()
        ) {
            Button(
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading
            ) {
                if (loading) {
                    // Анимированный индикатор загрузки
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(buttonText)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Анимация вторичного действия
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(delayMillis = 500, durationMillis = 500)),
            exit = fadeOut()
        ) {
            secondaryAction()
        }
    }
}

