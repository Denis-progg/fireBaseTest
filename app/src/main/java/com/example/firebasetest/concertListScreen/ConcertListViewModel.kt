package com.example.firebasetest.concertListScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.firebasetest.concert.Concert
import com.example.firebasetest.concert.ConcertRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class ConcertListViewModel(
    private val concertRepository: ConcertRepository,
    private val selectedDate: LocalDate // Дата, для которой отображаем список концертов
) : ViewModel() {

    private val _concerts = MutableStateFlow<List<Concert>>(emptyList())
    val concerts: StateFlow<List<Concert>> = _concerts.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadConcerts()
    }

    private fun loadConcerts() {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                // Подписываемся на изменения в репозитории для выбранной даты
                concertRepository.getConcertsForDate(selectedDate).collect { concertList ->
                    _concerts.value = concertList
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка загрузки концертов: ${e.localizedMessage}"
                _isLoading.value = false
            }
        }
    }

    // ViewModel Factory для передачи зависимостей
    class Factory(private val selectedDate: LocalDate) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            if (modelClass.isAssignableFrom(ConcertListViewModel::class.java)) {
                val firestore = FirebaseFirestore.getInstance()
                val repository = ConcertRepository(firestore)
                return ConcertListViewModel(repository, selectedDate) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
