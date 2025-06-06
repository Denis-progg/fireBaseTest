package com.example.firebasetest.calendar

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AirportShuttle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CoPresent
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.firebasetest.appNavigation.Routes
import com.example.firebasetest.concert.Concert
import com.example.firebasetest.loginViewModel.LoginViewModel
import com.example.firebasetest.worktimetracker.*
import kotlinx.coroutines.launch
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
    viewModel: CalendarViewModel = viewModel(factory = CalendarViewModel.Factory),
    loginViewModel: LoginViewModel = viewModel(factory = LoginViewModel.Factory(LocalContext.current))
) {
    val context = LocalContext.current
    val concertsForMonth by viewModel.concertsForMonth.collectAsState()
    val currentDisplayMonth by viewModel.currentDisplayMonth.collectAsState()

    // Состояние для бокового меню
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SideMenuContent(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = 0.6f)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                onClose = { scope.launch { drawerState.close() } },
                onLogout = {
                    loginViewModel.logout()
                    navController.navigate(Routes.AUTH) {
                        popUpTo(0)
                    }
                }
            )
        },
        gesturesEnabled = true,
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Календарь") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Открыть меню")
                        }
                    },
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
                    // ИСПРАВЛЕННЫЙ КОМПОНЕНТ НАВИГАЦИИ ПО МЕСЯЦАМ
                    MonthNavigationHeader(
                        currentMonth = currentDisplayMonth,
                        onPreviousMonthClick = {
                            viewModel.setCurrentDisplayMonth(currentDisplayMonth.minusMonths(1))
                        },
                        onNextMonthClick = {
                            viewModel.setCurrentDisplayMonth(currentDisplayMonth.plusMonths(1))
                        }
                    )
                }

                item {
                    DaysOfWeekHeader()
                }

                item {
                    MonthGrid(
                        month = currentDisplayMonth,
                        onDayClick = { day ->
                            navController.navigate(Routes.concertList(day))
                        },
                        concerts = concertsForMonth.filter { (date, _) ->
                            date.year == currentDisplayMonth.year && date.month == currentDisplayMonth.month
                        }
                    )
                }
            }
        }
    }
}

// Компонент бокового меню
@Composable
fun SideMenuContent(
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(16.dp)
    ) {
        Text(
            text = "Меню",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Пункты меню
        MenuItem("Профиль", Icons.Default.Person) { /* Действие */ }
        MenuItem("Настройки", Icons.Default.Settings) {  /* Действие */}
        MenuItem("Статистика", Icons.Default.BarChart) {  /* Действие */ }
        MenuItem("Помощь", icon = Icons.Default.AirportShuttle) {  /* Действие */ }

        Spacer(modifier = Modifier.weight(1f))

        MenuItem("Выход", Icons.Default.ExitToApp) {
            onLogout()
            onClose()
        }
    }
}

@Composable
fun MenuItem(text: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge
        )
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
        itemsIndexed(days) { index, day ->
            DayCell(
                day = day,
                onDayClick = onDayClick,
                hasConcert = day?.let { concerts.containsKey(it) } ?: false
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DayCell(day: LocalDate?, onDayClick: (LocalDate) -> Unit, hasConcert: Boolean) {
    val context = LocalContext.current
    val workDuration = remember(day) {
        if (day != null) WorkSessionRepository.getTotalWorkTimeForDay(context, day) else 0L
    }

    val isToday = day?.isEqual(LocalDate.now()) ?: false

    // Цвет текста
    val textColor = when {
        workDuration > 0 -> MaterialTheme.colorScheme.onSecondaryContainer
        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    // Цвет кружка - желтый для текущего дня
    val circleColor = when {
        isToday -> Color.Yellow
        hasConcert -> MaterialTheme.colorScheme.tertiaryContainer
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .padding(4.dp)
            .clickable(enabled = day != null) {
                day?.let { onDayClick(it) }
            },
        contentAlignment = Alignment.Center
    ) {
        // Кружок вокруг числа
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(circleColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = day?.dayOfMonth?.toString() ?: "",
                color = textColor,
                fontWeight = if (isToday || hasConcert || workDuration > 0) FontWeight.Bold else FontWeight.Normal,
                fontSize = 18.sp
            )
        }

        // Время работы под кружком
        if (workDuration > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
            ) {
                Text(
                    text = WorkSessionRepository.formatShortDuration(workDuration),
                    fontSize = 8.sp,
                    color = textColor,
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MonthNavigationHeader(
    currentMonth: YearMonth,
    onPreviousMonthClick: () -> Unit,
    onNextMonthClick: () -> Unit
) {
    // ИЗМЕНЕНИЕ: ТОЛЬКО МЕСЯЦ И ГОД
    val monthName = remember(currentMonth) {
        currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("ru")))
    }

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
            text = monthName, // Формат "Июнь 2025"
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