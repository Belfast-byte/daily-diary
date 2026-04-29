# Stage 3: Calendar & History Screens Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build CalendarScreen (horizontal date scroller with mood dots), HistoryScreen (card list with search + mood filter), and shared DayDetailScreen (view/edit entry) — enabling browsing past entries by date or timeline.

**Architecture:** Three @HiltViewModel classes (CalendarViewModel, HistoryViewModel, DayDetailViewModel) following the Stage 2 pattern. Calendar queries recorded-dates range + entries range from DiaryRepository. History queries with optional search/filter. DayDetail loads single entry by date. Navigation uses existing NavHost with a new day_detail route.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Hilt ViewModel, Navigation Compose, Coroutines, StateFlow

---

## File Structure

```
feature/calendar/
├── CalendarScreen.kt              (MODIFY — replace placeholder)
├── CalendarViewModel.kt           (NEW)
└── CalendarUiState.kt             (NEW)
feature/history/
├── HistoryScreen.kt               (MODIFY — replace placeholder)
├── HistoryViewModel.kt            (NEW)
├── HistoryUiState.kt              (NEW)
└── components/
    ├── SearchBar.kt               (NEW)
    ├── MoodFilterChips.kt         (NEW)
    └── EntryCard.kt               (NEW)
feature/daydetail/
├── DayDetailScreen.kt             (NEW)
└── DayDetailViewModel.kt          (NEW)
feature/shell/
├── DailyDiaryApp.kt               (MODIFY — add route)
app/src/test/java/.../
├── feature/calendar/CalendarViewModelTest.kt   (NEW)
├── feature/history/HistoryViewModelTest.kt     (NEW)
```

---

### Task 1: Navigation — DayDetail Route

**Files:**
- Modify: `app/src/main/java/com/example/dailydiary/feature/shell/DailyDiaryApp.kt`
- Modify: `app/src/main/java/com/example/dailydiary/feature/shell/AppDestination.kt` (optional — DayDetail is not a bottom tab, no enum change needed)

- [ ] **Step 1: Add day_detail composable route to NavHost**

In `DailyDiaryApp.kt`, add the import:
```kotlin
import com.example.dailydiary.feature.daydetail.DayDetailScreen
```

Inside the `NavHost` composable block, add after the Settings route:
```kotlin
composable("day_detail/{date}") { backStackEntry ->
    val date = backStackEntry.arguments?.getString("date")
        ?.let { java.time.LocalDate.parse(it) } ?: return@composable
    DayDetailScreen(date = date, onNavigateBack = { navController.popBackStack() })
}
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: FAIL — DayDetailScreen class not found. This is expected; the implementation comes in Task 6.

---

### Task 2: CalendarUiState + CalendarViewModel

**Files:**
- Create: `app/src/main/java/com/example/dailydiary/feature/calendar/CalendarUiState.kt`
- Create: `app/src/main/java/com/example/dailydiary/feature/calendar/CalendarViewModel.kt`

- [ ] **Step 1: Write CalendarUiState**

```kotlin
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
```

- [ ] **Step 2: Write CalendarViewModel**

```kotlin
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
```

- [ ] **Step 3: Verify compilation**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/dailydiary/feature/calendar/
git commit -m "feat: add CalendarUiState and CalendarViewModel"
```

---

### Task 3: CalendarScreen Components + Screen

**Files:**
- Modify: `app/src/main/java/com/example/dailydiary/feature/calendar/CalendarScreen.kt`

All components are defined inside the file to avoid excessive file creation.

- [ ] **Step 1: Write CalendarScreen with inline components**

```kotlin
package com.example.dailydiary.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = hiltViewModel(),
    onDateClicked: (LocalDate) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        when (val state = uiState) {
            is CalendarUiState.Success -> {
                MonthYearHeader(
                    yearMonth = state.currentMonth,
                    onPrevious = viewModel::previousMonth,
                    onNext = viewModel::nextMonth
                )
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDateScroller(
                    dates = state.dates,
                    onDateClicked = onDateClicked
                )
            }
            is CalendarUiState.Error -> {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun MonthYearHeader(
    yearMonth: java.time.YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Text("◀", style = MaterialTheme.typography.titleMedium)
        }
        Text(
            text = "${yearMonth.year}年${yearMonth.monthValue}月",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        IconButton(onClick = onNext) {
            Text("▶", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun HorizontalDateScroller(
    dates: List<CalendarDate>,
    onDateClicked: (LocalDate) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(dates) { calDate ->
            DateCircle(
                calDate = calDate,
                onClick = { onDateClicked(calDate.date) }
            )
        }
    }
}

@Composable
private fun DateCircle(
    calDate: CalendarDate,
    onClick: () -> Unit
) {
    val borderMod = if (calDate.isToday) {
        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
    } else if (calDate.moodColor != null) {
        Modifier.border(1.dp, calDate.moodColor.copy(alpha = 0.5f), CircleShape)
    } else {
        Modifier
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(48.dp)
            .clip(CircleShape)
            .then(borderMod)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = calDate.weekday.getDisplayName(TextStyle.SHORT, Locale.CHINESE),
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = calDate.dayOfMonth.toString(),
            fontSize = 14.sp,
            fontWeight = if (calDate.isToday) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
        if (calDate.moodColor != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(calDate.moodColor, CircleShape)
            )
        } else {
            Spacer(modifier = Modifier.height(8.dp)) // preserve spacing
        }
    }
}
```

- [ ] **Step 2: Update DailyDiaryApp and CalendarScreen call site**

In `DailyDiaryApp.kt`, change `composable(AppDestination.Calendar.route) { CalendarScreen() }` to:
```kotlin
composable(AppDestination.Calendar.route) {
    CalendarScreen(
        onDateClicked = { date ->
            navController.navigate("day_detail/${date}")
        }
    )
}
```

Note: At this point the day_detail route doesn't exist yet (Task 1 created it but DayDetailScreen doesn't exist). For now, the navigation call will be a no-op at runtime until Task 6. Compilation will still pass because the CalendarScreen signature includes onDateClicked as a default param.

- [ ] **Step 3: Verify compilation**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL. (CalendarScreen compiles; DayDetailScreen reference from Task 1 will fail if Task 1 was committed.)

Important: If Task 1 was committed with the day_detail route referencing DayDetailScreen, you must either revert that commit or make the route a stub. The simplest approach is to keep Task 1 uncommitted until Task 6.

**Alternative:** Do NOT commit Task 1 separately. Instead, combine Tasks 1 and 6 in the same commit. Skip Step 2 of Task 1 (the verify step will fail as expected).

Adjustment: The navigation route addition goes in Task 6 (DayDetail). For Task 3, only update the Calendar composable call to pass `onDateClicked`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/dailydiary/feature/calendar/CalendarScreen.kt app/src/main/java/com/example/dailydiary/feature/shell/DailyDiaryApp.kt
git commit -m "feat: implement CalendarScreen with horizontal date scroller"
```

---

### Task 4: HistoryUiState + HistoryViewModel

**Files:**
- Create: `app/src/main/java/com/example/dailydiary/feature/history/HistoryUiState.kt`
- Create: `app/src/main/java/com/example/dailydiary/feature/history/HistoryViewModel.kt`

- [ ] **Step 1: Write HistoryUiState**

```kotlin
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
```

- [ ] **Step 2: Write HistoryViewModel**

```kotlin
package com.example.dailydiary.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailydiary.domain.model.Mood
import com.example.dailydiary.domain.repository.DiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: DiaryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadEntries()
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun onMoodFilterSelected(mood: Mood?) {
        _uiState.update { it.copy(selectedMoodFilter = mood) }
        applyFilters()
    }

    fun onToggleSearch() {
        _uiState.update { it.copy(isSearchExpanded = !it.isSearchExpanded) }
        if (!_uiState.value.isSearchExpanded) {
            // collapsed; reset search
            _uiState.update { it.copy(searchQuery = "") }
            loadEntries()
        }
    }

    private fun loadEntries() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val entries = repository.getAllEntries()
                val enriched = entries.map { entry ->
                    val mood = try { Mood.fromId(entry.moodId) } catch (_: Exception) { null }
                    val tags = repository.getTagsForEntry(entry.id)
                    DiaryEntryWithMeta(entry = entry, mood = mood, tags = tags)
                }
                _uiState.update { it.copy(entries = enriched, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "加载失败") }
            }
        }
    }

    private fun applyFilters() {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val entries = when {
                    state.searchQuery.isNotBlank() && state.selectedMoodFilter != null -> {
                        val bySearch = repository.searchByContent(state.searchQuery)
                        bySearch.filter { it.moodId == state.selectedMoodFilter!!.id }
                    }
                    state.searchQuery.isNotBlank() -> {
                        repository.searchByContent(state.searchQuery)
                    }
                    state.selectedMoodFilter != null -> {
                        repository.getByMood(state.selectedMoodFilter.id)
                    }
                    else -> repository.getAllEntries()
                }
                val enriched = entries.map { entry ->
                    val mood = try { Mood.fromId(entry.moodId) } catch (_: Exception) { null }
                    val tags = repository.getTagsForEntry(entry.id)
                    DiaryEntryWithMeta(entry = entry, mood = mood, tags = tags)
                }
                _uiState.update { it.copy(entries = enriched, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "查询失败") }
            }
        }
    }
}
```

- [ ] **Step 3: Verify compilation**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/dailydiary/feature/history/HistoryUiState.kt app/src/main/java/com/example/dailydiary/feature/history/HistoryViewModel.kt
git commit -m "feat: add HistoryUiState and HistoryViewModel"
```

---

### Task 5: HistoryScreen Components + Screen

**Files:**
- Create: `app/src/main/java/com/example/dailydiary/feature/history/components/SearchBar.kt`
- Create: `app/src/main/java/com/example/dailydiary/feature/history/components/MoodFilterChips.kt`
- Create: `app/src/main/java/com/example/dailydiary/feature/history/components/EntryCard.kt`
- Modify: `app/src/main/java/com/example/dailydiary/feature/history/HistoryScreen.kt`

- [ ] **Step 1: Write SearchBar**

```kotlin
package com.example.dailydiary.feature.history.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = isVisible) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            placeholder = {
                Text(
                    "搜索日记...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            },
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
            )
        )
    }
}
```

- [ ] **Step 2: Write MoodFilterChips**

```kotlin
package com.example.dailydiary.feature.history.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dailydiary.domain.model.Mood

@Composable
fun MoodFilterChips(
    moods: List<Mood>,
    selectedMood: Mood?,
    onMoodSelected: (Mood?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        item {
            FilterChip(
                label = "全部",
                isSelected = selectedMood == null,
                color = MaterialTheme.colorScheme.primary,
                onClick = { onMoodSelected(null) }
            )
        }
        items(moods) { mood ->
            FilterChip(
                label = "${mood.label.take(1)} ${mood.label}",
                isSelected = mood == selectedMood,
                color = mood.color,
                onClick = { onMoodSelected(mood) }
            )
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
        tonalElevation = if (isSelected) 0.dp else 1.dp
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}
```

- [ ] **Step 3: Write EntryCard**

```kotlin
package com.example.dailydiary.feature.history.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.dailydiary.feature.history.DiaryEntryWithMeta
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun EntryCard(
    entry: DiaryEntryWithMeta,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: date + mood badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val dayOfWeek = entry.entry.entryDate.dayOfWeek
                    .getDisplayName(TextStyle.FULL, Locale.CHINESE)
                Text(
                    text = "${entry.entry.entryDate.monthValue}月${entry.entry.entryDate.dayOfMonth}日 $dayOfWeek",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.weight(1f))
                entry.mood?.let { mood ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = mood.color.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${mood.label.take(1)} ${mood.label}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = mood.color,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Content preview
            if (entry.entry.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = entry.entry.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Tags
            if (entry.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    entry.tags.take(4).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                        ) {
                            Text(
                                text = tag.name,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    if (entry.tags.size > 4) {
                        Text(
                            text = "+${entry.tags.size - 4}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 4: Rewrite HistoryScreen**

```kotlin
package com.example.dailydiary.feature.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
        androidx.compose.foundation.layout.Row(
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
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
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
```

- [ ] **Step 5: Update DailyDiaryApp History call site**

In `DailyDiaryApp.kt`, change `composable(AppDestination.History.route) { HistoryScreen() }` to:
```kotlin
composable(AppDestination.History.route) {
    HistoryScreen(
        onEntryClicked = { date ->
            navController.navigate("day_detail/${date}")
        }
    )
}
```

- [ ] **Step 6: Verify compilation**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/example/dailydiary/feature/history/ app/src/main/java/com/example/dailydiary/feature/shell/DailyDiaryApp.kt
git commit -m "feat: implement HistoryScreen with search, mood filter, and entry cards"
```

---

### Task 6: DayDetailScreen + ViewModel + Navigation

**Files:**
- Create: `app/src/main/java/com/example/dailydiary/feature/daydetail/DayDetailViewModel.kt`
- Create: `app/src/main/java/com/example/dailydiary/feature/daydetail/DayDetailScreen.kt`
- Modify: `app/src/main/java/com/example/dailydiary/feature/shell/DailyDiaryApp.kt` (add route + import)

- [ ] **Step 1: Write DayDetailViewModel**

```kotlin
package com.example.dailydiary.feature.daydetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailydiary.domain.model.ActivityTag
import com.example.dailydiary.domain.model.DiaryEntry
import com.example.dailydiary.domain.model.Mood
import com.example.dailydiary.domain.repository.DiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class DayDetailUiState(
    val entry: DiaryEntry? = null,
    val mood: Mood? = null,
    val tags: List<ActivityTag> = emptyList(),
    val isEditing: Boolean = false,
    val editContent: String = "",
    val editMood: Mood? = null,
    val editTagIds: Set<Long> = emptySet(),
    val availableTags: List<ActivityTag> = emptyList(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class DayDetailViewModel @Inject constructor(
    private val repository: DiaryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DayDetailUiState())
    val uiState: StateFlow<DayDetailUiState> = _uiState.asStateFlow()

    fun load(date: LocalDate) {
        viewModelScope.launch {
            repository.observeEntryByDate(date).collect { entry ->
                val mood = entry?.let { try { Mood.fromId(it.moodId) } catch (_: Exception) { null } }
                val tags = entry?.let { repository.getTagsForEntry(it.id) } ?: emptyList()
                _uiState.update { state ->
                    state.copy(entry = entry, mood = mood, tags = tags)
                }
            }
        }
        viewModelScope.launch {
            repository.observeAllTags().collect { tags ->
                _uiState.update { it.copy(availableTags = tags) }
            }
        }
    }

    fun toggleEdit() {
        val state = _uiState.value
        if (state.isEditing) {
            _uiState.update { it.copy(isEditing = false) }
        } else {
            _uiState.update {
                it.copy(
                    isEditing = true,
                    editContent = it.entry?.content ?: "",
                    editMood = it.mood,
                    editTagIds = it.tags.map { t -> t.id }.toSet()
                )
            }
        }
    }

    fun onEditContentChanged(text: String) {
        _uiState.update { it.copy(editContent = text) }
    }

    fun onEditMoodSelected(mood: Mood) {
        _uiState.update { it.copy(editMood = mood) }
    }

    fun onEditTagToggled(tagId: Long) {
        _uiState.update { state ->
            val newSet = state.editTagIds.toMutableSet()
            if (newSet.contains(tagId)) newSet.remove(tagId) else newSet.add(tagId)
            state.copy(editTagIds = newSet)
        }
    }

    fun onSave() {
        val state = _uiState.value
        val mood = state.editMood ?: return
        val entry = state.entry ?: return
        if (state.isSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                repository.saveEntry(
                    date = entry.entryDate,
                    mood = mood,
                    content = state.editContent,
                    tagIds = state.editTagIds.toList()
                )
                _uiState.update { it.copy(isSaving = false, isEditing = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = e.message ?: "保存失败")
                }
            }
        }
    }
}
```

- [ ] **Step 2: Write DayDetailScreen**

```kotlin
package com.example.dailydiary.feature.daydetail

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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.dailydiary.domain.model.Mood
import com.example.dailydiary.feature.today.components.ContentCard
import com.example.dailydiary.feature.today.components.MoodSelector
import com.example.dailydiary.feature.today.components.TagSelector
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DayDetailScreen(
    date: LocalDate,
    onNavigateBack: () -> Unit,
    viewModel: DayDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(date) {
        viewModel.load(date)
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINESE)
    val dateText = "${date.monthValue}月${date.dayOfMonth}日 $dayOfWeek"

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Top bar: back + date + edit toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Text("←", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (uiState.entry != null && !uiState.isEditing) {
                    IconButton(onClick = { viewModel.toggleEdit() }) {
                        Text("✏️", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            if (uiState.entry == null && !uiState.isEditing) {
                // No entry for this date
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "当天没有记录",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.toggleEdit() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("创建记录")
                }
            } else if (uiState.isEditing) {
                // Edit mode
                Spacer(modifier = Modifier.height(12.dp))
                MoodSelector(
                    moods = Mood.entries,
                    selectedMood = uiState.editMood,
                    onMoodSelected = viewModel::onEditMoodSelected
                )
                Spacer(modifier = Modifier.height(12.dp))
                ContentCard(
                    content = uiState.editContent,
                    onContentChanged = viewModel::onEditContentChanged
                )
                Spacer(modifier = Modifier.height(12.dp))
                TagSelector(
                    tags = uiState.availableTags,
                    selectedTagIds = uiState.editTagIds,
                    onTagToggled = viewModel::onEditTagToggled
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { viewModel.toggleEdit() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                    ) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.onSave() },
                        modifier = Modifier.weight(1f),
                        enabled = uiState.editMood != null && !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("保存")
                        }
                    }
                }
            } else {
                // Readonly view
                Spacer(modifier = Modifier.height(16.dp))

                // Mood display
                uiState.mood?.let { mood ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = mood.color.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = mood.label.take(1),
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = mood.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = mood.color
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content
                if (!uiState.entry?.content.isNullOrBlank()) {
                    Text(
                        text = uiState.entry!!.content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Text(
                        text = "（无内容）",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }

                // Tags
                if (uiState.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🏷",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        uiState.tags.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                            ) {
                                Text(
                                    text = tag.name,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 3: Add navigation route in DailyDiaryApp.kt**

Add import:
```kotlin
import com.example.dailydiary.feature.daydetail.DayDetailScreen
import java.time.LocalDate
```

Add composable route inside NavHost (after existing routes):
```kotlin
composable("day_detail/{date}") { backStackEntry ->
    val dateStr = backStackEntry.arguments?.getString("date") ?: return@composable
    val date = LocalDate.parse(dateStr)
    DayDetailScreen(
        date = date,
        onNavigateBack = { navController.popBackStack() }
    )
}
```

- [ ] **Step 4: Verify compilation**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/dailydiary/feature/daydetail/ app/src/main/java/com/example/dailydiary/feature/shell/DailyDiaryApp.kt
git commit -m "feat: add DayDetailScreen with view/edit toggle and navigation"
```

---

### Task 7: ViewModel Unit Tests

**Files:**
- Create: `app/src/test/java/com/example/dailydiary/feature/calendar/CalendarViewModelTest.kt`
- Create: `app/src/test/java/com/example/dailydiary/feature/history/HistoryViewModelTest.kt`

- [ ] **Step 1: Write CalendarViewModelTest**

```kotlin
package com.example.dailydiary.feature.calendar

import app.cash.turbine.test
import com.example.dailydiary.domain.model.ActivityTag
import com.example.dailydiary.domain.model.DiaryEntry
import com.example.dailydiary.domain.model.DiaryEntryWithTags
import com.example.dailydiary.domain.model.Mood
import com.example.dailydiary.domain.repository.DiaryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeDiaryRepository
    private lateinit var viewModel: CalendarViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeDiaryRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads current month dates`() = runTest(testDispatcher) {
        val today = LocalDate.now()
        val entry = DiaryEntry(
            id = 1, entryDate = today, moodId = "happy",
            content = "", createdAt = Instant.now(), updatedAt = Instant.now()
        )
        repository.setEntryForDate(today, entry)

        viewModel = CalendarViewModel(repository)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is CalendarUiState.Success)
            val success = state as CalendarUiState.Success
            assertEquals(YearMonth.now(), success.currentMonth)
            assertTrue(success.dates.isNotEmpty())
            assertTrue(today in success.recordedDateSet)
        }
    }

    @Test
    fun `previousMonth decrements month`() = runTest(testDispatcher) {
        viewModel = CalendarViewModel(repository)
        advanceUntilIdle()

        viewModel.uiState.test {
            val initial = (awaitItem() as CalendarUiState.Success)
            viewModel.previousMonth()
            val prev = (awaitItem() as CalendarUiState.Success)
            assertEquals(initial.currentMonth.minusMonths(1), prev.currentMonth)
        }
    }

    @Test
    fun `nextMonth increments month`() = runTest(testDispatcher) {
        viewModel = CalendarViewModel(repository)
        advanceUntilIdle()

        viewModel.uiState.test {
            val initial = (awaitItem() as CalendarUiState.Success)
            viewModel.nextMonth()
            val next = (awaitItem() as CalendarUiState.Success)
            assertEquals(initial.currentMonth.plusMonths(1), next.currentMonth)
        }
    }
}

class FakeDiaryRepository : DiaryRepository {
    private val entriesByDate = mutableMapOf<LocalDate, DiaryEntry>()

    fun setEntryForDate(date: LocalDate, entry: DiaryEntry) {
        entriesByDate[date] = entry
    }

    override suspend fun getEntryByDate(date: LocalDate): DiaryEntry? = entriesByDate[date]
    override fun observeEntryByDate(date: LocalDate): Flow<DiaryEntry?> = flowOf(entriesByDate[date])
    override suspend fun saveEntry(date: LocalDate, mood: Mood, content: String, tagIds: List<Long>): DiaryEntry {
        val entry = DiaryEntry(id = 1, entryDate = date, moodId = mood.id, content = content, createdAt = Instant.now(), updatedAt = Instant.now())
        entriesByDate[date] = entry
        return entry
    }
    override suspend fun getEntriesInRange(start: LocalDate, end: LocalDate): List<DiaryEntry> =
        entriesByDate.filterKeys { !it.isBefore(start) && !it.isAfter(end) }.values.toList()
    override suspend fun getEntryWithTags(entryId: Long): DiaryEntryWithTags? = null
    override suspend fun searchByContent(keyword: String): List<DiaryEntry> = emptyList()
    override suspend fun getByMood(moodId: String): List<DiaryEntry> = emptyList()
    override suspend fun getAllEntries(): List<DiaryEntry> = entriesByDate.values.toList()
    override suspend fun getTotalEntryCount(): Int = entriesByDate.size
    override suspend fun getEntryCountInRange(start: LocalDate, end: LocalDate): Int = 0
    override suspend fun getRecordedDatesInRange(start: LocalDate, end: LocalDate): List<LocalDate> =
        entriesByDate.keys.filter { !it.isBefore(start) && !it.isAfter(end) }
    override suspend fun getMoodDistribution(start: LocalDate, end: LocalDate): List<Pair<Mood, Long>> = emptyList()
    override suspend fun getActiveTags(): List<ActivityTag> = emptyList()
    override fun observeAllTags(): Flow<List<ActivityTag>> = flowOf(emptyList())
    override suspend fun getTagsForEntry(entryId: Long): List<ActivityTag> = emptyList()
    override suspend fun seedDefaultTagsIfNeeded() {}
}
```

- [ ] **Step 2: Write HistoryViewModelTest**

```kotlin
package com.example.dailydiary.feature.history

import app.cash.turbine.test
import com.example.dailydiary.domain.model.ActivityTag
import com.example.dailydiary.domain.model.DiaryEntry
import com.example.dailydiary.domain.model.DiaryEntryWithTags
import com.example.dailydiary.domain.model.Mood
import com.example.dailydiary.domain.repository.DiaryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeDiaryRepository
    private lateinit var viewModel: HistoryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeDiaryRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads all entries`() = runTest(testDispatcher) {
        val entry = DiaryEntry(
            id = 1, entryDate = LocalDate.now(), moodId = "happy",
            content = "test", createdAt = Instant.now(), updatedAt = Instant.now()
        )
        repository.addEntry(entry)
        viewModel = HistoryViewModel(repository)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.entries.size)
            assertEquals("test", state.entries[0].entry.content)
        }
    }

    @Test
    fun `onSearchQueryChanged filters entries`() = runTest(testDispatcher) {
        repository.addEntry(DiaryEntry(id = 1, entryDate = LocalDate.now(), moodId = "happy", content = "hello world", createdAt = Instant.now(), updatedAt = Instant.now()))
        repository.addEntry(DiaryEntry(id = 2, entryDate = LocalDate.now().minusDays(1), moodId = "calm", content = "goodbye", createdAt = Instant.now(), updatedAt = Instant.now()))
        viewModel = HistoryViewModel(repository)
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("hello")
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.entries.size)
            assertEquals("hello world", state.entries[0].entry.content)
        }
    }

    @Test
    fun `onMoodFilterSelected filters by mood`() = runTest(testDispatcher) {
        repository.addEntry(DiaryEntry(id = 1, entryDate = LocalDate.now(), moodId = "happy", content = "a", createdAt = Instant.now(), updatedAt = Instant.now()))
        repository.addEntry(DiaryEntry(id = 2, entryDate = LocalDate.now().minusDays(1), moodId = "sad", content = "b", createdAt = Instant.now(), updatedAt = Instant.now()))
        viewModel = HistoryViewModel(repository)
        advanceUntilIdle()

        viewModel.onMoodFilterSelected(Mood.HAPPY)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.entries.size)
            assertEquals(Mood.HAPPY, state.entries[0].mood)
        }
    }

    @Test
    fun `onMoodFilterSelected with null shows all`() = runTest(testDispatcher) {
        repository.addEntry(DiaryEntry(id = 1, entryDate = LocalDate.now(), moodId = "happy", content = "a", createdAt = Instant.now(), updatedAt = Instant.now()))
        repository.addEntry(DiaryEntry(id = 2, entryDate = LocalDate.now().minusDays(1), moodId = "sad", content = "b", createdAt = Instant.now(), updatedAt = Instant.now()))
        viewModel = HistoryViewModel(repository)
        advanceUntilIdle()
        viewModel.onMoodFilterSelected(Mood.HAPPY)
        advanceUntilIdle()

        viewModel.onMoodFilterSelected(null)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.entries.size)
        }
    }
}

class FakeDiaryRepository : DiaryRepository {
    private val entries = mutableListOf<DiaryEntry>()

    fun addEntry(entry: DiaryEntry) {
        entries.add(entry)
    }

    override suspend fun getEntryByDate(date: LocalDate): DiaryEntry? = entries.firstOrNull { it.entryDate == date }
    override fun observeEntryByDate(date: LocalDate): Flow<DiaryEntry?> = flowOf(entries.firstOrNull { it.entryDate == date })
    override suspend fun saveEntry(date: LocalDate, mood: Mood, content: String, tagIds: List<Long>): DiaryEntry {
        val e = DiaryEntry(id = 1, entryDate = date, moodId = mood.id, content = content, createdAt = Instant.now(), updatedAt = Instant.now())
        entries.add(e); return e
    }
    override suspend fun getEntriesInRange(start: LocalDate, end: LocalDate): List<DiaryEntry> = emptyList()
    override suspend fun getEntryWithTags(entryId: Long): DiaryEntryWithTags? = null
    override suspend fun searchByContent(keyword: String): List<DiaryEntry> =
        entries.filter { it.content.contains(keyword, ignoreCase = true) }
    override suspend fun getByMood(moodId: String): List<DiaryEntry> = entries.filter { it.moodId == moodId }
    override suspend fun getAllEntries(): List<DiaryEntry> = entries.toList()
    override suspend fun getTotalEntryCount(): Int = entries.size
    override suspend fun getEntryCountInRange(start: LocalDate, end: LocalDate): Int = 0
    override suspend fun getRecordedDatesInRange(start: LocalDate, end: LocalDate): List<LocalDate> = emptyList()
    override suspend fun getMoodDistribution(start: LocalDate, end: LocalDate): List<Pair<Mood, Long>> = emptyList()
    override suspend fun getActiveTags(): List<ActivityTag> = emptyList()
    override fun observeAllTags(): Flow<List<ActivityTag>> = flowOf(emptyList())
    override suspend fun getTagsForEntry(entryId: Long): List<ActivityTag> = emptyList()
    override suspend fun seedDefaultTagsIfNeeded() {}
}
```

- [ ] **Step 3: Run tests**

```bash
./gradlew :app:testDebugUnitTest --tests "com.example.dailydiary.feature.calendar.*" --tests "com.example.dailydiary.feature.history.*"
```
Expected: All tests PASS.

- [ ] **Step 4: Commit**

```bash
git add app/src/test/java/com/example/dailydiary/feature/calendar/ app/src/test/java/com/example/dailydiary/feature/history/
git commit -m "test: add unit tests for CalendarViewModel and HistoryViewModel"
```

---

## Stage 3 Validation Checklist

1. `./gradlew :app:compileDebugKotlin` succeeds.
2. `./gradlew :app:testDebugUnitTest` — all ViewModel tests pass.
3. Calendar shows current month with horizontal date scroller.
4. Dates with entries show mood color dots; today is highlighted.
5. Month navigation (◀ ▶) works.
6. History shows all entries in LazyColumn cards.
7. Search bar expands/collapses and filters entries.
8. Mood filter chips filter entries by mood.
9. Tapping a calendar date or history card navigates to DayDetail.
10. DayDetail shows readonly view with mood, content, tags.
11. Edit toggle switches to editable mode; save persists changes.

## Next Stage

After Stage 3 is complete, proceed to Stage 4 (Statistics) using the same planning process.
