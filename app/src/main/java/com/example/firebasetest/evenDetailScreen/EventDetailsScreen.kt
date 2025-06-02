package com.example.firebasetest.evenDetailScreen

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.firebasetest.concert.ConcertType
import com.example.firebasetest.eventDetailsviewModel.EventDetailsUiState
import com.example.firebasetest.eventDetailsviewModel.EventDetailsViewModel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.example.firebasetest.auth.UserRoleManager // НОВЫЙ ИМПОРТ
import com.example.firebasetest.auth.UserRole // НОВЫЙ ИМПОРТ

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailsScreen(
    selectedDate: LocalDate,
    onBackClick: () -> Unit,
    concertId: String? = null,
    viewModel: EventDetailsViewModel = viewModel(factory = EventDetailsViewModel.Factory(selectedDate, concertId))
) {
    // Наблюдаем за ролью пользователя
    val userRole by UserRoleManager.userRole.collectAsState()
    val isAdmin = userRole == UserRole.ADMIN

    // Наблюдаем за состояниями из ViewModel
    val address by viewModel.address.collectAsState()
    val description by viewModel.description.collectAsState()
    val distance by viewModel.distance.collectAsState()
    val departureTime by viewModel.departureTime.collectAsState()
    val startTime by viewModel.startTime.collectAsState()
    val selectedConcertType by viewModel.selectedConcertType.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }

    // Эффект для обработки состояний UI (успех/ошибка)
    LaunchedEffect(uiState) {
        when (uiState) {
            is EventDetailsUiState.Success -> {
                snackbarHostState.showSnackbar((uiState as EventDetailsUiState.Success).message)
                viewModel.resetUiState()
            }
            is EventDetailsUiState.Error -> {
                snackbarHostState.showSnackbar((uiState as EventDetailsUiState.Error).message)
                viewModel.resetUiState()
            }
            EventDetailsUiState.Loading -> { /* Покажем индикатор загрузки, если нужно */ }
            EventDetailsUiState.Idle -> { /* Ничего не делаем */ }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val formatter = DateTimeFormatter.ofPattern("d MMMM", Locale("ru"))
                    Text(if (concertId == null) "Добавить концерт на ${selectedDate.format(formatter)}" else "Редактировать концерт")
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
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
                .padding(16.dp)
                .imePadding()
        ) {
            // --- Выбор типа концерта ---
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { if (isAdmin) expanded = it } // Только ADMIN может открыть меню
            ) {
                OutlinedTextField(
                    value = selectedConcertType.displayName,
                    onValueChange = { /* Только для отображения */ },
                    readOnly = true,
                    label = { Text("Тип концерта") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .clickable(enabled = isAdmin) { expanded = !expanded }, // Только ADMIN может кликнуть
                    enabled = isAdmin && uiState != EventDetailsUiState.Loading
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    ConcertType.values().forEach { type ->
                        if (type != ConcertType.UNKNOWN) {
                            DropdownMenuItem(
                                text = { Text(type.displayName) },
                                onClick = {
                                    viewModel.onConcertTypeChange(type)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Поле для адреса концерта
            OutlinedTextField(
                value = address,
                onValueChange = { if (isAdmin) viewModel.onAddressChange(it) },
                label = { Text("Адрес концерта") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = isAdmin && uiState != EventDetailsUiState.Loading
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Поле для описания концерта
            OutlinedTextField(
                value = description,
                onValueChange = { if (isAdmin) viewModel.onDescriptionChange(it) },
                label = { Text("Описание концерта") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                enabled = isAdmin && uiState != EventDetailsUiState.Loading
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Поле для расстояния от Воронежа
            OutlinedTextField(
                value = distance,
                onValueChange = { newValue ->
                    if (isAdmin && (newValue.all { it.isDigit() } || newValue.isEmpty())) {
                        viewModel.onDistanceChange(newValue)
                    }
                },
                label = { Text("Расстояние от Воронежа (км)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = isAdmin && uiState != EventDetailsUiState.Loading
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Поле для времени выезда
                OutlinedTextField(
                    value = departureTime,
                    onValueChange = { if (isAdmin) viewModel.onDepartureTimeChange(it) },
                    label = { Text("Время выезда (ЧЧ:ММ)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    enabled = isAdmin && uiState != EventDetailsUiState.Loading
                )
                Spacer(modifier = Modifier.width(16.dp))

                // Поле для времени начала концерта
                OutlinedTextField(
                    value = startTime,
                    onValueChange = { if (isAdmin) viewModel.onStartTimeChange(it) },
                    label = { Text("Время начала (ЧЧ:ММ)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    enabled = isAdmin && uiState != EventDetailsUiState.Loading
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Кнопки Сохранить, Отменить и Удалить
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                if (isAdmin) { // Кнопка "Сохранить" только для ADMIN
                    Button(
                        onClick = { viewModel.saveConcert() },
                        enabled = uiState != EventDetailsUiState.Loading
                    ) {
                        Text(if (uiState == EventDetailsUiState.Loading) "Сохранение..." else "Сохранить")
                    }
                }
                OutlinedButton(
                    onClick = onBackClick,
                    enabled = uiState != EventDetailsUiState.Loading
                ) {
                    Text("Назад") // Переименовано с "Отменить" на "Назад", так как для USER это просто возврат
                }

                // Кнопка "Удалить" (только для ADMIN и если редактируем существующий концерт)
                if (isAdmin && concertId != null) {
                    Button(
                        onClick = { showDeleteConfirmationDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        enabled = uiState != EventDetailsUiState.Loading
                    ) {
                        Text("Удалить")
                    }
                }
            }
        }

        // Диалоговое окно подтверждения удаления (только для ADMIN)
        if (showDeleteConfirmationDialog && isAdmin) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmationDialog = false },
                title = { Text("Подтвердите удаление") },
                text = { Text("Вы уверены, что хотите удалить этот концерт? Это действие необратимо.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteConcert()
                            showDeleteConfirmationDialog = false
                        }
                    ) {
                        Text("Удалить")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmationDialog = false }) {
                        Text("Отмена")
                    }
                }
            )
        }
    }
}
