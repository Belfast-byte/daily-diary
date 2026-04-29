package com.example.dailydiary.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailydiary.domain.model.Mood
import com.example.dailydiary.domain.repository.DiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repository: DiaryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadStats()
    }

    fun retry() {
        loadStats()
    }

    private fun loadStats() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val today = LocalDate.now()
                val allDates = repository.getRecordedDatesInRange(LocalDate.MIN, today)
                val sortedDates = allDates.sortedDescending()
                val totalDays = allDates.size
                val streakDays = computeStreak(sortedDates, today)
                val monthlyCount = repository.getEntryCountInRange(
                    today.withDayOfMonth(1), today
                )
                val weekDist = repository.getMoodDistribution(
                    today.minusDays(7), today
                ).mapNotNull { (mood, count) -> mood?.let { MoodCount(it, count) } }
                val topMood = weekDist.maxByOrNull { it.count }?.mood
                val entries30 = repository.getEntriesInRange(
                    today.minusDays(30), today
                ).associateBy { it.entryDate }
                val monthTrend = (0..30).map { daysAgo ->
                    val date = today.minusDays(daysAgo.toLong())
                    val mood = entries30[date]?.let { Mood.fromId(it.moodId) }
                    DayMood(date = date, mood = mood)
                }.reversed()

                _uiState.update {
                    it.copy(
                        totalDays = totalDays,
                        streakDays = streakDays,
                        monthlyCount = monthlyCount,
                        topMood = topMood,
                        weekDistribution = weekDist,
                        monthTrend = monthTrend,
                        isLoading = false,
                        hasEnoughData = totalDays >= 3
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "加载失败") }
            }
        }
    }

    private fun computeStreak(sortedDesc: List<LocalDate>, today: LocalDate): Int {
        if (sortedDesc.isEmpty()) return 0
        var expected = today
        var streak = 0
        for (date in sortedDesc) {
            if (date == expected) {
                streak++
                expected = expected.minusDays(1)
            } else if (date < expected) break
        }
        return streak
    }
}
