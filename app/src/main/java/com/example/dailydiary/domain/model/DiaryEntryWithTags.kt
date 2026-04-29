package com.example.dailydiary.domain.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class DiaryEntryWithTags(
    @Embedded val entry: DiaryEntry,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = DiaryEntryTagCrossRef::class,
            parentColumn = "entryId",
            entityColumn = "tagId"
        )
    )
    val tags: List<ActivityTag>
)
