package com.example.dailydiary.data.repository

import com.example.dailydiary.core.database.dao.ActivityTagDao
import com.example.dailydiary.core.database.dao.DiaryEntryDao
import com.example.dailydiary.core.database.dao.DiaryEntryTagCrossRefDao
import com.example.dailydiary.core.database.SeedDefaults
import com.example.dailydiary.domain.model.ActivityTag
import com.example.dailydiary.domain.model.DiaryEntry
import com.example.dailydiary.domain.model.DiaryEntryTagCrossRef
import com.example.dailydiary.domain.model.DiaryEntryWithTags
import com.example.dailydiary.domain.model.Mood
import com.example.dailydiary.domain.repository.DiaryRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiaryRepositoryImpl @Inject constructor(
    private val diaryEntryDao: DiaryEntryDao,
    private val activityTagDao: ActivityTagDao,
    private val crossRefDao: DiaryEntryTagCrossRefDao
) : DiaryRepository {

    override suspend fun getEntryByDate(date: LocalDate): DiaryEntry? {
        return diaryEntryDao.getByDate(date)
    }

    override fun observeEntryByDate(date: LocalDate): Flow<DiaryEntry?> {
        return diaryEntryDao.observeByDate(date)
    }

    override suspend fun saveEntry(
        date: LocalDate,
        mood: Mood,
        content: String,
        tagIds: List<Long>
    ): DiaryEntry {
        val existing = diaryEntryDao.getByDate(date)
        if (existing != null) {
            existing.copy(
                moodId = mood.id,
                content = content,
                updatedAt = Instant.now()
            ).also { diaryEntryDao.update(it) }

            crossRefDao.deleteByEntryId(existing.id)
            crossRefDao.insertAll(tagIds.map { DiaryEntryTagCrossRef(existing.id, it) })
        } else {
            DiaryEntry(
                entryDate = date,
                moodId = mood.id,
                content = content,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            ).also { diaryEntryDao.insert(it) }

            val entryWithId = diaryEntryDao.getByDate(date)!!
            crossRefDao.insertAll(tagIds.map { DiaryEntryTagCrossRef(entryWithId.id, it) })
        }

        return diaryEntryDao.getByDate(date)!!
    }

    override suspend fun getEntriesInRange(start: LocalDate, end: LocalDate): List<DiaryEntry> {
        return diaryEntryDao.getByDateRange(start, end)
    }

    override suspend fun getEntryWithTags(entryId: Long): DiaryEntryWithTags? {
        val entry = diaryEntryDao.getById(entryId) ?: return null
        val tags = crossRefDao.getTagsForEntry(entryId)
        return DiaryEntryWithTags(entry, tags)
    }

    override suspend fun searchByContent(keyword: String): List<DiaryEntry> {
        return diaryEntryDao.searchByContent(keyword)
    }

    override suspend fun getByMood(moodId: String): List<DiaryEntry> {
        return diaryEntryDao.getByMood(moodId)
    }

    override suspend fun getAllEntries(): List<DiaryEntry> {
        return diaryEntryDao.getAll()
    }

    override suspend fun getTotalEntryCount(): Int {
        return diaryEntryDao.count()
    }

    override suspend fun getEntryCountInRange(start: LocalDate, end: LocalDate): Int {
        return diaryEntryDao.countByDateRange(start, end)
    }

    override suspend fun getRecordedDatesInRange(
        start: LocalDate,
        end: LocalDate
    ): List<LocalDate> {
        return diaryEntryDao.getRecordedDatesInRange(start, end)
    }

    override suspend fun getMoodDistribution(
        start: LocalDate,
        end: LocalDate
    ): List<Pair<Mood, Long>> {
        return diaryEntryDao.getMoodDistribution(start, end).map { mc ->
            Mood.fromId(mc.moodId) to mc.count
        }
    }

    override suspend fun getActiveTags(): List<ActivityTag> {
        return activityTagDao.getActive()
    }

    override fun observeAllTags(): Flow<List<ActivityTag>> {
        return activityTagDao.observeAll()
    }

    override suspend fun getTagsForEntry(entryId: Long): List<ActivityTag> {
        return crossRefDao.getTagsForEntry(entryId)
    }

    override suspend fun seedDefaultTagsIfNeeded() {
        if (activityTagDao.count() == 0) {
            activityTagDao.insertAll(SeedDefaults.defaultTags)
        }
    }
}
