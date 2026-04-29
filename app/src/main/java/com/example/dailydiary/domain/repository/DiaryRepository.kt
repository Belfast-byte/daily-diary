package com.example.dailydiary.domain.repository

import com.example.dailydiary.domain.model.ActivityTag
import com.example.dailydiary.domain.model.DiaryEntry
import com.example.dailydiary.domain.model.DiaryEntryWithTags
import com.example.dailydiary.domain.model.Mood
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface DiaryRepository {
    suspend fun getEntryByDate(date: LocalDate): DiaryEntry?
    fun observeEntryByDate(date: LocalDate): Flow<DiaryEntry?>
    suspend fun saveEntry(
        date: LocalDate,
        mood: Mood,
        content: String = "",
        tagIds: List<Long> = emptyList()
    ): DiaryEntry
    suspend fun getEntriesInRange(start: LocalDate, end: LocalDate): List<DiaryEntry>
    suspend fun getEntryWithTags(entryId: Long): DiaryEntryWithTags?
    suspend fun searchByContent(keyword: String): List<DiaryEntry>
    suspend fun getByMood(moodId: String): List<DiaryEntry>
    suspend fun getAllEntries(): List<DiaryEntry>
    suspend fun getTotalEntryCount(): Int
    suspend fun getEntryCountInRange(start: LocalDate, end: LocalDate): Int
    suspend fun getRecordedDatesInRange(start: LocalDate, end: LocalDate): List<LocalDate>
    suspend fun getMoodDistribution(start: LocalDate, end: LocalDate): List<Pair<Mood, Long>>
    suspend fun getActiveTags(): List<ActivityTag>
    fun observeAllTags(): Flow<List<ActivityTag>>
    suspend fun getTagsForEntry(entryId: Long): List<ActivityTag>
    suspend fun seedDefaultTagsIfNeeded()
}
