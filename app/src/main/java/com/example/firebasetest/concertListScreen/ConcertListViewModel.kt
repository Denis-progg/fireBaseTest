package com.example.firebasetest.concertListScreen

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.firebasetest.concert.Concert
import com.example.firebasetest.concert.ConcertRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.time.LocalDate

class ConcertListViewModel(
    private val concertRepository: ConcertRepository,
    private val selectedDate: LocalDate
) : ViewModel() {

    private val _concerts = MutableLiveData<List<Concert>>()
    val concerts: LiveData<List<Concert>> = _concerts

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        loadConcerts()
    }

    private fun loadConcerts() {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
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