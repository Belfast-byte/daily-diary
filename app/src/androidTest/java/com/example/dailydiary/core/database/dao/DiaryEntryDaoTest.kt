package com.example.dailydiary.core.database.dao

import com.example.dailydiary.domain.model.DiaryEntry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class DiaryEntryDaoTest : DaoTestBase() {

    @Test
    fun insertAndGetByDate() = runTest {
        val entry = DiaryEntry(
            entryDate = LocalDate.of(2026, 4, 29),
            moodId = "happy",
            content = "Today was a good day",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val id = db.diaryEntryDao().insert(entry)
        assertTrue(id > 0)

        val retrieved = db.diaryEntryDao().getByDate(LocalDate.of(2026, 4, 29))
        assertNotNull(retrieved)
        assertEquals("happy", retrieved!!.moodId)
        assertEquals("Today was a good day", retrieved.content)
    }

    @Test(expected = Exception::class)
    fun dateUniquenessConstraint() = runTest {
        val date = LocalDate.of(2026, 4, 29)
        val entry1 = DiaryEntry(entryDate = date, moodId = "happy")
        db.diaryEntryDao().insert(entry1)

        val entry2 = DiaryEntry(entryDate = date, moodId = "sad")
        db.diaryEntryDao().insert(entry2)
    }

    @Test
    fun updateExistingEntry() = runTest {
        val entry = DiaryEntry(
            entryDate = LocalDate.of(2026, 4, 29),
            moodId = "calm",
            content = "Original content"
        )
        val id = db.diaryEntryDao().insert(entry)

        val retrieved = db.diaryEntryDao().getById(id)!!
        retrieved.copy(moodId = "happy", content = "Updated content").also {
            db.diaryEntryDao().update(it)
        }

        val updated = db.diaryEntryDao().getById(id)
        assertEquals("happy", updated!!.moodId)
        assertEquals("Updated content", updated.content)
    }

    @Test
    fun searchByContent() = runTest {
        db.diaryEntryDao().insert(
            DiaryEntry(entryDate = LocalDate.of(2026, 4, 28), moodId = "calm", content = "Worked on the project")
        )
        db.diaryEntryDao().insert(
            DiaryEntry(entryDate = LocalDate.of(2026, 4, 29), moodId = "happy", content = "Completed the feature")
        )

        val results = db.diaryEntryDao().searchByContent("project")
        assertEquals(1, results.size)
        assertEquals("Worked on the project", results[0].content)
    }

    @Test
    fun getByMood() = runTest {
        db.diaryEntryDao().insert(
            DiaryEntry(entryDate = LocalDate.of(2026, 4, 28), moodId = "calm")
        )
        db.diaryEntryDao().insert(
            DiaryEntry(entryDate = LocalDate.of(2026, 4, 29), moodId = "happy")
        )
        db.diaryEntryDao().insert(
            DiaryEntry(entryDate = LocalDate.of(2026, 4, 27), moodId = "happy")
        )

        val happyEntries = db.diaryEntryDao().getByMood("happy")
        assertEquals(2, happyEntries.size)
    }

    @Test
    fun getMoodDistribution() = runTest {
        val start = LocalDate.of(2026, 4, 1)
        val end = LocalDate.of(2026, 4, 30)
        db.diaryEntryDao().insert(
            DiaryEntry(entryDate = LocalDate.of(2026, 4, 1), moodId = "happy")
        )
        db.diaryEntryDao().insert(
            DiaryEntry(entryDate = LocalDate.of(2026, 4, 2), moodId = "happy")
        )
        db.diaryEntryDao().insert(
            DiaryEntry(entryDate = LocalDate.of(2026, 4, 3), moodId = "sad")
        )

        val dist = db.diaryEntryDao().getMoodDistribution(start, end)
        assertEquals(2, dist.size)
        val moodCounts = dist.associate { it.moodId to it.count }
        assertEquals(2L, moodCounts["happy"])
        assertEquals(1L, moodCounts["sad"])
    }

    @Test
    fun countByDateRange() = runTest {
        db.diaryEntryDao().insert(
            DiaryEntry(entryDate = LocalDate.of(2026, 4, 10), moodId = "calm")
        )
        db.diaryEntryDao().insert(
            DiaryEntry(entryDate = LocalDate.of(2026, 4, 20), moodId = "calm")
        )
        db.diaryEntryDao().insert(
            DiaryEntry(entryDate = LocalDate.of(2026, 5, 1), moodId = "calm")
        )

        val aprilCount = db.diaryEntryDao().countByDateRange(
            LocalDate.of(2026, 4, 1),
            LocalDate.of(2026, 4, 30)
        )
        assertEquals(2, aprilCount)
    }

    @Test
    fun emptySearchReturnsEmpty() = runTest {
        val results = db.diaryEntryDao().searchByContent("nonexistent")
        assertTrue(results.isEmpty())
    }
}
