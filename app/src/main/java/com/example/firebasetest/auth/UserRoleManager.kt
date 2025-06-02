package com.example.firebasetest.auth
import android.annotation.SuppressLint
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

enum class UserRole {
    ADMIN, USER, UNKNOWN
}

object UserRoleManager {
    private val auth = FirebaseAuth.getInstance()
    @SuppressLint("StaticFieldLeak")
    private val firestore = FirebaseFirestore.getInstance()

    private val _userRole = MutableStateFlow(UserRole.UNKNOWN)
    val userRole: StateFlow<UserRole> = _userRole.asStateFlow()

    private val _isRoleLoading = MutableStateFlow(true)
    val isRoleLoading: StateFlow<Boolean> = _isRoleLoading.asStateFlow()

    suspend fun fetchUserRole() {
        _isRoleLoading.value = true // Устанавливаем состояние загрузки
        val user = auth.currentUser
        if (user != null) {
            try {
                val doc = firestore.collection("users").document(user.uid).get().await()
                val roleString = doc.getString("role") ?: "user"
                _userRole.value = when (roleString.lowercase()) {
                    "admin" -> UserRole.ADMIN
                    else -> UserRole.USER
                }
                println("UserRoleManager: Role loaded as ${_userRole.value} for UID ${user.uid}")
            } catch (e: Exception) {
                _userRole.value = UserRole.USER // По умолчанию — пользователь
                println("UserRoleManager: Error loading role - ${e.message}")
            } finally {
                _isRoleLoading.value = false // Завершаем состояние загрузки
            }
        } else {
            _userRole.value = UserRole.UNKNOWN
            _isRoleLoading.value = false
        }
    }
}
