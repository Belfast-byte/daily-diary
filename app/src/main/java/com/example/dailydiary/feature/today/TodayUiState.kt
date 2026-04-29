package com.example.dailydiary.feature.today

import com.example.dailydiary.domain.model.ActivityTag
import com.example.dailydiary.domain.model.Mood

data class TodayUiState(
    val selectedMood: Mood? = null,
    val content: String = "",
    val selectedTagIds: Set<Long> = emptySet(),
    val availableTags: List<ActivityTag> = emptyList(),
    val isExistingEntry: Boolean = false,
    val isSaving: Boolean = false,
    val hasSaved: Boolean = false,
    val errorMessage: String? = null
)
