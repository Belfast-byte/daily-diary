# Stage 2: Today Entry Screen Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the TodayScreen where users select a mood (required), optionally write diary content and pick activity tags, then save — completing the core recording loop. One entry per day.

**Architecture:** @HiltViewModel exposes StateFlow<TodayUiState>, observes today's entry and available tags from DiaryRepository, and handles save with upsert logic. Screen composes stateless components (MoodSelector, ContentCard, TagSelector) driven by callbacks from the ViewModel.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Hilt ViewModel, Coroutines, StateFlow, JUnit + Turbine

---

## File Structure

```
app/src/main/java/com/example/dailydiary/
  feature/today/
    TodayScreen.kt                    (MODIFY — replace placeholder)
    TodayViewModel.kt                 (NEW)
    TodayUiState.kt                   (NEW)
    components/
      MoodSelector.kt                 (NEW)
      ContentCard.kt                  (NEW)
      TagSelector.kt                  (NEW)
app/src/test/java/com/example/dailydiary/
  feature/today/
    TodayViewModelTest.kt             (NEW)
gradle/
  libs.versions.toml                  (MODIFY — add 2 library entries)
app/build.gradle.kts                  (MODIFY — add 2 dependencies)
```

---

### Task 1: Add ViewModel Dependencies

**Files:**
- Modify: `gradle/libs.versions.toml` — add library entries
- Modify: `app/build.gradle.kts` — add implementation deps

- [ ] **Step 1: Add library entries to version catalog**

In `gradle/libs.versions.toml`, add after line 42 (`hilt-compiler`):

```toml
hilt-navigation-compose = { module = "androidx.hilt:hilt-navigation-compose", version = "1.2.0" }
lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
```

- [ ] **Step 2: Add dependencies to app/build.gradle.kts**

In `app/build.gradle.kts`, add after line 60 (`implementation(libs.hilt.android)`):

```kotlin
implementation(libs.hilt.navigation.compose)
implementation(libs.lifecycle.viewmodel.compose)
```

- [ ] **Step 3: Add test dependencies to app/build.gradle.kts**

In `app/build.gradle.kts`, add after line 70 (`testImplementation(libs.junit)`):

```kotlin
testImplementation("app.cash.turbine:turbine:1.2.0")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
```

- [ ] **Step 4: Verify compilation**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "build: add Hilt ViewModel and turbine dependencies"
```

---

### Task 2: TodayUiState Data Class

**Files:**
- Create: `app/src/main/java/com/example/dailydiary/feature/today/TodayUiState.kt`

- [ ] **Step 1: Write TodayUiState**

```kotlin
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
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/dailydiary/feature/today/TodayUiState.kt
git commit -m "feat: add TodayUiState data class"
```

---

### Task 3: TodayViewModel with Unit Tests (TDD)

**Files:**
- Create: `app/src/test/java/com/example/dailydiary/feature/today/TodayViewModelTest.kt`
- Create: `app/src/main/java/com/example/dailydiary/feature/today/TodayViewModel.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.example.dailydiary.feature.today

import app.cash.turbine.test
import com.example.dailydiary.domain.model.ActivityTag
import com.example.dailydiary.domain.model.DiaryEntry
import com.example.dailydiary.domain.model.DiaryEntryWithTags
import com.example.dailydiary.domain.model.Mood
import com.example.dailydiary.domain.repository.DiaryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeDiaryRepository
    private lateinit var viewModel: TodayViewModel

    @Before
    fun setUp() {
        repository = FakeDiaryRepository()
        viewModel = TodayViewModel(repository)
    }

    @Test
    fun `init loads available tags via Flow`() = runTest(testDispatcher) {
        val tags = listOf(
            ActivityTag(id = 1, name = "Work", color = "#FF0000", sortOrder = 0),
            ActivityTag(id = 2, name = "Study", color = "#00FF00", sortOrder = 1)
        )
        repository.tagsFlow.value = tags

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(tags, state.availableTags)
        }
    }

    @Test
    fun `init loads existing entry for today and marks isExistingEntry`() = runTest(testDispatcher) {
        val today = LocalDate.now()
        val existing = DiaryEntry(
            id = 1,
            entryDate = today,
            moodId = "happy",
            content = "Hello",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        repository.setEntryForDate(today, existing)
        repository.entryTags[today] = emptyList()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(Mood.HAPPY, state.selectedMood)
            assertEquals("Hello", state.content)
            assertTrue(state.isExistingEntry)
        }
    }

    @Test
    fun `onMoodSelected updates selectedMood in state`() = runTest(testDispatcher) {
        viewModel.uiState.test {
            awaitItem() // skip initial

            viewModel.onMoodSelected(Mood.CALM)
            val state = awaitItem()
            assertEquals(Mood.CALM, state.selectedMood)
        }
    }

    @Test
    fun `onContentChanged updates content in state`() = runTest(testDispatcher) {
        viewModel.uiState.test {
            awaitItem() // skip initial

            viewModel.onContentChanged("new content")
            val state = awaitItem()
            assertEquals("new content", state.content)
        }
    }

    @Test
    fun `onTagToggled adds and removes from selectedTagIds`() = runTest(testDispatcher) {
        viewModel.uiState.test {
            awaitItem() // skip initial

            viewModel.onTagToggled(1L)
            assertEquals(setOf(1L), awaitItem().selectedTagIds)

            viewModel.onTagToggled(2L)
            assertEquals(setOf(1L, 2L), awaitItem().selectedTagIds)

            viewModel.onTagToggled(1L)
            assertEquals(setOf(2L), awaitItem().selectedTagIds)
        }
    }

    @Test
    fun `save calls repository with correct params and sets hasSaved`() = runTest(testDispatcher) {
        viewModel.uiState.test {
            awaitItem() // skip initial

            viewModel.onMoodSelected(Mood.HAPPY)
            awaitItem()
            viewModel.onContentChanged("test content")
            awaitItem()
            viewModel.onTagToggled(1L)
            awaitItem()

            viewModel.onSave()
            // skip intermediate isSaving=true state
            skipItems(1)

            val state = awaitItem()
            assertTrue(state.hasSaved)
            assertFalse(state.isSaving)
            assertNotNull(repository.lastSavedDate)
            assertEquals(Mood.HAPPY, repository.lastSavedMood)
            assertEquals("test content", repository.lastSavedContent)
            assertEquals(listOf(1L), repository.lastSavedTagIds)
        }
    }

    @Test
    fun `save failure sets errorMessage and clears isSaving`() = runTest(testDispatcher) {
        repository.shouldFail = true
        viewModel.uiState.test {
            awaitItem() // skip initial

            viewModel.onMoodSelected(Mood.HAPPY)
            awaitItem()

            viewModel.onSave()
            // skip intermediate isSaving=true
            skipItems(1)

            val state = awaitItem()
            assertNotNull(state.errorMessage)
            assertFalse(state.isSaving)
            assertFalse(state.hasSaved)
        }
    }
}

class FakeDiaryRepository : DiaryRepository {
    val tagsFlow = MutableStateFlow<List<ActivityTag>>(emptyList())
    private val entriesByDate = mutableMapOf<LocalDate, DiaryEntry>()
    val entryTags = mutableMapOf<LocalDate, List<ActivityTag>>()
    var shouldFail = false

    var lastSavedDate: LocalDate? = null
    var lastSavedMood: Mood? = null
    var lastSavedContent: String? = null
    var lastSavedTagIds: List<Long>? = null

    fun setEntryForDate(date: LocalDate, entry: DiaryEntry) {
        entriesByDate[date] = entry
    }

    override suspend fun getEntryByDate(date: LocalDate): DiaryEntry? = entriesByDate[date]
    override fun observeEntryByDate(date: LocalDate): Flow<DiaryEntry?> =
        flowOf(entriesByDate[date])

    override suspend fun saveEntry(date: LocalDate, mood: Mood, content: String, tagIds: List<Long>): DiaryEntry {
        if (shouldFail) throw RuntimeException("test failure")
        lastSavedDate = date
        lastSavedMood = mood
        lastSavedContent = content
        lastSavedTagIds = tagIds
        return DiaryEntry(
            id = 1, entryDate = date, moodId = mood.id, content = content,
            createdAt = Instant.now(), updatedAt = Instant.now()
        )
    }

    override suspend fun getEntriesInRange(start: LocalDate, end: LocalDate): List<DiaryEntry> = emptyList()
    override suspend fun getEntryWithTags(entryId: Long): DiaryEntryWithTags? = null
    override suspend fun searchByContent(keyword: String): List<DiaryEntry> = emptyList()
    override suspend fun getByMood(moodId: String): List<DiaryEntry> = emptyList()
    override suspend fun getAllEntries(): List<DiaryEntry> = emptyList()
    override suspend fun getTotalEntryCount(): Int = 0
    override suspend fun getEntryCountInRange(start: LocalDate, end: LocalDate): Int = 0
    override suspend fun getRecordedDatesInRange(start: LocalDate, end: LocalDate): List<LocalDate> = emptyList()
    override suspend fun getMoodDistribution(start: LocalDate, end: LocalDate): List<Pair<Mood, Long>> = emptyList()
    override suspend fun getActiveTags(): List<ActivityTag> = emptyList()
    override fun observeAllTags(): Flow<List<ActivityTag>> = tagsFlow
    override suspend fun getTagsForEntry(entryId: Long): List<ActivityTag> = emptyList()
    override suspend fun seedDefaultTagsIfNeeded() {}
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :app:testDebugUnitTest --tests "com.example.dailydiary.feature.today.TodayViewModelTest"
```
Expected: FAIL — TodayViewModel class not found.

- [ ] **Step 3: Write TodayViewModel implementation**

```kotlin
package com.example.dailydiary.feature.today

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
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val repository: DiaryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedDefaultTagsIfNeeded()
        }

        val today = LocalDate.now()

        viewModelScope.launch {
            repository.observeEntryByDate(today).collect { entry ->
                _uiState.update { state ->
                    if (entry != null) {
                        state.copy(
                            selectedMood = Mood.fromId(entry.moodId),
                            content = entry.content,
                            isExistingEntry = true
                        )
                    } else state
                }
            }
        }

        viewModelScope.launch {
            repository.observeAllTags().collect { tags ->
                _uiState.update { it.copy(availableTags = tags) }
            }
        }

        viewModelScope.launch {
            val entry = repository.getEntryByDate(today)
            if (entry != null) {
                val tags = repository.getTagsForEntry(entry.id)
                _uiState.update {
                    it.copy(selectedTagIds = tags.map { t -> t.id }.toSet())
                }
            }
        }
    }

    fun onMoodSelected(mood: Mood) {
        _uiState.update { it.copy(selectedMood = mood) }
    }

    fun onContentChanged(text: String) {
        _uiState.update { it.copy(content = text) }
    }

    fun onTagToggled(tagId: Long) {
        _uiState.update { state ->
            val newSet = state.selectedTagIds.toMutableSet()
            if (newSet.contains(tagId)) newSet.remove(tagId) else newSet.add(tagId)
            state.copy(selectedTagIds = newSet)
        }
    }

    fun onSave() {
        val state = _uiState.value
        val mood = state.selectedMood ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                repository.saveEntry(
                    date = LocalDate.now(),
                    mood = mood,
                    content = state.content,
                    tagIds = state.selectedTagIds.toList()
                )
                _uiState.update { it.copy(isSaving = false, hasSaved = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = e.message ?: "保存失败"
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
./gradlew :app:testDebugUnitTest --tests "com.example.dailydiary.feature.today.TodayViewModelTest"
```
Expected: All 7 tests PASS.

- [ ] **Step 5: Verify full compilation**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add app/src/test/java/com/example/dailydiary/feature/today/TodayViewModelTest.kt app/src/main/java/com/example/dailydiary/feature/today/TodayViewModel.kt
git commit -m "feat: add TodayViewModel with unit tests"
```

---

### Task 4: MoodSelector Component

**Files:**
- Create: `app/src/main/java/com/example/dailydiary/feature/today/components/MoodSelector.kt`

- [ ] **Step 1: Write MoodSelector**

```kotlin
package com.example.dailydiary.feature.today.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dailydiary.domain.model.Mood

@Composable
fun MoodSelector(
    moods: List<Mood>,
    selectedMood: Mood?,
    onMoodSelected: (Mood) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "今天感觉怎么样？",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(moods) { mood ->
                MoodCapsule(
                    mood = mood,
                    isSelected = mood == selectedMood,
                    onClick = { onMoodSelected(mood) }
                )
            }
        }
    }
}

@Composable
private fun MoodCapsule(
    mood: Mood,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderModifier = if (isSelected) {
        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(18.dp))
    } else {
        Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
    }

    Surface(
        modifier = Modifier
            .width(52.dp)
            .clip(RoundedCornerShape(18.dp))
            .then(borderModifier)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = mood.color.copy(alpha = if (isSelected) 0.25f else 0.10f)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)
        ) {
            Text(
                text = mood.label.take(1),
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = mood.label,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                lineHeight = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/dailydiary/feature/today/components/MoodSelector.kt
git commit -m "feat: add MoodSelector component with vertical capsule pills"
```

---

### Task 5: ContentCard Component

**Files:**
- Create: `app/src/main/java/com/example/dailydiary/feature/today/components/ContentCard.kt`

- [ ] **Step 1: Write ContentCard**

```kotlin
package com.example.dailydiary.feature.today.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ContentCard(
    content: String,
    onContentChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            // Header bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                        RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "📝 今天的记录",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Content area
            OutlinedTextField(
                value = content,
                onValueChange = onContentChanged,
                placeholder = {
                    Text(
                        text = "写下今天的日记...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f)
                ),
                textStyle = MaterialTheme.typography.bodyLarge
            )

            // Character counter
            Text(
                text = "${content.length} / 2000",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.End)
                    .padding(end = 16.dp, bottom = 10.dp)
            )
        }
    }
}
```

**Note:** The background import needs `import androidx.compose.foundation.background` in addition to the shown code. The full import list is:

```kotlin
package com.example.dailydiary.feature.today.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/dailydiary/feature/today/components/ContentCard.kt
git commit -m "feat: add ContentCard component with header bar and character counter"
```

---

### Task 6: TagSelector Component

**Files:**
- Create: `app/src/main/java/com/example/dailydiary/feature/today/components/TagSelector.kt`

- [ ] **Step 1: Write TagSelector**

```kotlin
package com.example.dailydiary.feature.today.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dailydiary.domain.model.ActivityTag

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagSelector(
    tags: List<ActivityTag>,
    selectedTagIds: Set<Long>,
    onTagToggled: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            // Header bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f),
                        RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "🏷 活动标签",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            // Tag chips
            if (tags.isEmpty()) {
                Text(
                    text = "暂无标签，请先在设置中添加",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                FlowRow(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                ) {
                    tags.forEach { tag ->
                        TagChip(
                            tag = tag,
                            isSelected = tag.id in selectedTagIds,
                            onClick = { onTagToggled(tag.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TagChip(
    tag: ActivityTag,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val chipColor = try {
        Color(android.graphics.Color.parseColor(tag.color))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val bgColor = if (isSelected) chipColor.copy(alpha = 0.15f)
    else MaterialTheme.colorScheme.surface

    val borderColor = if (isSelected) chipColor.copy(alpha = 0.5f)
    else MaterialTheme.colorScheme.outlineVariant

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(chipColor, RoundedCornerShape(4.dp))
            )
            Text(
                text = tag.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) chipColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/dailydiary/feature/today/components/TagSelector.kt
git commit -m "feat: add TagSelector component with colored chip multi-select"
```

---

### Task 7: Rewrite TodayScreen

**Files:**
- Modify: `app/src/main/java/com/example/dailydiary/feature/today/TodayScreen.kt`

- [ ] **Step 1: Write the full TodayScreen**

```kotlin
package com.example.dailydiary.feature.today

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.dailydiary.domain.model.Mood
import com.example.dailydiary.feature.today.components.ContentCard
import com.example.dailydiary.feature.today.components.MoodSelector
import com.example.dailydiary.feature.today.components.TagSelector
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun TodayScreen(
    viewModel: TodayViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Error handling
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val today = LocalDate.now()
    val dateText = remember {
        val dayOfWeek = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINESE)
        "${today.monthValue}月${today.dayOfMonth}日 $dayOfWeek"
    }

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
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Date header
            Text(
                text = dateText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )

            if (uiState.isExistingEntry) {
                Text(
                    text = "继续编辑今天的记录",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mood selector
            MoodSelector(
                moods = Mood.entries,
                selectedMood = uiState.selectedMood,
                onMoodSelected = viewModel::onMoodSelected
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Content card
            ContentCard(
                content = uiState.content,
                onContentChanged = viewModel::onContentChanged
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tag selector
            TagSelector(
                tags = uiState.availableTags,
                selectedTagIds = uiState.selectedTagIds,
                onTagToggled = viewModel::onTagToggled
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Save button
            Button(
                onClick = { viewModel.onSave() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = uiState.selectedMood != null && !uiState.isSaving,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                )
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else if (uiState.hasSaved) {
                    Text(
                        text = "✓ 已保存",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        text = "保存记录",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run ViewModel tests to ensure nothing broken**

```bash
./gradlew :app:testDebugUnitTest --tests "com.example.dailydiary.feature.today.TodayViewModelTest"
```
Expected: All 7 tests PASS.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/dailydiary/feature/today/TodayScreen.kt
git commit -m "feat: implement TodayScreen with mood, content, tag selection and save"
```

---

## Stage 2 Validation Checklist

After all tasks complete, verify:

1. `./gradlew :app:compileDebugKotlin` succeeds.
2. `./gradlew :app:testDebugUnitTest --tests "com.example.dailydiary.feature.today.TodayViewModelTest"` — all 7 tests pass.
3. App launches and TodayScreen shows mood selector, content card, tag selector, save button.
4. Mood selection enables save button; no mood = disabled.
5. Tag chips toggle on/off with visual feedback.
6. Save persists and shows "已保存" state.
7. Re-opening app shows existing entry (mood pre-selected, content filled).

---

## Next Stage

After Stage 2 is complete and validated, proceed to Stage 3 (Calendar & History) using the same planning process.
