package com.example.dailydiary.feature.stats

import com.example.dailydiary.domain.model.Mood
import java.time.LocalDate

data class MoodCount(val mood: Mood, val count: Long)

data class DayMood(val date: LocalDate, val mood: Mood?)

data class StatsUiState(
    val totalDays: Int = 0,
    val streakDays: Int = 0,
    val monthlyCount: Int = 0,
    val topMood: Mood? = null,
    val weekDistribution: List<MoodCount> = emptyList(),
    val monthTrend: List<DayMood> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val hasEnoughData: Boolean = false
)
