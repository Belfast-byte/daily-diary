package com.example.dailydiary.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showTimePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "设置",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Security section
        SectionHeader("安全")
        SettingRow(
            label = "隐私锁",
            description = "切换到后台后需要验证身份",
            switch = {
                Switch(
                    checked = uiState.privacyLockEnabled,
                    onCheckedChange = viewModel::setPrivacyLockEnabled
                )
            }
        )

        Spacer(modifier = Modifier.height(16.dp))
        SectionHeader("提醒")
        SettingRow(
            label = "每日提醒",
            description = "每天固定时间提醒记录",
            switch = {
                Switch(
                    checked = uiState.reminderEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                                != PackageManager.PERMISSION_GRANTED) {
                                (context as? android.app.Activity)?.requestPermissions(
                                    arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0
                                )
                            }
                        }
                        viewModel.setReminderEnabled(enabled)
                    }
                )
            }
        )
        if (uiState.reminderEnabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 4.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("提醒时间: ${uiState.reminderTime}")
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = { showTimePicker = !showTimePicker }) {
                    Text("更改")
                }
            }
            if (showTimePicker) {
                val timeState = rememberTimePickerState()
                TimePicker(state = timeState)
                Row(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = { showTimePicker = false }) { Text("取消") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        val hour = timeState.hour.toString().padStart(2, '0')
                        val minute = timeState.minute.toString().padStart(2, '0')
                        viewModel.setReminderTime("$hour:$minute")
                        showTimePicker = false
                    }) { Text("确认") }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        SectionHeader("数据")
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = viewModel::exportJson,
                modifier = Modifier.weight(1f),
                enabled = !uiState.isExporting
            ) { Text("导出 JSON") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = viewModel::exportCsv,
                modifier = Modifier.weight(1f),
                enabled = !uiState.isExporting
            ) { Text("导出 CSV") }
        }
        uiState.exportError?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "隐私说明\n\nDaily Diary 所有数据仅存储在您的设备本地，不会上传到任何服务器。日记内容使用 SQLCipher 加密存储，密码通过 Android KeyStore 保护。\n\n导出文件不加密，请妥善保管。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun SettingRow(
    label: String,
    description: String,
    switch: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
            Spacer(modifier = Modifier.width(12.dp))
            switch()
        }
    }
}
