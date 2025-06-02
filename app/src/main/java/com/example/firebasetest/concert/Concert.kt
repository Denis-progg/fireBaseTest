package com.example.firebasetest.concert

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.time.LocalDate
import java.time.LocalTime
import java.util.Date

data class Concert(
    @DocumentId
    val id: String = "",
    val date: String = "", // Дата концерта в формате "YYYY-MM-DD"
    val address: String = "", // Где концерт (адрес, название площадки)
    val description: String = "", // Описание концерта
    val distanceKmFromVoronezh: Int = 0, // Сколько км от Воронежа
    val departureTime: String = "", // Время выезда в формате "HH:MM"
    val startTime: String = "", // Время начала концерта в формате "HH:MM"
    val concertType: String = ConcertType.UNKNOWN.name, // <<-- НОВОЕ ПОЛЕ: тип концерта
    @ServerTimestamp
    val timestamp: Date? = null
) {
    fun getLocalDate(): LocalDate? {
        return try {
            LocalDate.parse(date)
        } catch (e: Exception) {
            null
        }
    }

    fun getLocalDepartureTime(): LocalTime? {
        return try {
            LocalTime.parse(departureTime)
        } catch (e: Exception) {
            null
        }
    }

    fun getLocalStartTime(): LocalTime? {
        return try {
            LocalTime.parse(startTime)
        } catch (e: Exception) {
            null
        }
    }

    // Вспомогательная функция для получения ConcertType
    fun getConcertTypeEnum(): ConcertType {
        return try {
            ConcertType.valueOf(concertType)
        } catch (e: IllegalArgumentException) {
            ConcertType.UNKNOWN
        }
    }
}
