package com.example.firebasetest.concertListScreen

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.firebasetest.appNavigation.Routes
import com.example.firebasetest.concert.Concert
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import com.example.firebasetest.auth.UserRoleManager
import com.example.firebasetest.auth.UserRole

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConcertListScreen(
    selectedDate: LocalDate,
    navController: NavController,
    viewModel: ConcertListViewModel = viewModel(factory = ConcertListViewModel.Factory(selectedDate))
) {
    // Наблюдаем за ролью пользователя и состоянием загрузки
    val userRole by UserRoleManager.userRole.collectAsState()
    val isRoleLoading by UserRoleManager.isRoleLoading.collectAsState()
    val isAdmin = userRole == UserRole.ADMIN

    val concerts by viewModel.concerts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru"))
                    Text("Концерты на ${selectedDate.format(formatter)}")
                },
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
            if (!isRoleLoading && isAdmin) { // Показываем FAB только после загрузки роли
                FloatingActionButton(onClick = {
                    navController.navigate(Routes.eventDetails(selectedDate))
                }) {
                    Icon(Icons.Filled.Add, "Добавить концерт")
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
            when {
                isRoleLoading -> { // Показываем индикатор загрузки роли
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    Text(
                        text = "Загрузка данных пользователя...",
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                errorMessage != null -> {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                concerts.isEmpty() -> {
                    Text(
                        text = if (isAdmin) "На эту дату концертов нет. Нажмите '+' чтобы добавить."
                        else "На эту дату концертов нет.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
                else -> {
                    LazyColumn(
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

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun ConcertListScreenPreview() {
    MaterialTheme {
        ConcertListScreen(
            selectedDate = LocalDate.now(),
            navController = rememberNavController()
        )
    }
}

