
package com.example.firebasetest.eventDetailScreen

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.firebasetest.auth.UserRole
import com.example.firebasetest.auth.UserRoleManager
import com.example.firebasetest.concert.ConcertType
import com.example.firebasetest.eventDetailsviewModel.EventDetailsUiState
import com.example.firebasetest.eventDetailsviewModel.EventDetailsViewModel


import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EventDetailsScreen(
    selectedDate: LocalDate,
    onBackClick: () -> Unit,
    concertId: String? = null,
    viewModel: EventDetailsViewModel = viewModel(factory = EventDetailsViewModel.Factory(selectedDate, concertId))
) {
    val userRole by UserRoleManager.userRole.collectAsState()
    val isAdmin = userRole == UserRole.ADMIN

    val address by viewModel.address.collectAsState()
    val description by viewModel.description.collectAsState()
    val distance by viewModel.distance.collectAsState()
    val departureTime by viewModel.departureTime.collectAsState()
    val startTime by viewModel.startTime.collectAsState()
    val selectedConcertType by viewModel.selectedConcertType.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val members by viewModel.members.collectAsState()
    val showAddMemberDialog by viewModel.showAddMemberDialog.collectAsState()
    val newMemberName by viewModel.newMemberName.collectAsState()
    val busSeats by viewModel.busSeats.collectAsState()
    val driverName by viewModel.driverName.collectAsState()

    // Для выбора места: номер ряда (от 1 до 13) и индекс места в ряду (0-3)
    val selectedSeatRow by viewModel.selectedSeatRow.collectAsState()
    val selectedSeatIndexInRow by viewModel.selectedSeatIndexInRow.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }

    // Переменная для отображения диалога назначения места
    var showSeatAssignmentDialog by remember { mutableStateOf(false) }

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
            else -> {}
        }
    }

    // Запускаем диалог, когда место выбрано
    LaunchedEffect(selectedSeatRow, selectedSeatIndexInRow) {
        showSeatAssignmentDialog = (selectedSeatRow != null && selectedSeatIndexInRow != null)
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
                .verticalScroll(rememberScrollState())
        ) {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { if (isAdmin) expanded = it }
            ) {
                OutlinedTextField(
                    value = selectedConcertType.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Тип концерта") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .clickable(enabled = isAdmin) { expanded = !expanded },
                    enabled = isAdmin && uiState != EventDetailsUiState.Loading,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledContainerColor = MaterialTheme.colorScheme.surface,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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

            OutlinedTextField(
                value = address,
                onValueChange = { if (isAdmin) viewModel.onAddressChange(it) },
                label = { Text("Адрес концерта") },


            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = isAdmin && uiState != EventDetailsUiState.Loading,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { if (isAdmin) viewModel.onDescriptionChange(it) },
                label = { Text("Описание концерта") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                enabled = isAdmin && uiState != EventDetailsUiState.Loading,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

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
                enabled = isAdmin && uiState != EventDetailsUiState.Loading,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedTextField(
                    value = departureTime,
                    onValueChange = { if (isAdmin) viewModel.onDepartureTimeChange(it) },
                    label = { Text("Время выезда (ЧЧ:ММ)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    enabled = isAdmin && uiState != EventDetailsUiState.Loading,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledContainerColor = MaterialTheme.colorScheme.surface,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                )
                Spacer(modifier = Modifier.width(16.dp))

                OutlinedTextField(
                    value = startTime,
                    onValueChange = { if (isAdmin) viewModel.onStartTimeChange(it) },
                    label = { Text("Время начала (ЧЧ:ММ)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,


                enabled = isAdmin && uiState != EventDetailsUiState.Loading,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Состав на концерт",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                if (isAdmin) {
                    IconButton(
                        onClick = { viewModel.showAddMemberDialog() },
                        enabled = uiState != EventDetailsUiState.Loading
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Добавить участника")
                    }
                }
            }

            if (members.isEmpty()) {
                Text(
                    text = "Участники не добавлены",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp) // Ограничиваем высоту для скролла
                    ) {
                        itemsIndexed(members) { index, member ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(24.dp)
                                )
                                Text(
                                    text = member,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                                )

                                if (isAdmin) {
                                    IconButton(
                                        onClick = { viewModel.removeMember(index) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = "Удалить",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }


                            if (index < members.lastIndex) {
                                Divider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.DirectionsBus, contentDescription = "Автобус",
                    modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Схема автобуса",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Автобус 1",
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Place, contentDescription = "Направление",
                        modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = address,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        modifier = Modifier.widthIn(max = 200.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            BusLayout(
                busSeats = busSeats,
                driverName = driverName,
                isAdmin = isAdmin,
                onSeatClick = { row, indexInRow ->
                    if (isAdmin) {
                        viewModel.selectSeat(row, indexInRow)
                        // showSeatAssignmentDialog управляется LaunchedEffect
                    }
                },
                onDriverNameChange = { if (isAdmin) viewModel.onDriverNameChange(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                if (isAdmin) {
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
                    Text("Назад")
                }

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

        if (showAddMemberDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.hideAddMemberDialog() },
                title = { Text("Добавить участника") },
                text = {
                    Column {
                        Text("Введите ФИО участника:",
                            modifier = Modifier.padding(bottom = 8.dp))
                        OutlinedTextField(
                            value = newMemberName,
                            onValueChange = { viewModel.onNewMemberNameChange(it) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("Фамилия Имя Отчество") }
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.addMember()
                            viewModel.hideAddMemberDialog()
                        },
                        enabled = newMemberName.isNotBlank()
                    ) {
                        Text("Добавить")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.hideAddMemberDialog() }) {
                        Text("Отмена")
                    }
                }
            )
        }

        if (showSeatAssignmentDialog && selectedSeatRow != null && selectedSeatIndexInRow != null) {
            val namesInRow = busSeats[selectedSeatRow!!] ?: emptyList()
            val currentMember = namesInRow.getOrNull(selectedSeatIndexInRow!!)?.takeIf { it.isNotBlank() }

            SeatAssignmentDialog(
                row = selectedSeatRow!!,
                indexInRow = selectedSeatIndexInRow!!,
                currentMember = currentMember,
                members = members,
                onAssign = { memberName ->
                    viewModel.assignMemberToSeat(memberName)
                    viewModel.selectSeat(null, null) // Сбрасываем выбор
                },
                onClear = {
                    viewModel.clearSeat(selectedSeatRow!!, selectedSeatIndexInRow!!)
                    viewModel.selectSeat(null, null) // Сбрасываем выбор
                },
                onDismiss = {
                    viewModel.selectSeat(null, null) // Сбрасываем выбор при закрытии
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BusLayout(
    busSeats: Map<Int, List<String>>, // Теперь Map<Int, List<String>>
    driverName: String,
    isAdmin: Boolean,
    onSeatClick: (row: Int, indexInRow: Int) -> Unit, // Измененные параметры клика
    onDriverNameChange: (String) -> Unit
) {
    val totalRows = 13 // 13 рядов

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),


    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Водитель
            OutlinedTextField(
                value = driverName,
                onValueChange = onDriverNameChange,
                label = { Text("Водитель") },
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = "Водитель") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                singleLine = true,
                enabled = isAdmin
            )

            // Проход (один для всего автобуса)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Проход",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Ряды сидений
            // Нумерация мест: 0 - левое переднее, 1 - левое заднее, 2 - правое переднее, 3 - правое заднее
            for (row in 1..totalRows) {
                val namesInRow = busSeats[row] ?: emptyList()

                val leftFront = namesInRow.getOrNull(0) ?: ""
                val leftBack = namesInRow.getOrNull(1) ?: ""
                val rightFront = namesInRow.getOrNull(2) ?: ""
                val rightBack = namesInRow.getOrNull(3) ?: ""

                SeatRowItem(
                    rowNumber = row,
                    leftFrontPassenger = leftFront,
                    leftBackPassenger = leftBack,
                    rightFrontPassenger = rightFront,
                    rightBackPassenger = rightBack,
                    hasRightSeats = (row != 7), // Ряд 7 имеет только левые места
                    isAdmin = isAdmin,
                    onClick = onSeatClick,
                    onLongClick = onSeatClick // Долгое нажатие тоже вызывает выбор, чтобы предложить очистку в диалоге
                )
                if (row < totalRows) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

// Новый компонент для отрисовки одного ряда сидений (до 4-х мест)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SeatRowItem(
    rowNumber: Int,
    leftFrontPassenger: String,
    leftBackPassenger: String,
    rightFrontPassenger: String,
    rightBackPassenger: String,
    hasRightSeats: Boolean, // Определяет, есть ли места справа (для ряда 7)
    isAdmin: Boolean,
    onClick: (row: Int, indexInRow: Int) -> Unit,
    onLongClick: (row: Int, indexInRow: Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Номер ряда
        Text(
            text = "$rowNumber.",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(30.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))

        // Левые места
        Column(modifier = Modifier.weight(1f)) {
            SeatBox(
                passengerName = leftFrontPassenger,


            isAdmin = isAdmin,
            onClick = { onClick(rowNumber, 0) }, // Индекс 0: левое переднее
            onLongClick = { onLongClick(rowNumber, 0) }
            )
            Spacer(modifier = Modifier.height(4.dp))
            SeatBox(
                passengerName = leftBackPassenger,
                isAdmin = isAdmin,
                onClick = { onClick(rowNumber, 1) }, // Индекс 1: левое заднее
                onLongClick = { onLongClick(rowNumber, 1) }
            )
        }

        Spacer(modifier = Modifier.width(8.dp)) // Проход между левыми и правыми местами

        // Правые места или индикатор двери
        if (hasRightSeats) {
            Column(modifier = Modifier.weight(1f)) {
                SeatBox(
                    passengerName = rightFrontPassenger,
                    isAdmin = isAdmin,
                    onClick = { onClick(rowNumber, 2) }, // Индекс 2: правое переднее
                    onLongClick = { onLongClick(rowNumber, 2) }
                )
                Spacer(modifier = Modifier.height(4.dp))
                SeatBox(
                    passengerName = rightBackPassenger,
                    isAdmin = isAdmin,
                    onClick = { onClick(rowNumber, 3) }, // Индекс 3: правое заднее
                    onLongClick = { onLongClick(rowNumber, 3) }
                )
            }
        } else {
            // Если правых мест нет (для 7 ряда), показываем "Дверь/Проход"
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(130.dp) // Высота двух мест + отступ
                    .background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Дверь/\nПроход",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SeatBox(
    passengerName: String,
    isAdmin: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val backgroundColor = if (passengerName.isNotBlank()) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = if (passengerName.isNotBlank()) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(backgroundColor, RoundedCornerShape(4.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .combinedClickable(
                enabled = isAdmin,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (passengerName.isNotBlank()) {
                Text(
                    text = passengerName,
                    fontSize = 12.sp,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            } else {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = "Свободно",


                modifier = Modifier.size(24.dp),
                tint = textColor
                )
                Text(
                    text = "Свободно",
                    fontSize = 10.sp,
                    color = textColor
                )
            }
        }
    }
}

// Измененный SeatAssignmentDialog
@Composable
fun SeatAssignmentDialog(
    row: Int,
    indexInRow: Int,
    currentMember: String?,
    members: List<String>,
    onAssign: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val seatDescription = when (indexInRow) {
        0 -> "левое переднее"
        1 -> "левое заднее"
        2 -> "правое переднее"
        3 -> "правое заднее"
        else -> ""
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ряд $row, место $seatDescription") },
        text = {
            Column {
                if (currentMember != null && currentMember.isNotBlank()) {
                    Text("Текущий пассажир: $currentMember")
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text("Выберите участника:", modifier = Modifier.padding(bottom = 8.dp))

                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(members) { member ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAssign(member) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(member)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (currentMember != null && currentMember.isNotBlank()) {
                Button(
                    onClick = onClear,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Освободить место")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
//+1