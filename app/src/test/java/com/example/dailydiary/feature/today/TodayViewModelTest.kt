package com.example.dailydiary.feature.today

import app.cash.turbine.test
import com.example.dailydiary.domain.model.ActivityTag
import com.example.dailydiary.domain.model.DiaryEntry
import com.example.dailydiary.domain.model.DiaryEntryWithTags
import com.example.dailydiary.domain.model.Mood
import com.example.dailydiary.domain.repository.DiaryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
        Dispatchers.setMain(testDispatcher)
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
        testDispatcher.scheduler.advanceUntilIdle()

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
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel = TodayViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

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
            // skip intermediate isSaving=true state
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
    private val entryDateFlows = mutableMapOf<LocalDate, MutableStateFlow<DiaryEntry?>>()
    val entryTags = mutableMapOf<LocalDate, List<ActivityTag>>()
    var shouldFail = false

    var lastSavedDate: LocalDate? = null
    var lastSavedMood: Mood? = null
    var lastSavedContent: String? = null
    var lastSavedTagIds: List<Long>? = null

    fun setEntryForDate(date: LocalDate, entry: DiaryEntry) {
        entriesByDate[date] = entry
        entryDateFlows.getOrPut(date) { MutableStateFlow(null) }.value = entry
    }

    override suspend fun getEntryByDate(date: LocalDate): DiaryEntry? = entriesByDate[date]
    override fun observeEntryByDate(date: LocalDate): Flow<DiaryEntry?> =
        entryDateFlows.getOrPut(date) { MutableStateFlow(entriesByDate[date]) }

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
