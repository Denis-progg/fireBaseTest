package com.example.firebasetest.worktimetracker

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.ZoneOffset

// Используем data class для удобства
data class WorkSession(
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val durationMillis: Long // Продолжительность в миллисекундах
) {
    // Для сериализации/десериализации в SharedPreferences
    fun toJsonString(): String {
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        return "${startTime.format(formatter)}|${endTime.format(formatter)}|$durationMillis"
    }

    companion object {
        fun fromJsonString(jsonString: String): WorkSession? {
            val parts = jsonString.split("|")
            if (parts.size == 3) {
                try {
                    val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                    val startTime = LocalDateTime.parse(parts[0], formatter)
                    val endTime = LocalDateTime.parse(parts[1], formatter)
                    val duration = parts[2].toLong()
                    return WorkSession(startTime, endTime, duration)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return null
        }
    }
}

