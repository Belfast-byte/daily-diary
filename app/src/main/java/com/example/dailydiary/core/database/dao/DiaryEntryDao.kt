package com.example.dailydiary.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.dailydiary.domain.model.DiaryEntry
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DiaryEntryDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: DiaryEntry): Long

    @Update
    suspend fun update(entry: DiaryEntry)

    @Query("SELECT * FROM diary_entries WHERE id = :id")
    suspend fun getById(id: Long): DiaryEntry?

    @Query("SELECT * FROM diary_entries WHERE entryDate = :date")
    suspend fun getByDate(date: LocalDate): DiaryEntry?

    @Query("SELECT * FROM diary_entries WHERE entryDate = :date")
    fun observeByDate(date: LocalDate): Flow<DiaryEntry?>

    @Query("SELECT * FROM diary_entries WHERE entryDate BETWEEN :start AND :end ORDER BY entryDate DESC")
    suspend fun getByDateRange(start: LocalDate, end: LocalDate): List<DiaryEntry>

    @Query("SELECT * FROM diary_entries WHERE content LIKE '%' || :keyword || '%' ORDER BY entryDate DESC")
    suspend fun searchByContent(keyword: String): List<DiaryEntry>

    @Query("SELECT * FROM diary_entries WHERE moodId = :moodId ORDER BY entryDate DESC")
    suspend fun getByMood(moodId: String): List<DiaryEntry>

    @Query("SELECT * FROM diary_entries ORDER BY entryDate DESC")
    suspend fun getAll(): List<DiaryEntry>

    @Query("SELECT COUNT(*) FROM diary_entries")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM diary_entries WHERE entryDate BETWEEN :start AND :end")
    suspend fun countByDateRange(start: LocalDate, end: LocalDate): Int

    @Query("SELECT DISTINCT entryDate FROM diary_entries WHERE entryDate BETWEEN :start AND :end ORDER BY entryDate ASC")
    suspend fun getRecordedDatesInRange(start: LocalDate, end: LocalDate): List<LocalDate>

    @Query("SELECT moodId, COUNT(*) as count FROM diary_entries WHERE entryDate BETWEEN :start AND :end GROUP BY moodId")
    suspend fun getMoodDistribution(start: LocalDate, end: LocalDate): List<MoodCount>

    data class MoodCount(val moodId: String, val count: Long)
}
