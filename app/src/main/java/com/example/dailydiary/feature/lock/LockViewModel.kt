package com.example.dailydiary.feature.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailydiary.core.biometric.BiometricHelper
import com.example.dailydiary.core.datastore.AppSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LockUiState(
    val isLocked: Boolean = false,
    val lastUnlockTime: Long = 0,
    val errorMessage: String? = null
)

@HiltViewModel
class LockViewModel @Inject constructor(
    private val appSettings: AppSettings,
    private val biometricHelper: BiometricHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(LockUiState())
    val uiState: StateFlow<LockUiState> = _uiState.asStateFlow()

    private val LOCK_TIMEOUT_MS = 30_000L

    fun onAppBackground() {
        viewModelScope.launch {
            val enabled = appSettings.privacyLockEnabled.first()
            if (enabled) {
                _uiState.update { it.copy(isLocked = true) }
            }
        }
    }

    fun onAppForeground(activity: androidx.fragment.app.FragmentActivity) {
        viewModelScope.launch {
            val enabled = appSettings.privacyLockEnabled.first()
            if (!enabled) return@launch

            val state = _uiState.value
            val sinceLastUnlock = System.currentTimeMillis() - state.lastUnlockTime
            if (state.isLocked || sinceLastUnlock > LOCK_TIMEOUT_MS) {
                _uiState.update { it.copy(isLocked = true) }
                biometricHelper.authenticate(
                    activity = activity,
                    allowedDeviceCredential = true,
                    onSuccess = {
                        _uiState.update { it.copy(isLocked = false, lastUnlockTime = System.currentTimeMillis(), errorMessage = null) }
                    },
                    onError = { msg ->
                        _uiState.update { it.copy(errorMessage = msg) }
                    }
                )
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
