package com.example.firebasetest.concert

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.YearMonth

class ConcertRepository(private val firestore: FirebaseFirestore) {

    private val concertsCollection = firestore.collection("concerts")

    // Сохраняет или обновляет концерт
    suspend fun saveConcert(concert: Concert) {
        if (concert.id.isEmpty()) {
            concertsCollection.add(concert).await()
        } else {
            concertsCollection.document(concert.id).set(concert).await()
        }
    }

    // Получает концерт по ID
    suspend fun getConcertById(concertId: String): Concert? {
        return concertsCollection.document(concertId).get().await().toObject(Concert::class.java)?.copy(id = concertId)
    }

    // Получает все концерты на определенную дату
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

    // Получает все концерты за указанный месяц
    fun getConcertsForMonth(yearMonth: YearMonth): Flow<Map<LocalDate, List<Concert>>> {
        return getConcertsForMonthRange(yearMonth, yearMonth)
    }

    // Получает концерты за диапазон месяцев или все будущие концерты
    fun getConcertsForMonthRange(startMonth: YearMonth, endMonth: YearMonth? = null): Flow<Map<LocalDate, List<Concert>>> {
        val concertsMapFlow = MutableStateFlow<Map<LocalDate, List<Concert>>>(emptyMap())

        val startDate = startMonth.atDay(1).toString()
        val query = if (endMonth != null) {
            val endDate = endMonth.atEndOfMonth().toString()
            concertsCollection
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
        } else {
            // Загружаем все концерты, начиная с startDate, без верхней границы
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

    // Удаляет концерт по ID
    suspend fun deleteConcert(concertId: String) {
        concertsCollection.document(concertId).delete().await()
    }
}
