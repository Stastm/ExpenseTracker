package com.example.expensetracker.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.expensetracker.ui.add.AddTransactionScreen
import com.example.expensetracker.ui.auth.AuthScreen
import com.example.expensetracker.ui.main.MainScreen

/** Маршруты верхнего уровня. HOME — это контейнер с нижней навигацией. */
object Route {
    const val AUTH = "auth"
    const val HOME = "home"
    const val ADD = "add"
}

@Composable
fun AppNavHost(
    startDestination: String,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        // По умолчанию экраны верхнего уровня плавно проявляются и гаснут
        enterTransition = { fadeIn(tween(300)) },
        exitTransition = { fadeOut(tween(200)) },
        popEnterTransition = { fadeIn(tween(300)) },
        popExitTransition = { fadeOut(tween(200)) }
    ) {

        composable(Route.AUTH) {
            AuthScreen(
                onAuthSuccess = {
                    // popUpTo очищает back stack, чтобы «Назад» не возвращал на логин
                    navController.navigate(Route.HOME) {
                        popUpTo(Route.AUTH) { inclusive = true }
                    }
                }
            )
        }

        composable(Route.HOME) {
            MainScreen(
                onAddClick = { navController.navigate(Route.ADD) },
                onLogout = {
                    onLogout()
                    navController.navigate(Route.AUTH) {
                        popUpTo(Route.HOME) { inclusive = true }
                    }
                }
            )
        }

        // Экран добавления выезжает снизу и уезжает обратно вниз
        composable(
            route = Route.ADD,
            enterTransition = {
                slideInVertically(tween(350)) { full -> full / 6 } + fadeIn(tween(300))
            },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = {
                slideOutVertically(tween(300)) { full -> full / 6 } + fadeOut(tween(250))
            }
        ) {
            AddTransactionScreen(onBack = { navController.popBackStack() })
        }
    }
}