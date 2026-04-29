package com.example.dailydiary.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.dailydiary.domain.model.ActivityTag
import com.example.dailydiary.domain.model.DiaryEntryTagCrossRef

@Dao
interface DiaryEntryTagCrossRefDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(crossRef: DiaryEntryTagCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(crossRefs: List<DiaryEntryTagCrossRef>)

    @Query("DELETE FROM diary_entry_tag_cross_ref WHERE entryId = :entryId")
    suspend fun deleteByEntryId(entryId: Long)

    @Query("SELECT tagId FROM diary_entry_tag_cross_ref WHERE entryId = :entryId")
    suspend fun getTagIdsForEntry(entryId: Long): List<Long>

    @Query("SELECT t.* FROM activity_tags t INNER JOIN diary_entry_tag_cross_ref r ON t.id = r.tagId WHERE r.entryId = :entryId")
    suspend fun getTagsForEntry(entryId: Long): List<ActivityTag>
}
