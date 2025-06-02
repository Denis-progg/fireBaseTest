
package com.example.firebasetest.appNavigation
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

import java.time.LocalDate
import java.time.format.DateTimeParseException
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import com.example.firebasetest.calendar.CalendarScreen
import com.example.firebasetest.evenDetailScreen.EventDetailsScreen
import com.example.firebasetest.concertListScreen.ConcertListScreen // <<-- НОВЫЙ ИМПОРТ
import com.example.firebasetest.login.LoginScreen

// Определяем маршруты
object Routes {
    const val AUTH = "auth"
    const val CALENDAR = "calendar"
    // <<-- ИЗМЕНЕНИЕ МАРШРУТА: теперь он ведет на список концертов -->>
    const val CONCERT_LIST = "concert_list/{date}"
    fun concertList(date: LocalDate) = "concert_list/${date.toString()}"

    // <<-- НОВЫЙ МАРШРУТ для деталей концерта с опциональным ID -->>
    // {concertId}? - означает, что concertId необязателен.
    // Если его нет, то создаем новый концерт. Если есть, редактируем существующий.
    const val EVENT_DETAILS = "event_details/{date}/{concertId}"
    fun eventDetails(date: LocalDate, concertId: String? = null): String {
        return if (concertId != null) {
            "event_details/${date.toString()}/$concertId"
        } else {
            "event_details/${date.toString()}/null" // Используем "null" как заглушку для отсутствия ID
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.AUTH) {
        composable(Routes.AUTH) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.CALENDAR) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.CALENDAR) {
            CalendarScreen(navController = navController)
        }
        // <<-- НОВЫЙ COMPOSABLE для ConcertListScreen -->>
        composable(
            route = Routes.CONCERT_LIST,
            arguments = listOf(navArgument("date") { type = NavType.StringType })
        ) { backStackEntry ->
            val dateString = backStackEntry.arguments?.getString("date")
            val selectedDate = remember(dateString) {
                if (dateString != null) {
                    try {
                        LocalDate.parse(dateString)
                    } catch (e: DateTimeParseException) {
                        null
                    }
                } else {
                    null
                }
            }

            if (selectedDate != null) {
                ConcertListScreen(
                    selectedDate = selectedDate,
                    navController = navController
                )
            } else {
                Text("Ошибка: Дата не найдена или некорректна.")
            }
        }
        // <<-- ИЗМЕНЕННЫЙ COMPOSABLE для EventDetailsScreen -->>
        composable(
            route = Routes.EVENT_DETAILS,
            arguments = listOf(
                navArgument("date") { type = NavType.StringType },
                navArgument("concertId") {
                    type = NavType.StringType
                    nullable = true // Это очень важно! Позволяет передавать null
                    defaultValue = null // Если значение не передано, будет null
                }
            )
        ) { backStackEntry ->
            val dateString = backStackEntry.arguments?.getString("date")
            val concertId = backStackEntry.arguments?.getString("concertId").let {
                // Если передали "null" как строку, то это означает, что ID нет.
                if (it == "null") null else it
            }


            val selectedDate = remember(dateString) {
                if (dateString != null) {
                    try {
                        LocalDate.parse(dateString)
                    } catch (e: DateTimeParseException) {
                        null
                    }
                } else {
                    null
                }
            }

            if (selectedDate != null) {
                EventDetailsScreen(
                    selectedDate = selectedDate,
                    concertId = concertId, // <<-- ПЕРЕДАЕМ concertId
                    onBackClick = { navController.popBackStack() }
                )
            } else {
                Text("Ошибка: Дата не найдена или некорректна.")
            }
        }
    }
}
