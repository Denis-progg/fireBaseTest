package com.example.firebasetest.worktimetracker

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.util.concurrent.TimeUnit

object WorkSessionRepository {
    private const val PREFS_NAME = "work_tracker_sessions"
    private const val KEY_SESSIONS = "work_sessions"
    private val gson = Gson()

    // Сохранение новой сессии
    fun saveWorkSession(context: Context, session: WorkSession) {
        val sessions = getWorkSessions(context).toMutableList()
        sessions.add(session)
        saveSessions(context, sessions)
    }

    // Получение всех сохраненных сессий
    fun getWorkSessions(context: Context): List<WorkSession> {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = sharedPrefs.getString(KEY_SESSIONS, "[]") ?: "[]"
        val type = object : TypeToken<List<WorkSession>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    // Получение сессий за конкретную дату
    fun getWorkSessionsForDay(context: Context, date: LocalDate): List<WorkSession> {
        return getWorkSessions(context).filter {
            it.startTime.toLocalDate() == date
        }
    }

    // Подсчет времени за определенный день
    fun getTotalWorkTimeForDay(context: Context, date: LocalDate): Long {
        return getWorkSessionsForDay(context, date)
            .sumOf { it.durationMillis }
    }

    // Форматирование времени в ЧЧ:ММ:СС
    fun formatDuration(durationMillis: Long): String {
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60
        val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    // Форматирование времени в ЧЧ ч ММ мин
    fun formatShortDuration(durationMillis: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60
        val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)
        return "${hours}ч ${minutes}мин"
    }

    private fun saveSessions(context: Context, sessions: List<WorkSession>) {
        val json = gson.toJson(sessions)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SESSIONS, json)
            .apply()
    }
}


