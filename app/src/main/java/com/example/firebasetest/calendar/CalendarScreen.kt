package com.example.firebasetest.calendar
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.firebasetest.appNavigation.Routes
import com.example.firebasetest.worktimetracker.CalendarViewModel
import com.example.firebasetest.concert.Concert
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    navController: NavController,
    viewModel: CalendarViewModel = viewModel(factory = CalendarViewModel.Factory)
) {
    val currentDisplayMonth by viewModel.currentDisplayMonth.collectAsState()
    val concertsForMonth by viewModel.concertsForMonth.collectAsState()

    var selectedDateForConfirmation by remember { mutableStateOf<LocalDate?>(null) }
    var showStopConfirmationDialog by remember { mutableStateOf(false) }

    val isTracking by viewModel.isTracking.collectAsState()
    val currentWorkDuration by viewModel.currentWorkDurationFormatted.collectAsState()
    val totalTimeToday by viewModel.totalTimeTodayFormatted.collectAsState()
    val totalTimeThisWeek by viewModel.totalTimeThisWeekFormatted.collectAsState()
    val totalTimeThisMonth by viewModel.totalTimeThisMonthFormatted.collectAsState()
    val totalTimeThisYear by viewModel.totalTimeThisYearFormatted.collectAsState()

    val context = LocalContext.current

    val foregroundServiceDataSyncPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            if (!isTracking) {
                viewModel.startTracking(context)
            }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                foregroundServiceDataSyncPermissionLauncher.launch(Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC)
            } else {
                if (!isTracking) {
                    viewModel.startTracking(context)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Календарь") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                MonthNavigationHeader(
                    currentMonth = currentDisplayMonth,
                    onPreviousMonthClick = { viewModel.setCurrentDisplayMonth(currentDisplayMonth.minusMonths(1)) },
                    onNextMonthClick = { viewModel.setCurrentDisplayMonth(currentDisplayMonth.plusMonths(1)) }
                )
            }

            item {
                DaysOfWeekHeader()
            }

            item {
                MonthGrid(
                    month = currentDisplayMonth,
                    onDayClick = { day -> selectedDateForConfirmation = day },
                    concerts = concertsForMonth.filter { (date, _) ->
                        date.year == currentDisplayMonth.year && date.month == currentDisplayMonth.month
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Текущее время работы:",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = currentWorkDuration,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Button(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else if (!isTracking) {
                                        viewModel.startTracking(context)
                                    }
                                },
                                enabled = !isTracking
                            ) {
                                Text("Начать работать")
                            }
                            Button(
                                onClick = { showStopConfirmationDialog = true },
                                enabled = isTracking,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Закончить работать")
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Статистика рабочего времени",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                        TimeStatRow(label = "Сегодня:", time = totalTimeToday)
                        TimeStatRow(label = "На этой неделе:", time = totalTimeThisWeek)
                        TimeStatRow(label = "В этом месяце:", time = totalTimeThisMonth)
                        TimeStatRow(label = "В этом году:", time = totalTimeThisYear)
                    }
                }
            }
        }

        selectedDateForConfirmation?.let { date ->
            ConfirmationDialog(
                date = date,
                onConfirm = { confirmedDate ->
                    navController.navigate(Routes.concertList(confirmedDate))
                    selectedDateForConfirmation = null
                },
                onDismiss = {
                    selectedDateForConfirmation = null
                }
            )
        }

        if (showStopConfirmationDialog) {
            AlertDialog(
                onDismissRequest = { showStopConfirmationDialog = false },
                title = { Text("Завершить работу?") },
                text = { Text("Вы уверены, что хотите завершить текущую рабочую сессию?") },
                confirmButton = {
                    Button(onClick = {
                        viewModel.stopTracking(context)
                        showStopConfirmationDialog = false
                    }) {
                        Text("Да, завершить")
                    }
                },
                dismissButton = {
                    Button(onClick = { showStopConfirmationDialog = false }) {
                        Text("Отмена")
                    }
                }
            )
        }
    }
}

@Composable
fun TimeStatRow(label: String, time: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Text(time, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MonthNavigationHeader(
    currentMonth: YearMonth,
    onPreviousMonthClick: () -> Unit,
    onNextMonthClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonthClick) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Предыдущий месяц")
        }
        Text(
            text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("ru"))),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        IconButton(onClick = onNextMonthClick) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Следующий месяц")
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DaysOfWeekHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        DayOfWeek.values().forEach { dayOfWeek ->
            Text(
                text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("ru")).take(2),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MonthGrid(
    month: YearMonth,
    onDayClick: (LocalDate) -> Unit,
    concerts: Map<LocalDate, List<Concert>>
) {
    val firstDayOfMonth = month.atDay(1)
    val firstDayOfWeek = (firstDayOfMonth.dayOfWeek.value - 1 + 7) % 7
    val daysInMonth = month.lengthOfMonth()

    val days = remember(month) {
        val leadingBlanks = firstDayOfWeek
        MutableList<LocalDate?>(leadingBlanks) { null }.apply {
            for (i in 1..daysInMonth) {
                add(month.atDay(i))
            }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 250.dp, max = 400.dp)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalArrangement = Arrangement.Center
    ) {
        items(days) { day ->
            DayCell(
                day = day,
                onDayClick = onDayClick,
                hasConcert = concerts.containsKey(day)
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DayCell(day: LocalDate?, onDayClick: (LocalDate) -> Unit, hasConcert: Boolean) {
    val isToday = day?.isEqual(LocalDate.now()) ?: false
    val backgroundColor = when {
        hasConcert -> MaterialTheme.colorScheme.tertiaryContainer
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }
    val textColor = when {
        hasConcert -> MaterialTheme.colorScheme.onTertiaryContainer
        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .padding(4.dp)
            .background(backgroundColor, MaterialTheme.shapes.medium)
            .clickable(enabled = day != null) { day?.let { onDayClick(it) } },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day?.dayOfMonth?.toString() ?: "",
                color = textColor,
                fontWeight = if (isToday || hasConcert) FontWeight.Bold else FontWeight.Normal,
                fontSize = 18.sp
            )
            if (hasConcert) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(MaterialTheme.colorScheme.tertiary, MaterialTheme.shapes.extraSmall)
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ConfirmationDialog(
    date: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Просмотреть события?") },
        text = { Text("Вы хотите просмотреть/добавить события для ${date.format(DateTimeFormatter.ofPattern("dd MMMM", Locale("ru")))}?") },
        confirmButton = {
            Button(onClick = { onConfirm(date) }) {
                Text("Да")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}



