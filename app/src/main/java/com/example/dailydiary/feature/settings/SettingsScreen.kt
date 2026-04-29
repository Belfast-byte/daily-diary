package com.example.dailydiary.feature.settings

import androidx.compose.runtime.Composable
import com.example.dailydiary.feature.common.EmptyFeatureScreen

@Composable
fun SettingsScreen() {
    EmptyFeatureScreen(
        title = "设置",
        subtitle = "这里将管理隐私锁、提醒和数据导出。"
    )
}
