package com.example.dailydiary.feature.settings

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.dailydiary.core.datastore.AppSettings
import com.example.dailydiary.core.export.DiaryExporter
import com.example.dailydiary.core.notification.ReminderWorker
import com.example.dailydiary.domain.model.DiaryEntry
import com.example.dailydiary.domain.repository.DiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class SettingsUiState(
    val privacyLockEnabled: Boolean = true,
    val reminderEnabled: Boolean = false,
    val reminderTime: String = "21:00",
    val isExporting: Boolean = false,
    val exportError: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appSettings: AppSettings,
    private val repository: DiaryRepository,
    private val exporter: DiaryExporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val workManager = WorkManager.getInstance(context)

    init {
        viewModelScope.launch {
            appSettings.privacyLockEnabled.collect { enabled ->
                _uiState.update { it.copy(privacyLockEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            appSettings.reminderEnabled.collect { enabled ->
                _uiState.update { it.copy(reminderEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            appSettings.reminderTime.collect { time ->
                _uiState.update { it.copy(reminderTime = time) }
            }
        }
    }

    fun setPrivacyLockEnabled(enabled: Boolean) {
        viewModelScope.launch { appSettings.setPrivacyLockEnabled(enabled) }
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appSettings.setReminderEnabled(enabled)
            if (enabled) scheduleReminder() else cancelReminder()
        }
    }

    fun setReminderTime(time: String) {
        viewModelScope.launch {
            appSettings.setReminderTime(time)
            if (_uiState.value.reminderEnabled) {
                cancelReminder()
                scheduleReminder()
            }
        }
    }

    fun exportJson() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            try {
                val entries = repository.getAllEntries()
                val tagsMap = buildTagsMap(entries)
                val file = exporter.exportJson(entries) { entryId ->
                    tagsMap[entryId] ?: emptyList()
                }
                shareFile(file, "application/json")
                _uiState.update { it.copy(isExporting = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isExporting = false, exportError = e.message ?: "导出失败") }
            }
        }
    }

    fun exportCsv() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            try {
                val entries = repository.getAllEntries()
                val tagsMap = buildTagsMap(entries)
                val file = exporter.exportCsv(entries) { entryId ->
                    tagsMap[entryId] ?: emptyList()
                }
                shareFile(file, "text/csv")
                _uiState.update { it.copy(isExporting = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isExporting = false, exportError = e.message ?: "导出失败") }
            }
        }
    }

    private suspend fun buildTagsMap(entries: List<DiaryEntry>): Map<Long, List<String>> {
        val map = mutableMapOf<Long, List<String>>()
        for (entry in entries) {
            map[entry.id] = repository.getTagsForEntry(entry.id).map { it.name }
        }
        return map
    }

    private fun shareFile(file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享日记导出"))
    }

    fun clearExportError() {
        _uiState.update { it.copy(exportError = null) }
    }

    private fun scheduleReminder() {
        val time = _uiState.value.reminderTime
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 21
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val now = java.time.LocalTime.now()
        val target = java.time.LocalTime.of(hour, minute)
        var delayMinutes = java.time.Duration.between(now, target).toMinutes()
        if (delayMinutes < 0) delayMinutes += 24 * 60

        val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "daily_reminder",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private fun cancelReminder() {
        workManager.cancelUniqueWork("daily_reminder")
    }
}
