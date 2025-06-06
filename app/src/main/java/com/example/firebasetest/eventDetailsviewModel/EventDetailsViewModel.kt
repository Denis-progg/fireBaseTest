

// package com.example.firebasetest.eventDetailsviewModel
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

    private val _members = MutableStateFlow<List<String>>(emptyList())
    val members: StateFlow<List<String>> = _members.asStateFlow()

    private val _showAddMemberDialog = MutableStateFlow(false)
    val showAddMemberDialog: StateFlow<Boolean> = _showAddMemberDialog.asStateFlow()

    private val _newMemberName = MutableStateFlow("")
    val newMemberName: StateFlow<String> = _newMemberName.asStateFlow()

    // Изменено на Map<Int, List<String>>
    private val _busSeats = MutableStateFlow<Map<Int, List<String>>>(emptyMap())
    val busSeats: StateFlow<Map<Int, List<String>>> = _busSeats.asStateFlow()

    private val _driverName = MutableStateFlow("")
    val driverName: StateFlow<String> = _driverName.asStateFlow()

    // Для выбора места: номер ряда (от 1 до 13) и позиция в ряду (0-3)
    private val _selectedSeatRow = MutableStateFlow<Int?>(null) // Номер ряда
    val selectedSeatRow: StateFlow<Int?> = _selectedSeatRow.asStateFlow()

    private val _selectedSeatIndexInRow = MutableStateFlow<Int?>(null) // Индекс места в ряду (0, 1, 2, 3)
    val selectedSeatIndexInRow: StateFlow<Int?> = _selectedSeatIndexInRow.asStateFlow()


    init {
        concertId?.let { loadConcertById(it) }
    }

    private fun loadConcertById(id: String) {
        _uiState.value = EventDetailsUiState.Loading
        viewModelScope.launch {
            try {
                val existingConcert = concertRepository.getConcertById(id)
                if (existingConcert != null) {
                    _address.value = existingConcert.address
                    _description.value = existingConcert.description
                    _distance.value = existingConcert.distanceKmFromVoronezh.toString()
                    _departureTime.value = existingConcert.departureTime


                    _startTime.value = existingConcert.startTime
                    _selectedConcertType.value = existingConcert.getConcertTypeEnum()
                    _members.value = existingConcert.members
                    _busSeats.value = existingConcert.busSeats
                    _driverName.value = existingConcert.driverName
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
    fun onNewMemberNameChange(name: String) { _newMemberName.value = name }
    fun onDriverNameChange(name: String) { _driverName.value = name }

    fun showAddMemberDialog() {
        _showAddMemberDialog.value = true
    }

    fun hideAddMemberDialog() {
        _showAddMemberDialog.value = false
        _newMemberName.value = ""
    }

    fun addMember() {
        if (_newMemberName.value.isNotBlank()) {
            _members.value = _members.value + _newMemberName.value
            _newMemberName.value = ""
        }
    }

    fun removeMember(index: Int) {
        val removedMember = _members.value[index]
        _members.value = _members.value.toMutableList().apply { removeAt(index) }

        // Также нужно проверить, не сидит ли этот участник в автобусе, и очистить его место
        val newBusSeats = _busSeats.value.toMutableMap()
        newBusSeats.forEach { (row, namesInRow) ->
            val newNamesInRow = namesInRow.toMutableList()
            var changed = false
            for (i in newNamesInRow.indices) {
                if (newNamesInRow[i] == removedMember) {
                    newNamesInRow[i] = ""
                    changed = true
                }
            }
            if (changed) {
                newBusSeats[row] = newNamesInRow
            }
        }
        _busSeats.value = newBusSeats
    }

    fun selectSeat(row: Int?, indexInRow: Int?) {
        _selectedSeatRow.value = row
        _selectedSeatIndexInRow.value = indexInRow
    }

    fun assignMemberToSeat(memberName: String) {
        _selectedSeatRow.value?.let { selectedRow ->
            _selectedSeatIndexInRow.value?.let { selectedIndex ->
                val newBusSeats = _busSeats.value.toMutableMap()

                // Сначала удаляем участника с любого другого места
                newBusSeats.forEach { (row, namesInRow) ->
                    val newNamesInRow = namesInRow.toMutableList()
                    var changed = false
                    for (i in newNamesInRow.indices) {
                        if (newNamesInRow[i] == memberName) {
                            newNamesInRow[i] = ""
                            changed = true
                        }
                    }
                    if (changed) {
                        newBusSeats[row] = newNamesInRow
                    }
                }

                // Теперь назначаем участника на выбранное место
                val currentNamesInRow = newBusSeats[selectedRow]?.toMutableList() ?: mutableListOf("", "", "", "")
                // Убедимся, что список достаточно большой
                while (currentNamesInRow.size <= selectedIndex) {
                    currentNamesInRow.add("")
                }
                currentNamesInRow[selectedIndex] = memberName


                newBusSeats[selectedRow] = currentNamesInRow
                _busSeats.value = newBusSeats
            }
        }
        _selectedSeatRow.value = null
        _selectedSeatIndexInRow.value = null
    }

    fun clearSeat(row: Int, indexInRow: Int) {
        val newBusSeats = _busSeats.value.toMutableMap()
        val currentNamesInRow = newBusSeats[row]?.toMutableList() ?: mutableListOf("", "", "", "")

        if (indexInRow < currentNamesInRow.size) {
            currentNamesInRow[indexInRow] = ""
        }

        // Если все места в ряду пусты, можно удалить этот ряд из карты
        // Но лучше оставить, чтобы не терять структуру ряда (особенно для 7-го ряда)
        // if (currentNamesInRow.all { it.isBlank() }) {
        //    newBusSeats.remove(row)
        // } else {
        newBusSeats[row] = currentNamesInRow
        // }
        _busSeats.value = newBusSeats
    }

    fun saveConcert() {
        if (_address.value.isBlank() || _description.value.isBlank()
            || _departureTime.value.isBlank() || _startTime.value.isBlank()) {
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
                    concertType = _selectedConcertType.value.name,
                    members = _members.value,
                    busSeats = _busSeats.value,
                    driverName = _driverName.value
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

//+



