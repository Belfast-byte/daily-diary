package com.example.dailydiary

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.dailydiary.core.design.DailyDiaryTheme
import com.example.dailydiary.feature.lock.LockScreen
import com.example.dailydiary.feature.lock.LockViewModel
import com.example.dailydiary.feature.shell.DailyDiaryApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val lockViewModel: LockViewModel by viewModels()

    private var isResumed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DailyDiaryTheme {
                val lockState by lockViewModel.uiState.collectAsState()

                Box(modifier = Modifier.fillMaxSize()) {
                    DailyDiaryApp()
                    if (lockState.isLocked) {
                        LockScreen(errorMessage = lockState.errorMessage)
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        lockViewModel.onAppBackground()
    }

    override fun onResume() {
        super.onResume()
        if (isResumed) {
            lockViewModel.onAppForeground(this)
        }
        isResumed = true
    }
}
