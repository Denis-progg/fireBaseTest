
package com.example.firebasetest.concert

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.YearMonth

// Enum ConcertType уже был в этом файле, его содержание не меняется
// enum class ConcertType { ... }

class ConcertRepository(private val firestore: FirebaseFirestore) {

    private val concertsCollection = firestore.collection("concerts")

    suspend fun saveConcert(concert: Concert) {
        val concertData = mapOf(
            "date" to concert.date,
            "address" to concert.address,
            "description" to concert.description,
            "distanceKmFromVoronezh" to concert.distanceKmFromVoronezh,
            "departureTime" to concert.departureTime,
            "startTime" to concert.startTime,
            "concertType" to concert.concertType,
            "members" to concert.members,
            "busSeats" to concert.busSeats,
            "driverName" to concert.driverName // Сохраняем имя водителя
        )

        if (concert.id.isEmpty()) {
            concertsCollection.add(concertData).await()
        } else {
            concertsCollection.document(concert.id).set(concertData).await()
        }
    }

    suspend fun getConcertById(concertId: String): Concert? {
        return concertsCollection.document(concertId).get().await().toObject(Concert::class.java)?.copy(id = concertId)
    }

    fun getConcertsForDate(date: LocalDate): Flow<List<Concert>> {
        val concertsForDateFlow = MutableStateFlow<List<Concert>>(emptyList())
        val dateString = date.toString()

        concertsCollection
            .whereEqualTo("date", dateString)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    concertsForDateFlow.value = emptyList()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val concerts = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Concert::class.java)?.copy(id = doc.id)
                    }.sortedBy { it.getConcertTypeEnum().ordinal }
                    concertsForDateFlow.value = concerts
                } else {
                    concertsForDateFlow.value = emptyList()
                }
            }
        return concertsForDateFlow
    }

    fun getConcertsForMonth(yearMonth: YearMonth): Flow<Map<LocalDate, List<Concert>>> {
        return getConcertsForMonthRange(yearMonth, yearMonth)
    }

    fun getConcertsForMonthRange(startMonth: YearMonth, endMonth: YearMonth? = null): Flow<Map<LocalDate, List<Concert>>> {
        val concertsMapFlow = MutableStateFlow<Map<LocalDate, List<Concert>>>(emptyMap())

        val startDate = startMonth.atDay(1).toString()
        val query = if (endMonth != null) {
            val endDate = endMonth.atEndOfMonth().toString()
            concertsCollection
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
        } else {
            concertsCollection
                .whereGreaterThanOrEqualTo("date", startDate)
        }

        query.addSnapshotListener { snapshot, e ->
            if (e != null) {
                concertsMapFlow.value = emptyMap()
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val concerts = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Concert::class.java)?.copy(id = doc.id)
                }
                val map = concerts
                    .filter { it.getLocalDate() != null }


                .groupBy { it.getLocalDate()!! }
                    .mapValues { (_, value) -> value.sortedBy { it.getConcertTypeEnum().ordinal } }
                concertsMapFlow.value = map
            } else {
                concertsMapFlow.value = emptyMap()
            }
        }
        return concertsMapFlow
    }

    suspend fun deleteConcert(concertId: String) {
        concertsCollection.document(concertId).delete().await()
    }
}