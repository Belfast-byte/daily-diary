package com.example.dailydiary.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.dailydiary.core.database.dao.ActivityTagDao
import com.example.dailydiary.core.database.dao.DiaryEntryDao
import com.example.dailydiary.core.database.dao.DiaryEntryTagCrossRefDao
import com.example.dailydiary.domain.model.ActivityTag
import com.example.dailydiary.domain.model.Attachment
import com.example.dailydiary.domain.model.DiaryEntry
import com.example.dailydiary.domain.model.DiaryEntryTagCrossRef

@Database(
    entities = [
        DiaryEntry::class,
        ActivityTag::class,
        DiaryEntryTagCrossRef::class,
        Attachment::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class DiaryDatabase : RoomDatabase() {
    abstract fun diaryEntryDao(): DiaryEntryDao
    abstract fun activityTagDao(): ActivityTagDao
    abstract fun diaryEntryTagCrossRefDao(): DiaryEntryTagCrossRefDao
}
