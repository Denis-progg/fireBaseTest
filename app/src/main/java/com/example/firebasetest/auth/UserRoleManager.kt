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
        _isRoleLoading.value = true
        val user = auth.currentUser
        println("UserRoleManager: Current user = ${user?.uid ?: "null"}")
        if (user != null) {
            try {
                val doc = firestore.collection("users").document(user.uid).get().await()
                println("UserRoleManager: Document exists = ${doc.exists()}, data = ${doc.data}")
                val roleString = doc.getString("role") ?: "user"
                println("UserRoleManager: Role string = $roleString")
                _userRole.value = when (roleString) {
                    "admin" -> UserRole.ADMIN
                    else -> UserRole.USER
                }
                println("UserRoleManager: Role loaded as ${_userRole.value} for UID ${user.uid}")
            } catch (e: Exception) {
                println("UserRoleManager: Error loading role - ${e.message}")
                _userRole.value = UserRole.USER
            } finally {
                _isRoleLoading.value = false
            }
        } else {
            println("UserRoleManager: No authenticated user")
            _userRole.value = UserRole.UNKNOWN
            _isRoleLoading.value = false
        }
    }
}