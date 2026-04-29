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
