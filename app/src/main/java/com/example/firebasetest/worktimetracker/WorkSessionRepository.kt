package com.example.firebasetest.worktimetracker
import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.WeekFields
import java.util.*
import java.util.concurrent.TimeUnit

object WorkSessionRepository {
    private const val PREFS_NAME = "work_tracker_sessions"
    private const val KEY_SESSIONS = "work_sessions"
    private val gson = Gson()

    // Сохранение новой сессии
    fun saveWorkSession(context: Context, session: WorkSession) {
        val sessions = getWorkSessions(context).toMutableList()
        sessions.add(session)
        val json = gson.toJson(sessions)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SESSIONS, json)
            .apply()
    }

    // Получение всех сохраненных сессий
    fun getWorkSessions(context: Context): List<WorkSession> {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SESSIONS, "[]")
        val type = object : TypeToken<List<WorkSession>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    // --- Методы для подсчета статистики ---

    // Подсчет времени за определенный день
    fun getTotalWorkTimeForDay(context: Context, date: LocalDate): Long {
        return getWorkSessions(context)
            .filter { it.startTime.toLocalDate() == date }
            .sumOf { it.durationMillis }
    }

    // Подсчет времени за неделю (по ISO стандарту, неделя начинается с понедельника)
    fun getTotalWorkTimeForWeek(context: Context, dateInWeek: LocalDate): Long {
        val weekFields = WeekFields.of(Locale.getDefault()) // Зависит от локали, для ISO: Locale.ROOT
        val weekNumber = dateInWeek.get(weekFields.weekOfWeekBasedYear())
        val year = dateInWeek.get(weekFields.weekBasedYear())

        return getWorkSessions(context)
            .filter {
                it.startTime.toLocalDate().get(weekFields.weekOfWeekBasedYear()) == weekNumber &&
                        it.startTime.toLocalDate().get(weekFields.weekBasedYear()) == year
            }
            .sumOf { it.durationMillis }
    }

    // Подсчет времени за месяц
    fun getTotalWorkTimeForMonth(context: Context, dateInMonth: LocalDate): Long {
        return getWorkSessions(context)
            .filter { it.startTime.toLocalDate().withDayOfMonth(1).year == dateInMonth.year &&
                    it.startTime.toLocalDate().withDayOfMonth(1).month == dateInMonth.month }
            .sumOf { it.durationMillis }
    }

    // Подсчет времени за год
    fun getTotalWorkTimeForYear(context: Context, dateInYear: LocalDate): Long {
        return getWorkSessions(context)
            .filter { it.startTime.toLocalDate().year == dateInYear.year }
            .sumOf { it.durationMillis }
    }

    // Форматирование миллисекунд в HH:MM:SS
    fun formatDuration(durationMillis: Long): String {
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60
        val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}

