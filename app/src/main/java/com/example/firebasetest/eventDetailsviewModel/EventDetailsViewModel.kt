package com.example.firebasetest.eventDetailsviewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.firebasetest.concert.Concert
import com.example.firebasetest.concert.ConcertRepository
import com.example.firebasetest.concert.ConcertType
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed class EventDetailsUiState {
    object Loading : EventDetailsUiState()
    data class Success(val message: String) : EventDetailsUiState()
    data class Error(val message: String) : EventDetailsUiState()
    object Idle : EventDetailsUiState()
}

class EventDetailsViewModel(
    private val concertRepository: ConcertRepository,
    private val selectedDate: LocalDate,
    private val concertId: String? = null
) : ViewModel() {

    private val _concert = MutableStateFlow<Concert?>(null)
    val concert: StateFlow<Concert?> = _concert.asStateFlow()

    private val _address = MutableStateFlow("")
    val address: StateFlow<String> = _address.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _distance = MutableStateFlow("0")
    val distance: StateFlow<String> = _distance.asStateFlow()

    private val _departureTime = MutableStateFlow("")
    val departureTime: StateFlow<String> = _departureTime.asStateFlow()

    private val _startTime = MutableStateFlow("")
    val startTime: StateFlow<String> = _startTime.asStateFlow()

    private val _selectedConcertType = MutableStateFlow(ConcertType.GENERAL)
    val selectedConcertType: StateFlow<ConcertType> = _selectedConcertType.asStateFlow()

    private val _uiState = MutableStateFlow<EventDetailsUiState>(EventDetailsUiState.Idle)
    val uiState: StateFlow<EventDetailsUiState> = _uiState.asStateFlow()

    init {
        concertId?.let { loadConcertById(it) }
    }

    private fun loadConcertById(id: String) {
        _uiState.value = EventDetailsUiState.Loading
        viewModelScope.launch {
            try {
                val existingConcert = concertRepository.getConcertById(id)
                if (existingConcert != null) {
                    _concert.value = existingConcert
                    _address.value = existingConcert.address
                    _description.value = existingConcert.description
                    _distance.value = existingConcert.distanceKmFromVoronezh.toString()
                    _departureTime.value = existingConcert.departureTime
                    _startTime.value = existingConcert.startTime
                    _selectedConcertType.value = existingConcert.getConcertTypeEnum()
                    _uiState.value = EventDetailsUiState.Success("Концерт загружен успешно.")
                } else {
                    _uiState.value = EventDetailsUiState.Error("Концерт не найден.")
                }
            } catch (e: Exception) {
                _uiState.value = EventDetailsUiState.Error("Ошибка загрузки: ${e.localizedMessage}")
            }
        }
    }

    fun onAddressChange(newAddress: String) { _address.value = newAddress }
    fun onDescriptionChange(newDescription: String) { _description.value = newDescription }
    fun onDistanceChange(newDistance: String) { _distance.value = newDistance }
    fun onDepartureTimeChange(newTime: String) { _departureTime.value = newTime }
    fun onStartTimeChange(newTime: String) { _startTime.value = newTime }
    fun onConcertTypeChange(newType: ConcertType) { _selectedConcertType.value = newType }

    fun saveConcert() {
        if (_address.value.isBlank() || _description.value.isBlank() ||
            _departureTime.value.isBlank() || _startTime.value.isBlank()) {
            _uiState.value = EventDetailsUiState.Error("Заполните все обязательные поля.")
            return
        }

        val distanceInt = _distance.value.toIntOrNull()
        if (distanceInt == null) {
            _uiState.value = EventDetailsUiState.Error("Расстояние должно быть числом.")
            return
        }

        if (!isValidTimeFormat(_departureTime.value) || !isValidTimeFormat(_startTime.value)) {
            _uiState.value = EventDetailsUiState.Error("Время должно быть в формате HH:MM.")
            return
        }

        _uiState.value = EventDetailsUiState.Loading
        viewModelScope.launch {
            try {
                val concertToSave = Concert(
                    id = concertId ?: "",
                    date = selectedDate.toString(),
                    address = _address.value,
                    description = _description.value,
                    distanceKmFromVoronezh = distanceInt,
                    departureTime = _departureTime.value,
                    startTime = _startTime.value,
                    concertType = _selectedConcertType.value.name
                )
                concertRepository.saveConcert(concertToSave)
                _uiState.value = EventDetailsUiState.Success("Концерт успешно сохранен.")
            } catch (e: Exception) {
                _uiState.value = EventDetailsUiState.Error("Ошибка сохранения: ${e.localizedMessage}")
            }
        }
    }

    fun deleteConcert() {
        concertId?.let {
            _uiState.value = EventDetailsUiState.Loading
            viewModelScope.launch {
                try {
                    concertRepository.deleteConcert(it)
                    _uiState.value = EventDetailsUiState.Success("Концерт успешно удален.")
                } catch (e: Exception) {
                    _uiState.value = EventDetailsUiState.Error("Ошибка удаления: ${e.localizedMessage}")
                }
            }
        } ?: run {
            _uiState.value = EventDetailsUiState.Error("ID концерта не найден.")
        }
    }

    private fun isValidTimeFormat(time: String): Boolean {
        val regex = Regex("""^\d{2}:\d{2}$""")
        if (!time.matches(regex)) return false
        val (hours, minutes) = time.split(":").map { it.toIntOrNull() ?: return false }
        return hours in 0..23 && minutes in 0..59
    }

    fun resetUiState() {
        _uiState.value = EventDetailsUiState.Idle
    }

    class Factory(private val selectedDate: LocalDate, private val concertId: String? = null) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            if (modelClass.isAssignableFrom(EventDetailsViewModel::class.java)) {
                val firestore = FirebaseFirestore.getInstance()
                val repository = ConcertRepository(firestore)
                return EventDetailsViewModel(repository, selectedDate, concertId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}





