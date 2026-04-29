package com.example.dailydiary.feature.stats

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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeDiaryRepository
    private lateinit var viewModel: StatsViewModel

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
    fun `init computes total days and streak`() = runTest(testDispatcher) {
        val today = LocalDate.now()
        repository.addEntry(today, "happy")
        repository.addEntry(today.minusDays(1), "calm")
        repository.addEntry(today.minusDays(2), "happy")
        viewModel = StatsViewModel(repository)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(3, state.totalDays)
            assertEquals(3, state.streakDays)
            assertTrue(state.hasEnoughData)
        }
    }

    @Test
    fun `init with no entries shows hasEnoughData false`() = runTest(testDispatcher) {
        viewModel = StatsViewModel(repository)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(0, state.totalDays)
            assertEquals(0, state.streakDays)
            assertFalse(state.hasEnoughData)
        }
    }

    @Test
    fun `init computes top mood`() = runTest(testDispatcher) {
        val today = LocalDate.now()
        repository.addEntry(today, "happy")
        repository.addEntry(today.minusDays(1), "happy")
        repository.addEntry(today.minusDays(2), "sad")
        viewModel = StatsViewModel(repository)
        advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(Mood.HAPPY, state.topMood)
        }
    }
}

class FakeDiaryRepository : DiaryRepository {
    private val entries = mutableListOf<DiaryEntry>()

    fun addEntry(date: LocalDate, moodId: String) {
        entries.add(DiaryEntry(id = entries.size.toLong() + 1, entryDate = date, moodId = moodId, content = "", createdAt = Instant.now(), updatedAt = Instant.now()))
    }

    override suspend fun getEntryByDate(date: LocalDate): DiaryEntry? = entries.firstOrNull { it.entryDate == date }
    override fun observeEntryByDate(date: LocalDate): Flow<DiaryEntry?> = flowOf(entries.firstOrNull { it.entryDate == date })
    override suspend fun saveEntry(date: LocalDate, mood: Mood, content: String, tagIds: List<Long>): DiaryEntry { val e = DiaryEntry(1, date, mood.id, content, Instant.now(), Instant.now()); entries.add(e); return e }
    override suspend fun getEntriesInRange(start: LocalDate, end: LocalDate): List<DiaryEntry> = entries.filter { !it.entryDate.isBefore(start) && !it.entryDate.isAfter(end) }
    override suspend fun getEntryWithTags(entryId: Long): DiaryEntryWithTags? = null
    override suspend fun searchByContent(keyword: String): List<DiaryEntry> = emptyList()
    override suspend fun getByMood(moodId: String): List<DiaryEntry> = entries.filter { it.moodId == moodId }
    override suspend fun getAllEntries(): List<DiaryEntry> = entries
    override suspend fun getTotalEntryCount(): Int = entries.size
    override suspend fun getEntryCountInRange(start: LocalDate, end: LocalDate): Int = entries.count { !it.entryDate.isBefore(start) && !it.entryDate.isAfter(end) }
    override suspend fun getRecordedDatesInRange(start: LocalDate, end: LocalDate): List<LocalDate> = entries.map { it.entryDate }.filter { !it.isBefore(start) && !it.isAfter(end) }
    override suspend fun getMoodDistribution(start: LocalDate, end: LocalDate): List<Pair<Mood, Long>> =
        entries.filter { !it.entryDate.isBefore(start) && !it.entryDate.isAfter(end) }
            .groupBy { it.moodId }.map { (id, list) -> Mood.fromId(id)!! to list.size.toLong() }
    override suspend fun getActiveTags(): List<ActivityTag> = emptyList()
    override fun observeAllTags(): Flow<List<ActivityTag>> = flowOf(emptyList())
    override suspend fun getTagsForEntry(entryId: Long): List<ActivityTag> = emptyList()
    override suspend fun seedDefaultTagsIfNeeded() {}
}
