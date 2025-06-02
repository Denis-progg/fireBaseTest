package com.example.firebasetest.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.firebasetest.loginViewModel.AuthUiState
import com.example.firebasetest.loginViewModel.LoginViewModel

@OptIn(ExperimentalMaterial3Api::class)
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
    var showRegisterForm by remember { mutableStateOf(false) } // State for showing registration form

    val snackbarHostState = remember { SnackbarHostState() }

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
                title = { Text("Вход") }, // Simplified title since registration is now separate
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.toggleForgotPasswordForm(false) // Ensure forgot password is hidden
                            viewModel.onResetEmailChange("") // Clear reset email
                            viewModel.onEmailChange("") // Clear email
                            viewModel.onPasswordChange("") // Clear password
                            showRegisterForm = true // Show registration form
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Registration form with slide-in animation
            AnimatedVisibility(
                visible = showRegisterForm,
                enter = slideInVertically(initialOffsetY = { -it }), // Slide in from top
                exit = slideOutVertically(targetOffsetY = { -it }) // Slide out to top
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Регистрация нового пользователя",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { viewModel.onEmailChange(it) },
                        label = { Text("Email") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState != AuthUiState.Loading
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { viewModel.onPasswordChange(it) },
                        label = { Text("Пароль") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState != AuthUiState.Loading
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.register() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState != AuthUiState.Loading
                    ) {
                        Text(if (uiState == AuthUiState.Loading) "Регистрация..." else "Зарегистрироваться")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(
                        onClick = { showRegisterForm = false }
                    ) {
                        Text("Вернуться к входу")
                    }
                }
            }

            // Forgot Password form
            AnimatedVisibility(
                visible = showForgotPassword && !showRegisterForm,
                enter = slideInVertically(initialOffsetY = { -it }),
                exit = slideOutVertically(targetOffsetY = { -it })
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Введите Email для сброса пароля",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { viewModel.onResetEmailChange(it) },
                        label = { Text("Ваш Email") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState != AuthUiState.Loading
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.sendPasswordResetEmail() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState != AuthUiState.Loading
                    ) {
                        Text(if (uiState == AuthUiState.Loading) "Отправка..." else "Сбросить пароль")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(
                        onClick = { viewModel.toggleForgotPasswordForm(false) }
                    ) {
                        Text("Вернуться к входу")
                    }
                }
            }

            // Login form
            AnimatedVisibility(
                visible = !showForgotPassword && !showRegisterForm,
                enter = slideInVertically(initialOffsetY = { -it }),
                exit = slideOutVertically(targetOffsetY = { -it })
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { viewModel.onEmailChange(it) },
                        label = { Text("Email") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState != AuthUiState.Loading
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { viewModel.onPasswordChange(it) },
                        label = { Text("Пароль") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState != AuthUiState.Loading
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.login() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState != AuthUiState.Loading
                    ) {
                        Text(if (uiState == AuthUiState.Loading) "Загрузка..." else "Войти")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            viewModel.toggleForgotPasswordForm(true)
                            viewModel.onResetEmailChange(email)
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Забыли пароль?")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewLoginScreen() {
    MaterialTheme {
        LoginScreen(onLoginSuccess = {})
    }
}
