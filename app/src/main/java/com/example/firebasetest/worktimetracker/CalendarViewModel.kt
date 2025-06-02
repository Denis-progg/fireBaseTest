package com.example.firebasetest.worktimetracker

import android.annotation.SuppressLint
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.util.concurrent.TimeUnit

class CalendarViewModel(application: Application, private val concertRepository: ConcertRepository) : AndroidViewModel(application) {

    @SuppressLint("StaticFieldLeak")
    private val context: Context = application.applicationContext

    // StateFlow для хранения концертов за все месяцы
    private val _concertsForMonth = MutableStateFlow<Map<LocalDate, List<Concert>>>(emptyMap())
    val concertsForMonth: StateFlow<Map<LocalDate, List<Concert>>> = _concertsForMonth.asStateFlow()

    // StateFlow для текущего отображаемого месяца в календаре
    private val _currentDisplayMonth = MutableStateFlow(YearMonth.now())
    val currentDisplayMonth: StateFlow<YearMonth> = _currentDisplayMonth.asStateFlow()

    init {
        // Загружаем концерты начиная с года назад от текущего месяца
        viewModelScope.launch {
            val startMonth = YearMonth.now().minusYears(1) // Начинаем с прошлого года
            concertRepository.getConcertsForMonthRange(startMonth).collect { concertsMap ->
                _concertsForMonth.value = concertsMap
            }
        }
    }

    fun setCurrentDisplayMonth(yearMonth: YearMonth) {
        _currentDisplayMonth.value = yearMonth
    }

    // Состояние, указывающее, активно ли отслеживание
    val isTracking: StateFlow<Boolean> = TrackingService.isTracking
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    // Форматированное текущее время работы
    val currentWorkDurationFormatted: StateFlow<String> = TrackingService.currentWorkDurationMillis
        .map { millis -> formatDuration(millis) }
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = formatDuration(0L)
        )

    // Статистика за сегодня
    val totalTimeTodayFormatted: StateFlow<String> =
        TrackingService.isTracking.combine(TrackingService.currentWorkDurationMillis) { isTrackingValue, currentDurationMillis ->
            val today = LocalDate.now()
            val totalMillis = WorkSessionRepository.getTotalWorkTimeForDay(context, today)
            if (isTrackingValue && TrackingService.getSavedStartTime(context)?.toLocalDate() == today) {
                formatDuration(totalMillis + currentDurationMillis)
            } else {
                formatDuration(totalMillis)
            }
        }.stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = formatDuration(0L)
        )

    // Статистика за текущую неделю
    val totalTimeThisWeekFormatted: StateFlow<String> =
        TrackingService.isTracking.combine(TrackingService.currentWorkDurationMillis) { isTrackingValue, currentDurationMillis ->
            val today = LocalDate.now()
            val totalMillis = WorkSessionRepository.getTotalWorkTimeForWeek(context, today)
            if (isTrackingValue && TrackingService.getSavedStartTime(context)?.toLocalDate() == today) {
                formatDuration(totalMillis + currentDurationMillis)
            } else {
                formatDuration(totalMillis)
            }
        }.stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = formatDuration(0L)
        )

    // Статистика за текущий месяц
    val totalTimeThisMonthFormatted: StateFlow<String> =
        TrackingService.isTracking.combine(TrackingService.currentWorkDurationMillis) { isTrackingValue, currentDurationMillis ->
            val today = LocalDate.now()
            val totalMillis = WorkSessionRepository.getTotalWorkTimeForMonth(context, today)
            if (isTrackingValue && TrackingService.getSavedStartTime(context)?.toLocalDate() == today) {
                formatDuration(totalMillis + currentDurationMillis)
            } else {
                formatDuration(totalMillis)
            }
        }.stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = formatDuration(0L)
        )

    // Статистика за текущий год
    val totalTimeThisYearFormatted: StateFlow<String> =
        TrackingService.isTracking.combine(TrackingService.currentWorkDurationMillis) { isTrackingValue, currentDurationMillis ->
            val today = LocalDate.now()
            val totalMillis = WorkSessionRepository.getTotalWorkTimeForYear(context, today)
            if (isTrackingValue && TrackingService.getSavedStartTime(context)?.toLocalDate() == today) {
                formatDuration(totalMillis + currentDurationMillis)
            } else {
                formatDuration(totalMillis)
            }
        }.stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = formatDuration(0L)
        )

    @SuppressLint("ObsoleteSdkInt")
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

    // Вспомогательная функция для форматирования времени
    @SuppressLint("DefaultLocale")
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
                val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                requireNotNull(application) { "Application context must be available for CalendarViewModel" }
                val firestore = FirebaseFirestore.getInstance()
                val concertRepository = ConcertRepository(firestore)
                return CalendarViewModel(application as Application, concertRepository) as T
            }
        }
    }
}

