package com.example.dailydiary.feature.shell

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    Today("today", "今日", Icons.Filled.EditNote),
    Calendar("calendar", "日历", Icons.Filled.CalendarMonth),
    History("history", "历史", Icons.Filled.History),
    Stats("stats", "统计", Icons.Filled.ShowChart),
    Settings("settings", "设置", Icons.Filled.Settings)
}
