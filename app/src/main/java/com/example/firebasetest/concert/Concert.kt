package com.example.firebasetest.concert

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.time.LocalDate
import java.time.LocalTime
import java.util.Date

data class Concert(
    @DocumentId
    val id: String = "",
    val date: String = "",
    val address: String = "",
    val description: String = "",
    val distanceKmFromVoronezh: Int = 0,
    val departureTime: String = "",
    val startTime: String = "",
    val concertType: String = ConcertType.UNKNOWN.name,
    val members: List<String> = emptyList(), // Новое поле для участников
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

    fun getConcertTypeEnum(): ConcertType {
        return try {
            ConcertType.valueOf(concertType)
        } catch (e: IllegalArgumentException) {
            ConcertType.UNKNOWN
        }
    }
}
//+ менял 1 раз контр z