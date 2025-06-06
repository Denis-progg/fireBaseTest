package com.example.firebasetest.worktimetracker

import java.time.LocalDateTime

data class WorkSession(
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val durationMillis: Long
)


