package com.example.firebasetest.concertListScreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.firebasetest.appNavigation.Routes
import com.example.firebasetest.auth.UserRole
import com.example.firebasetest.auth.UserRoleManager
import com.example.firebasetest.concert.Concert
import com.example.firebasetest.worktimetracker.WorkSessionRepository
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConcertListScreen(
    selectedDate: LocalDate,
    navController: NavController,
    viewModel: ConcertListViewModel = viewModel(factory = ConcertListViewModel.Factory(selectedDate))
) {
    val concerts by viewModel.concerts.observeAsState(emptyList())
    val isLoading by viewModel.isLoading.observeAsState(false)
    val errorMessage by viewModel.errorMessage.observeAsState(null)
    val context = LocalContext.current

    // Получаем роль пользователя
    val userRole by UserRoleManager.userRole.collectAsState()
    val isAdmin = userRole == UserRole.ADMIN

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Концерты на ${selectedDate}") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                )
            )
        },
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = {
                        navController.navigate(Routes.eventDetails(selectedDate))
                    }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Добавить концерт")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Статистика работы за выбранный день
            val workSessions = remember(selectedDate) {
                WorkSessionRepository.getWorkSessionsForDay(context, selectedDate)
            }

            if (workSessions.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Рабочее время за день:",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        workSessions.forEach { session ->
                            Text(
                                text = "${session.startTime.toLocalTime()} - ${session.endTime.toLocalTime()}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val totalDuration = workSessions.sumOf { it.durationMillis }
                        Text(
                            text = "Итого: ${WorkSessionRepository.formatDuration(totalDuration)}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Список концертов
            when {
                isLoading -> CircularProgressIndicator()
                errorMessage != null -> Text(
                    text = errorMessage ?: "Неизвестная ошибка",
                    color = MaterialTheme.colorScheme.error
                )
                concerts.isEmpty() -> Text(
                    text = if (isAdmin) "На эту дату концертов нет. Нажмите '+' чтобы добавить."
                    else "На эту дату концертов нет.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(concerts) { concert ->
                        ConcertListItem(concert = concert) {
                            navController.navigate(
                                Routes.eventDetails(
                                    selectedDate,
                                    concert.id
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConcertListItem(concert: Concert, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = concert.getConcertTypeEnum().displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Адрес: ${concert.address}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Время: ${concert.startTime}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}