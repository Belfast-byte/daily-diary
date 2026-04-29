# Stage 3: Calendar & History Screens Design

## Goal

Build CalendarScreen (horizontal date scroller + mood dots), HistoryScreen (card list + search + mood filter), and a shared DayDetailScreen (view/edit entry). Users can browse past entries by date or timeline, search by keyword, and filter by mood.

## Architecture

Three ViewModels (CalendarViewModel, HistoryViewModel, DayDetailViewModel) each with StateFlow<T> pattern established in Stage 2. Calendar queries recorded dates and entries in month range. History queries all entries with optional search/filter. DayDetail loads a single entry by date. Navigation: Calendar date tap or History card tap → DayDetailScreen(date).

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Hilt ViewModel, Navigation Compose

---

## Component Tree

```
CalendarScreen
├── MonthYearHeader (◀ month/year ▶)
├── HorizontalDateScroller (LazyRow)
│   └── DateCircle × N (day number, weekday, mood dot if has entry, today highlighted)
└── → navigates to DayDetailScreen(date)

HistoryScreen
├── TopBar ("历史记录" + search icon)
├── SearchBar (expandable, OutlinedTextField)
├── MoodFilterChips (horizontal scroll, "全部" + each mood)
└── EntryCardList (LazyColumn)
    └── EntryCard (date, mood badge, content preview, tag chips → tap → DayDetail)

DayDetailScreen (shared, receives date param)
├── DateHeader
├── MoodChip (readonly)
├── ContentText (readonly → editable toggle)
├── TagChips (readonly)
└── EditButton → switches to TodayScreen-like edit mode
```

## Data Flow

### CalendarViewModel
```
sealed interface CalendarUiState {
    data class Success(
        val currentMonth: YearMonth,
        val dates: List<CalendarDate>,
        val recordedDateSet: Set<LocalDate>
    ) : CalendarUiState
    data class Error(val message: String) : CalendarUiState
}

data class CalendarDate(
    val date: LocalDate,
    val dayOfMonth: Int,
    val weekday: DayOfWeek,
    val moodColor: Color?,  // null if no entry
    val isToday: Boolean
)

Init: loadMonth(YearMonth.now())
  → getRecordedDatesInRange(monthStart, monthEnd)
  → getEntriesInRange(monthStart, monthEnd)
  → map to List<CalendarDate>
```

### HistoryViewModel
```
data class HistoryUiState(
    val entries: List<DiaryEntryWithMood> = emptyList(),
    val searchQuery: String = "",
    val selectedMoodFilter: Mood? = null,  // null = "全部"
    val isSearchExpanded: Boolean = false,
    val isLoading: Boolean = false
)

data class DiaryEntryWithMood(
    val entry: DiaryEntry,
    val mood: Mood?,
    val tags: List<ActivityTag>
)

Init: loadAll() → getAllEntries() → map with moods/tags
onSearch(query) → searchByContent(query) → update entries
onMoodFilter(mood?) → getByMood(mood.id) or getAllEntries()
```

### DayDetailViewModel
```
data class DayDetailUiState(
    val entry: DiaryEntry? = null,
    val mood: Mood? = null,
    val tags: List<ActivityTag> = emptyList(),
    val isEditing: Boolean = false
)

Init: observeEntryByDate(date) → populate (mood, content, tags)
toggleEdit() → isEditing = !isEditing
onSave(mood, content, tagIds) → saveEntry() → isEditing = false
```

## Navigation

Add 1 new route to AppDestination (no bottom nav tab):
```
composable("day_detail/{date}") { DayDetailScreen(date, navController) }
```

Calendar date tap → `navController.navigate("day_detail/${date}")`
History card tap → `navController.navigate("day_detail/${date}")`

## UI Design

### Calendar — Horizontal Date Scroller
- Fixed header: ◀ 2026年4月 ▶ with month navigation
- LazyRow of DateCircle composables (48dp diameter)
- Each circle: day number centered, weekday abbreviated below, mood color dot at bottom if has entry
- Today: primary color border ring
- Empty dates: muted text color, no background

### History — Card List
- Search bar: expandable on icon tap, slides in from top
- Mood filter chips: LazyRow below search, "全部" chip + one chip per mood (emoji + label)
- Entry cards: rounded 12dp white cards with date header, mood badge, content preview (2 lines max), tag chips row
- Pull-to-refresh or swipe-to-delete not in MVP — keep simple

### DayDetail — View/Edit Toggle
- Default: readonly view showing mood, content, tags
- Top-right edit icon button → switches to editable mode
- Edit mode: reuses MoodSelector, ContentCard, TagSelector from Stage 2
- Save → updates entry, switches back to readonly

## States & Edge Cases

| State | Behavior |
|-------|----------|
| Month with no entries | All DateCircles show without mood dots |
| Empty history | "暂无记录" centered text |
| Search with no results | "未找到匹配" with suggestion to broaden |
| DayDetail for date with no entry | "当天没有记录" → offer to create one |
| Month navigation at boundaries | Arrows disabled or wrap-around |

## Error Handling
- Repository query failures → CalendarUiState.Error / HistoryUiState with error
- Snackbar for transient errors (same pattern as Stage 2)
- DayDetail save failure → preserve edits, show error

## Testing
- CalendarViewModel unit tests: month loading, date list generation, navigation events
- HistoryViewModel unit tests: search, mood filter, combined filter
- DayDetailViewModel unit tests: entry loading, edit toggle, save

---

## File Structure

```
feature/calendar/
├── CalendarScreen.kt            (MODIFY)
├── CalendarViewModel.kt         (NEW)
└── CalendarUiState.kt           (NEW)
feature/history/
├── HistoryScreen.kt             (MODIFY)
├── HistoryViewModel.kt          (NEW)
├── HistoryUiState.kt            (NEW)
└── components/
    ├── SearchBar.kt             (NEW)
    ├── MoodFilterChips.kt       (NEW)
    └── EntryCard.kt             (NEW)
feature/daydetail/
├── DayDetailScreen.kt           (NEW)
└── DayDetailViewModel.kt        (NEW)
feature/shell/
├── DailyDiaryApp.kt             (MODIFY — add day_detail route)
└── AppDestination.kt            (MODIFY — add DayDetail destination)
```
