package com.example.firebasetest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.firebasetest.auth.UserRoleManager
import com.example.firebasetest.ui.theme.FireBaseTestTheme
import com.example.firebasetest.appNavigation.AppNavigation
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Загружаем роль пользователя при запуске активности

        lifecycleScope.launch {
            UserRoleManager.fetchUserRole()
            println("MainActivity: Forced role fetch completed")
        }
        setContent {
            FireBaseTestTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}
