package com.example.firebasetest.worktimetracker

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.firebasetest.concert.Concert
import com.example.firebasetest.concert.ConcertRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.util.concurrent.TimeUnit

class CalendarViewModel(application: Application, private val concertRepository: ConcertRepository) :
    AndroidViewModel(application) {

    private val context: Context = application.applicationContext

    private val _concertsForMonth = MutableStateFlow<Map<LocalDate, List<Concert>>>(emptyMap())
    val concertsForMonth: StateFlow<Map<LocalDate, List<Concert>>> = _concertsForMonth.asStateFlow()

    private val _currentDisplayMonth = MutableStateFlow(YearMonth.now())
    val currentDisplayMonth: StateFlow<YearMonth> = _currentDisplayMonth.asStateFlow()

    init {
        loadConcerts()
    }

    fun setCurrentDisplayMonth(yearMonth: YearMonth) {
        _currentDisplayMonth.value = yearMonth
    }

    val isTracking: StateFlow<Boolean> = TrackingService.isTracking
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val currentWorkDurationFormatted: StateFlow<String> = TrackingService.currentWorkDurationMillis
        .map { formatDuration(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), formatDuration(0L))

    val totalTimeTodayFormatted: StateFlow<String> =
        combine(isTracking, TrackingService.currentWorkDurationMillis) { isTracking, currentMillis ->
            val today = LocalDate.now()
            val baseTime = WorkSessionRepository.getTotalWorkTimeForDay(context, today)

            // Только для текущего дня добавляем активное время
            if (isTracking) {
                // Проверяем, что сессия началась сегодня
                val startTime = TrackingService.getSavedStartTime(context)
                if (startTime?.toLocalDate() == today) {
                    formatDuration(baseTime + currentMillis)
                } else {
                    formatDuration(baseTime)
                }
            } else {
                formatDuration(baseTime)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), formatDuration(0L))

    private fun loadConcerts() {
        viewModelScope.launch {
            val startMonth = YearMonth.now().minusYears(1)
            concertRepository.getConcertsForMonthRange(startMonth).collect {
                _concertsForMonth.value = it
            }
        }
    }

    fun startTracking(context: Context) {
        val intent = Intent(context, TrackingService::class.java).apply {
            action = TrackingService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopTracking(context: Context) {
        val intent = Intent(context, TrackingService::class.java).apply {
            action = TrackingService.ACTION_STOP
        }
        context.stopService(intent)
    }

    private fun formatDuration(durationMillis: Long): String {
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60
        val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                val firestore = FirebaseFirestore.getInstance()
                val concertRepository = ConcertRepository(firestore)
                return CalendarViewModel(application, concertRepository) as T
            }
        }
    }
}
