package com.example.dailydiary.feature.stats

import androidx.compose.runtime.Composable
import com.example.dailydiary.feature.common.EmptyFeatureScreen

@Composable
fun StatsScreen() {
    EmptyFeatureScreen(
        title = "统计",
        subtitle = "这里将展示心情趋势和连续记录天数。"
    )
}
