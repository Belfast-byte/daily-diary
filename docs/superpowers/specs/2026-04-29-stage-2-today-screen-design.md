# Stage 2: Today Entry Screen Design

## Goal

Build the TodayScreen where users complete the core diary recording loop: select mood (required), optionally write content and pick activity tags, then save. One entry per day — re-opening shows and edits the existing entry.

## Architecture

**ViewModel-driven Compose screen** with unidirectional data flow. `TodayViewModel` exposes a `StateFlow<TodayUiState>`, loads today's existing entry and available tags on init, and delegates persistence to `DiaryRepository`. Screen components are stateless, receiving callbacks and data from the ViewModel.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Hilt ViewModel, Kotlin Coroutines

---

## Component Tree

```
TodayScreen
├── DateHeader          — date display + greeting based on time of day
├── MoodSelector        — 7 vertical capsule pills, horizontally scrollable, single-select
├── ContentCard         — partitioned card with header "今天的记录", multiline text input
├── TagSelector         — partitioned card with header "活动标签", colored dot chips, horizontal scroll
└── SaveButton          — full-width primary button, disabled when no mood selected
```

## Data Flow

```
TodayViewModel (@HiltViewModel, @Inject constructor)
├── todayUiState: StateFlow<TodayUiState>
│     data class TodayUiState(
│       val selectedMood: Mood? = null,
│       val content: String = "",
│       val selectedTagIds: Set<Long> = emptySet(),
│       val availableTags: List<ActivityTag> = emptyList(),
│       val isExistingEntry: Boolean = false,
│       val isSaving: Boolean = false,
│       val hasSaved: Boolean = false
│     )
├── init: launch { seedDefaultTagsIfNeeded() }
│         observeEntryByDate(today) → populate if exists
│         observeAllTags() → available tags
│         if existing entry has tags → getTagsForEntry() → selectedTagIds
├── onMoodSelected(mood: Mood) → uiState.update { copy(selectedMood = mood) }
├── onContentChanged(text: String) → uiState.update { copy(content = text) }
├── onTagToggled(tagId: Long) → uiState.update { toggle in set }
└── onSave() → viewModelScope.launch {
      uiState.update { copy(isSaving = true) }
      try { saveEntry(today, mood, content, tagIds.toList())
            uiState.update { copy(isSaving = false, hasSaved = true) } }
      catch { uiState.update { copy(isSaving = false) }; expose error }
    }
```

## UI Design

### Visual Theme
- Warm, paper-like aesthetic matching DiaryColors (cream background, teal primary)
- Partitioned card style — each section has a tinted header bar and white content area
- Rounded corners (12-16dp), subtle borders, ample padding (16dp)

### DateHeader
- Date in Chinese format: "4月29日 周四"
- Greeting text: "今天感觉怎么样？" (new entry) / "今天的记录" (existing entry)
- Font: headlineSmall for date, titleMedium for greeting

### MoodSelector
- 7 mood options as vertical capsule pills (36dp wide, rounded 18dp)
- Gradient background matching each mood's color
- Emoji icon + tiny label text below
- Arranged horizontally, scrollable with LazyRow
- Selected state: scaled up, border ring in primary color
- Unselected: slightly dimmed/desaturated

### ContentCard
- Card with header bar: "📝 今天的记录" on tinted background
- Body: BasicTextField with placeholder "写下今天的日记..."
- Character counter at bottom-right
- Min height ~120dp

### TagSelector
- Card with header bar: "🏷 活动标签" on secondary-tinted background
- Chips in horizontal FlowRow (wrapping)
- Each chip: colored dot + name, rounded pill shape
- Selected: filled color background
- Unselected: outlined, neutral
- Scrollable if many tags

### SaveButton
- Full width, primary color background (teal #2F6F73)
- White text "保存记录", 48dp height, 12dp rounded
- Disabled state: grey, no click
- Loading state: circular progress indicator
- Feedback: brief "已保存" snackbar or inline checkmark

## States & Edge Cases

| State | Behavior |
|-------|----------|
| **New day, no entry** | All fields empty, mood unselected, save disabled |
| **Existing entry** | Pre-fill mood, content, selected tags. Save updates existing record |
| **No mood selected** | Save button disabled |
| **Empty content, mood selected** | Save allowed (mood-only entry) |
| **Saving** | Button shows spinner, inputs remain editable but clicks debounced |
| **Save success** | Brief checkmark animation, "已保存" text |
| **Save failure** | Snackbar with error message, inputs preserved |
| **Tags empty (first launch)** | Tag selector shows "暂无标签" or hidden |

## Error Handling

- `DiaryRepository.saveEntry()` throws → caught in ViewModel, error message shown via Snackbar
- User input preserved on failure
- No silent failures — every error path shows user-facing message

## Testing

- **Unit tests:** TodayViewModel with fake DiaryRepository
  - init loads existing entry and tags
  - mood selection updates state
  - content input updates state
  - tag toggle adds/removes from set
  - save calls repository with correct params
  - save failure preserves input
  - save disabled when no mood
- **UI tests (optional):** Compose test on MoodSelector and TagSelector interactions

---

## File Structure

```
feature/today/
├── TodayScreen.kt           (MODIFY — replace placeholder)
├── TodayViewModel.kt        (NEW)
├── TodayUiState.kt          (NEW)
└── components/
    ├── MoodSelector.kt      (NEW)
    ├── ContentCard.kt       (NEW)
    └── TagSelector.kt       (NEW)
```
