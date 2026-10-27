package com.example.expensetracker.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.expensetracker.ExpenseApp
import com.example.expensetracker.ui.history.HistoryScreen
import com.example.expensetracker.ui.home.HomeScreen
import com.example.expensetracker.ui.profile.ProfileScreen
import com.example.expensetracker.ui.stats.StatsScreen
import com.example.expensetracker.ui.stats.StatsViewModel

/** Вкладки нижней панели. */
private enum class Tab(val route: String, val title: String, val icon: ImageVector) {
    HOME("tab_home", "Главная", Icons.Filled.Home),
    HISTORY("tab_history", "История", Icons.Filled.History),
    STATS("tab_stats", "Статистика", Icons.Filled.PieChart),
    PROFILE("tab_profile", "Профиль", Icons.Filled.Person)
}

private const val TAB_ANIM_MS = 420

private fun tabIndex(route: String?): Int = Tab.entries.indexOfFirst { it.route == route }

/** 1: переход на вкладку правее, -1: левее. */
private fun direction(from: String?, to: String?): Int =
    if (tabIndex(to) >= tabIndex(from)) 1 else -1

/** Новый экран выезжает с нужной стороны на треть ширины и проявляется. */
private fun tabEnter(dir: Int): EnterTransition =
    slideInHorizontally(
        animationSpec = tween(durationMillis = TAB_ANIM_MS, easing = FastOutSlowInEasing)
    ) { full -> full / 3 * dir } +
            fadeIn(tween(durationMillis = TAB_ANIM_MS, delayMillis = 60, easing = FastOutSlowInEasing))

/** Старый экран уезжает в противоположную сторону и гаснет. */
private fun tabExit(dir: Int): ExitTransition =
    slideOutHorizontally(
        animationSpec = tween(durationMillis = TAB_ANIM_MS, easing = FastOutSlowInEasing)
    ) { full -> -full / 3 * dir } +
            fadeOut(tween(durationMillis = 220, easing = FastOutSlowInEasing))

/**
 * Контейнер с нижней навигацией. У него свой NavController для вкладок,
 * а полноэкранные страницы (добавление операции) открываются выше, в AppNavHost.
 */
@Composable
fun MainScreen(
    onAddClick: () -> Unit,
    onLogout: () -> Unit
) {
    val tabsNav = rememberNavController()
    val backStack by tabsNav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    // Переход на вкладку с сохранением состояния предыдущих
    val goToTab: (Tab) -> Unit = { tab ->
        tabsNav.navigate(tab.route) {
            popUpTo(tabsNav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 0.dp
            ) {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = { goToTab(tab) },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title, style = MaterialTheme.typography.labelMedium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            // Кнопка «Добавить» плавно появляется только на главной вкладке
            AnimatedVisibility(
                visible = currentRoute == Tab.HOME.route,
                enter = scaleIn(tween(300, easing = FastOutSlowInEasing)) +
                        fadeIn(tween(300)),
                exit = scaleOut(tween(200)) + fadeOut(tween(150))
            ) {
                ExtendedFloatingActionButton(
                    onClick = onAddClick,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Добавить", style = MaterialTheme.typography.labelLarge) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = MaterialTheme.shapes.extraLarge
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = tabsNav,
            startDestination = Tab.HOME.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                tabEnter(direction(initialState.destination.route, targetState.destination.route))
            },
            exitTransition = {
                tabExit(direction(initialState.destination.route, targetState.destination.route))
            },
            popEnterTransition = {
                tabEnter(direction(initialState.destination.route, targetState.destination.route))
            },
            popExitTransition = {
                tabExit(direction(initialState.destination.route, targetState.destination.route))
            }
        ) {
            composable(Tab.HOME.route) {
                HomeScreen(
                    onSeeAll = { goToTab(Tab.HISTORY) },
                    onOpenStats = { goToTab(Tab.STATS) }
                )
            }
            composable(Tab.HISTORY.route) {
                HistoryScreen()
            }
            composable(Tab.STATS.route) {
                val app = LocalContext.current.applicationContext as ExpenseApp
                val statsViewModel: StatsViewModel = viewModel(
                    factory = StatsViewModel.factory(app.container.transactionRepository)
                )
                StatsScreen(
                    viewModel = statsViewModel,
                    onBack = { goToTab(Tab.HOME) }
                )
            }
            composable(Tab.PROFILE.route) {
                ProfileScreen(onLogout = onLogout)
            }
        }
    }
}