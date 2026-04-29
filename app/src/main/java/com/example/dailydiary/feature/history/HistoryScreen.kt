package com.example.dailydiary.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.dailydiary.domain.model.Mood
import com.example.dailydiary.feature.history.components.EntryCard
import com.example.dailydiary.feature.history.components.MoodFilterChips
import com.example.dailydiary.feature.history.components.SearchBar
import java.time.LocalDate

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    onEntryClicked: (LocalDate) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "历史记录",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { viewModel.onToggleSearch() }) {
                Text("🔍", style = MaterialTheme.typography.titleMedium)
            }
        }

        // Search bar
        SearchBar(
            query = uiState.searchQuery,
            onQueryChanged = viewModel::onSearchQueryChanged,
            isVisible = uiState.isSearchExpanded
        )

        if (uiState.isSearchExpanded) {
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Mood filter
        MoodFilterChips(
            moods = Mood.entries,
            selectedMood = uiState.selectedMoodFilter,
            onMoodSelected = viewModel::onMoodFilterSelected
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Content
        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.errorMessage != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = uiState.errorMessage!!,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            uiState.entries.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (uiState.searchQuery.isNotBlank()) "未找到匹配" else "暂无记录",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.entries, key = { it.entry.id }) { entry ->
                        EntryCard(
                            entry = entry,
                            onClick = { onEntryClicked(entry.entry.entryDate) }
                        )
                    }
                }
            }
        }
    }
}
