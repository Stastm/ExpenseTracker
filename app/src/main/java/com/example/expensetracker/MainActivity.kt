package com.example.expensetracker

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.expensetracker.navigation.AppNavHost
import com.example.expensetracker.navigation.Route
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Edge-to-edge с тёмными системными панелями
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        val authRepository = (application as ExpenseApp).container.authRepository

        setContent {
            ExpenseTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // null = DataStore ещё читается, показываем пустой фон
                    val loggedIn by authRepository.isLoggedIn
                        .collectAsStateWithLifecycle<Boolean?>(initialValue = null)

                    loggedIn?.let { isLoggedIn ->
                        // Стартовый экран определяем один раз при запуске
                        val start = remember { if (isLoggedIn) Route.HOME else Route.AUTH }
                        AppNavHost(
                            startDestination = start,
                            onLogout = { lifecycleScope.launch { authRepository.logout() } }
                        )
                    }
                }
            }
        }
    }
}