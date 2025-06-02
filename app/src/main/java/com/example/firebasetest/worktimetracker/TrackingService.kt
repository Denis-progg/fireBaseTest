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
import android.util.Log // Важно: этот импорт должен быть
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
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

class TrackingService : Service() {

    private var isServiceRunning = false
    private var startTime: LocalDateTime? = null
    private lateinit var notificationManager: NotificationManager
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job()) // Coroutine scope for service
    private var timerJob: Job? = null

    companion object {
        private const val TAG = "TrackingService" // Ваш TAG для логов
        const val CHANNEL_ID = "WorkTimeTrackerChannel"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"

        // Хранение состояния сервиса
        private val _isTracking = MutableStateFlow(false)
        val isTracking: StateFlow<Boolean> = _isTracking

        // StateFlow для текущей продолжительности работы (сделано public в companion object)
        private val _currentWorkDurationMillis = MutableStateFlow(0L)
        val currentWorkDurationMillis: StateFlow<Long> = _currentWorkDurationMillis

        // Функция для получения сохраненного времени начала (сделана public в companion object)
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
        Log.d(TAG, "Service onCreate called.") // Лог в onCreate
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        // Восстанавливаем состояние при создании сервиса
        loadServiceState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called with action: ${intent?.action}") // Лог здесь
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_STOP -> stopTracking()
        }
        return START_STICKY // Если система убьет сервис, он будет перезапущен
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "onBind called.") // Лог в onBind
        return null // Мы не используем привязку к сервису
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service onDestroy called.") // Лог в onDestroy
        timerJob?.cancel() // Отменяем корутину таймера при уничтожении сервиса
        serviceScope.cancel() // Отменяем scope сервиса
    }

    private fun startTracking() {
        Log.d(TAG, "Attempting to start tracking...") // Лог здесь
        if (!isServiceRunning) {
            isServiceRunning = true
            _isTracking.value = true // Обновляем StateFlow в companion object
            startTime = LocalDateTime.now()
            saveStartTime(startTime!!)
            saveTrackingState(true)


            Log.d(TAG, "Calling startForeground for notification...") // Лог перед startForeground
            startForeground(NOTIFICATION_ID, createNotification("Работаем...").build())
            Log.d(TAG, "startForeground called successfully.") // Лог после startForeground

            startTimer()
            Log.d(TAG, "Tracking setup complete, timer should start.") // Лог после startTimer
        } else {
            Log.d(TAG, "Tracking already running, not restarting.") // Лог, если уже запущено
        }
    }

    private fun stopTracking() {
        Log.d(TAG, "Attempting to stop tracking...") // Лог в stopTracking
        if (isServiceRunning) {
            isServiceRunning = false
            _isTracking.value = false // Обновляем StateFlow в companion object
            timerJob?.cancel() // Отменяем таймер
            _currentWorkDurationMillis.value = 0L // Сбрасываем счетчик времени (обновляем StateFlow)

            val endTime = LocalDateTime.now()
            val savedStartTime = getSavedStartTimeInternal() // Используем внутренний метод для сервиса

            if (savedStartTime != null) {
                val durationMillis = ChronoUnit.MILLIS.between(savedStartTime, endTime)
                Log.d(TAG, "Saving work session: ${savedStartTime} to ${endTime}, duration: ${durationMillis}ms") // Лог перед сохранением
                WorkSessionRepository.saveWorkSession(this, WorkSession(savedStartTime, endTime, durationMillis))
            } else {
                Log.w(TAG, "Saved start time is null when stopping tracking. Work session not saved.") // Предупреждение
            }

            // Удаляем сохраненное время начала, так как сессия завершена
            clearStartTime()
            saveTrackingState(false)

            stopForeground(Service.STOP_FOREGROUND_REMOVE)
            stopSelf()
            Log.d(TAG, "Tracking stopped successfully.") // Лог после остановки
        } else {
            Log.d(TAG, "Tracking not running, no need to stop.") // Лог, если не запущено
        }
    }

    private fun startTimer() {
        Log.d(TAG, "startTimer called.") // Лог в startTimer
        timerJob?.cancel() // Отменяем предыдущий таймер, если есть
        startTime = getSavedStartTimeInternal() // Восстанавливаем startTime на случай перезапуска сервиса
        if (startTime == null) {
            Log.w(TAG, "startTime is null in startTimer. Stopping tracking.") // Предупреждение
            // Если startTime не найден (например, после перезагрузки телефона и до запуска app),
            // то логичнее считать, что сервис не запущен.
            // Или можно реализовать логику восстановления последней незавершенной сессии.
            // Для простоты пока остановим сервис, если нет startTime.
            stopTracking()
            return
        }

        timerJob = serviceScope.launch {
            Log.d(TAG, "Timer coroutine launched. isServiceRunning: $isServiceRunning") // Лог после запуска корутины
            while (isServiceRunning) {
                val currentDuration = ChronoUnit.MILLIS.between(startTime, LocalDateTime.now())
                _currentWorkDurationMillis.value = currentDuration // Обновляем StateFlow
                updateNotification(currentDuration) // Обновляем уведомление
                delay(1000) // Обновляем каждую секунду
                Log.v(TAG, "Timer tick: $currentDuration ms") // Детальный лог для таймера (VERBOSE)
            }
            Log.d(TAG, "Timer coroutine finished.") // Лог по завершении корутины
        }
    }

    @SuppressLint("NotificationPermission")
    private fun updateNotification(durationMillis: Long) {
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) % 60
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60
        val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)


        val timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds)
        // Log.v(TAG, "Updating notification with time: $timeString") // Можно включить для отладки частых обновлений
        notificationManager.notify(NOTIFICATION_ID, createNotification("Работаем: $timeString").build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "Creating notification channel.") // Лог при создании канала
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Отслеживание рабочего времени",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(contentText: String): NotificationCompat.Builder {
        Log.d(TAG, "Creating notification with content: '$contentText'") // Лог здесь
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
            .setSmallIcon(android.R.drawable.ic_menu_today) // Простая иконка
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Уведомление нельзя смахнуть
    }

    // --- Методы для работы с SharedPreferences ---
    private fun saveStartTime(time: LocalDateTime) {
        Log.d(TAG, "Saving start time: $time") // Лог сохранения
        val sharedPrefs = getSharedPreferences("work_tracker_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putLong("start_time_epoch", time.toEpochSecond(ZoneOffset.UTC)).apply()
    }

    // Внутренний метод для сервиса, чтобы не конфликтовать с getSavedStartTime в companion object
    private fun getSavedStartTimeInternal(): LocalDateTime? {
        val sharedPrefs = getSharedPreferences("work_tracker_prefs", Context.MODE_PRIVATE)
        val epochSeconds = sharedPrefs.getLong("start_time_epoch", -1L)
        val savedTime = if (epochSeconds != -1L) LocalDateTime.ofEpochSecond(epochSeconds, 0, ZoneOffset.UTC) else null
        Log.d(TAG, "Retrieved saved start time: $savedTime") // Лог получения
        return savedTime
    }

    private fun clearStartTime() {
        Log.d(TAG, "Clearing saved start time.") // Лог очистки
        val sharedPrefs = getSharedPreferences("work_tracker_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().remove("start_time_epoch").apply()
    }

    // Сохранение состояния запущено ли отслеживание
    private fun saveTrackingState(isTracking: Boolean) {
        Log.d(TAG, "Saving tracking state: $isTracking") // Лог сохранения состояния
        val sharedPrefs = getSharedPreferences("work_tracker_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean("is_tracking", isTracking).apply()
        // _isTracking.value = isTracking // Это уже делается в start/stopTracking, можно удалить дублирование, если уверены
    }

    private fun loadServiceState() {
        Log.d(TAG, "Loading service state...") // Лог загрузки состояния
        val sharedPrefs = getSharedPreferences("work_tracker_prefs", Context.MODE_PRIVATE)
        val trackingState = sharedPrefs.getBoolean("is_tracking", false)
        Log.d(TAG, "Loaded tracking state: $trackingState") // Лог загруженного состояния
        if (trackingState) {
            // Если сервис был запущен, пытаемся восстановить его состояние
            val savedStart = getSavedStartTimeInternal() // Используем внутренний метод


            if (savedStart != null) {
                isServiceRunning = true
                _isTracking.value = true // Обновляем StateFlow в companion object
                startTime = savedStart
                Log.d(TAG, "Restoring service state: tracking was active, starting foreground and timer.") // Лог восстановления
                startForeground(NOTIFICATION_ID, createNotification("Работаем...").build())
                startTimer() // Перезапускаем таймер
            } else {
                // Если состояние "is_tracking" true, но startTime потерялся, останавливаем
                Log.w(TAG, "Tracking state was active, but saved start time is null. Stopping service.") // Предупреждение
                saveTrackingState(false)
                stopSelf()
            }
        } else {
            Log.d(TAG, "Service not previously running (tracking state is false).") // Лог, если не было запущено
        }
    }
}



