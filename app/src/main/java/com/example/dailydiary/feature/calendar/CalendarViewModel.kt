package com.example.dailydiary.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailydiary.domain.model.Mood
import com.example.dailydiary.domain.repository.DiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: DiaryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CalendarUiState>(
        CalendarUiState.Success(
            currentMonth = YearMonth.now(),
            dates = emptyList(),
            recordedDateSet = emptySet()
        )
    )
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        loadMonth(YearMonth.now())
    }

    fun previousMonth() {
        val current = (_uiState.value as? CalendarUiState.Success)?.currentMonth ?: return
        loadMonth(current.minusMonths(1))
    }

    fun nextMonth() {
        val current = (_uiState.value as? CalendarUiState.Success)?.currentMonth ?: return
        loadMonth(current.plusMonths(1))
    }

    private fun loadMonth(month: YearMonth) {
        viewModelScope.launch {
            try {
                val start = month.atDay(1)
                val end = month.atEndOfMonth()
                val recordedDates = repository.getRecordedDatesInRange(start, end)
                val entries = repository.getEntriesInRange(start, end)
                val entryMap = entries.associateBy { it.entryDate }
                val today = LocalDate.now()

                val dates = start.datesUntil(end.plusDays(1)).map { date ->
                    val entry = entryMap[date]
                    CalendarDate(
                        date = date,
                        dayOfMonth = date.dayOfMonth,
                        weekday = date.dayOfWeek,
                        moodColor = entry?.let { Mood.fromId(it.moodId).color },
                        isToday = date == today
                    )
                }.toList()

                _uiState.value = CalendarUiState.Success(
                    currentMonth = month,
                    dates = dates,
                    recordedDateSet = recordedDates.toSet()
                )
            } catch (e: Exception) {
                _uiState.value = CalendarUiState.Error(e.message ?: "加载失败")
            }
        }
    }
}
