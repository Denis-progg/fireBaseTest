package com.example.firebasetest.worktimetracker

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.firebasetest.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class TrackingService : Service() {

    private var isServiceRunning = false
    private lateinit var notificationManager: NotificationManager
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var timerJob: Job? = null

    companion object {
        private const val TAG = "TrackingService"
        const val CHANNEL_ID = "WorkTimeTrackerChannel"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"

        private val _isTracking = MutableStateFlow(false)
        val isTracking: StateFlow<Boolean> = _isTracking

        private val _currentWorkDurationMillis = MutableStateFlow(0L)
        val currentWorkDurationMillis: StateFlow<Long> = _currentWorkDurationMillis

        fun getSavedStartTime(context: Context): LocalDateTime? {
            val sharedPrefs = context.getSharedPreferences("work_tracker_prefs", Context.MODE_PRIVATE)
            val epochSeconds = sharedPrefs.getLong("start_time_epoch", -1L)
            return if (epochSeconds != -1L) {
                LocalDateTime.ofEpochSecond(epochSeconds, 0, ZoneOffset.UTC)
            } else {
                null
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        loadServiceState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_STOP -> stopTracking()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        serviceScope.cancel()
    }

    private fun startTracking() {
        if (!isServiceRunning) {
            isServiceRunning = true
            _isTracking.value = true
            val startTime = LocalDateTime.now()
            saveStartTime(startTime)
            saveTrackingState(true)

            // Сбрасываем текущее время при начале новой сессии
            _currentWorkDurationMillis.value = 0L

            startForeground(NOTIFICATION_ID, createNotification("Работаем...").build())
            startTimer(startTime)
        }
    }

    private fun stopTracking() {
        if (isServiceRunning) {
            isServiceRunning = false
            _isTracking.value = false
            timerJob?.cancel()

            val endTime = LocalDateTime.now()
            val savedStartTime = getSavedStartTimeInternal()

            if (savedStartTime != null) {
                val durationMillis = ChronoUnit.MILLIS.between(savedStartTime, endTime)
                WorkSessionRepository.saveWorkSession(this, WorkSession(savedStartTime, endTime, durationMillis))
            }

            // Сбрасываем текущее время после остановки
            _currentWorkDurationMillis.value = 0L

            clearStartTime()
            saveTrackingState(false)
            stopForeground(Service.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun startTimer(startTime: LocalDateTime) {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isServiceRunning) {
                val currentDuration = ChronoUnit.MILLIS.between(startTime, LocalDateTime.now())
                _currentWorkDurationMillis.value = currentDuration
                updateNotification(currentDuration)
                delay(1000)
            }
        }
    }

    @SuppressLint("NotificationPermission")
    private fun updateNotification(durationMillis: Long) {
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60
        val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)

        val timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds)
        notificationManager.notify(NOTIFICATION_ID, createNotification("Работаем: $timeString").build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Отслеживание рабочего времени",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(contentText: String): NotificationCompat.Builder {
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Отслеживание работы")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
    }

    private fun saveStartTime(time: LocalDateTime) {
        val sharedPrefs = getSharedPreferences("work_tracker_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putLong("start_time_epoch", time.toEpochSecond(ZoneOffset.UTC)).apply()
    }

    private fun getSavedStartTimeInternal(): LocalDateTime? {
        val sharedPrefs = getSharedPreferences("work_tracker_prefs", Context.MODE_PRIVATE)
        val epochSeconds = sharedPrefs.getLong("start_time_epoch", -1L)
        return if (epochSeconds != -1L) {
            LocalDateTime.ofEpochSecond(epochSeconds, 0, ZoneOffset.UTC)
        } else {
            null
        }
    }

    private fun clearStartTime() {
        val sharedPrefs = getSharedPreferences("work_tracker_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().remove("start_time_epoch").apply()
    }

    private fun saveTrackingState(isTracking: Boolean) {
        val sharedPrefs = getSharedPreferences("work_tracker_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("is_tracking", isTracking).apply()
    }

    private fun loadServiceState() {
        val sharedPrefs = getSharedPreferences("work_tracker_prefs", Context.MODE_PRIVATE)
        val trackingState = sharedPrefs.getBoolean("is_tracking", false)

        if (trackingState) {
            val savedStart = getSavedStartTimeInternal()
            if (savedStart != null) {
                // Проверяем, что сессия началась сегодня
                if (savedStart.toLocalDate() == LocalDate.now()) {
                    isServiceRunning = true
                    _isTracking.value = true
                    startForeground(NOTIFICATION_ID, createNotification("Работаем...").build())
                    startTimer(savedStart)
                } else {
                    // Если сессия началась не сегодня, сбрасываем
                    saveTrackingState(false)
                    clearStartTime()
                }
            } else {
                saveTrackingState(false)
            }
        }
    }
}