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
