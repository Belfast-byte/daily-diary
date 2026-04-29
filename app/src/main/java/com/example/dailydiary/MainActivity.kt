package com.example.dailydiary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.dailydiary.core.design.DailyDiaryTheme
import com.example.dailydiary.feature.shell.DailyDiaryApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DailyDiaryTheme {
                DailyDiaryApp()
            }
        }
    }
}
