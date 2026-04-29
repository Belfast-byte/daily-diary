package com.example.dailydiary.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.dailydiary.domain.model.ActivityTag
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityTagDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(tag: ActivityTag): Long

    @Update
    suspend fun update(tag: ActivityTag)

    @Query("SELECT * FROM activity_tags WHERE isArchived = 0 ORDER BY sortOrder ASC")
    suspend fun getActive(): List<ActivityTag>

    @Query("SELECT * FROM activity_tags ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<ActivityTag>>

    @Query("SELECT * FROM activity_tags WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<ActivityTag>

    @Query("SELECT COUNT(*) FROM activity_tags")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(tags: List<ActivityTag>): List<Long>
}
