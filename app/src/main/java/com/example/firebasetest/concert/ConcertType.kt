package com.example.firebasetest.concert
enum class ConcertType(val displayName: String) {
    GENERAL("Общий концерт"),
    BRIGADE_1("Концерт 1 бригады"),
    BRIGADE_2("Концерт 2 бригады"),
    UNKNOWN("Неизвестный тип") // Добавим для обработки возможных некорректных данных
}
