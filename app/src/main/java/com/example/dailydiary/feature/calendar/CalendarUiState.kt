package com.example.dailydiary.feature.calendar

import androidx.compose.ui.graphics.Color
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

data class CalendarDate(
    val date: LocalDate,
    val dayOfMonth: Int,
    val weekday: DayOfWeek,
    val moodColor: Color?,
    val isToday: Boolean
)

sealed interface CalendarUiState {
    data class Success(
        val currentMonth: YearMonth,
        val dates: List<CalendarDate>,
        val recordedDateSet: Set<LocalDate>
    ) : CalendarUiState
    data class Error(val message: String) : CalendarUiState
}
