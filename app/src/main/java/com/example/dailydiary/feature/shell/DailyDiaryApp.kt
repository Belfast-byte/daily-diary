package com.example.dailydiary.feature.shell

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavHostController
import com.example.dailydiary.feature.calendar.CalendarScreen
import com.example.dailydiary.feature.history.HistoryScreen
import com.example.dailydiary.feature.settings.SettingsScreen
import com.example.dailydiary.feature.stats.StatsScreen
import com.example.dailydiary.feature.today.TodayScreen

@Composable
fun DailyDiaryApp() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { DiaryNavigationBar(navController) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Today.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(AppDestination.Today.route) { TodayScreen() }
            composable(AppDestination.Calendar.route) {
                CalendarScreen(
                    onDateClicked = { date ->
                        navController.navigate("day_detail/${date}")
                    }
                )
            }
            composable(AppDestination.History.route) {
                HistoryScreen(
                    onEntryClicked = { date ->
                        navController.navigate("day_detail/${date}")
                    }
                )
            }
            composable(AppDestination.Stats.route) { StatsScreen() }
            composable(AppDestination.Settings.route) { SettingsScreen() }
        }
    }
}

@Composable
private fun DiaryNavigationBar(navController: NavHostController) {
    val currentEntry = navController.currentBackStackEntryAsState()
    val currentDestination = currentEntry.value?.destination

    NavigationBar {
        AppDestination.entries.forEach { destination ->
            val selected = currentDestination
                ?.hierarchy
                ?.any { it.route == destination.route } == true
            DiaryNavigationItem(navController, destination, selected)
        }
    }
}

@Composable
private fun RowScope.DiaryNavigationItem(
    navController: NavHostController,
    destination: AppDestination,
    selected: Boolean
) {
    NavigationBarItem(
        selected = selected,
        onClick = { navigateToTopLevel(navController, destination) },
        icon = { Icon(destination.icon, contentDescription = destination.label) },
        label = { Text(destination.label) }
    )
}

private fun navigateToTopLevel(
    navController: NavHostController,
    destination: AppDestination
) {
    navController.navigate(destination.route) {
        launchSingleTop = true
        restoreState = true
        popUpTo(navController.graph.startDestinationId) {
            saveState = true
        }
    }
}
