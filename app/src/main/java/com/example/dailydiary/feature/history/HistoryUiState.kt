package com.example.dailydiary.feature.history

import com.example.dailydiary.domain.model.ActivityTag
import com.example.dailydiary.domain.model.DiaryEntry
import com.example.dailydiary.domain.model.Mood

data class DiaryEntryWithMeta(
    val entry: DiaryEntry,
    val mood: Mood?,
    val tags: List<ActivityTag>
)

data class HistoryUiState(
    val entries: List<DiaryEntryWithMeta> = emptyList(),
    val searchQuery: String = "",
    val selectedMoodFilter: Mood? = null,
    val isSearchExpanded: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
