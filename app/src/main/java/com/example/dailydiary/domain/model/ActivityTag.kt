package com.example.dailydiary.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_tags")
data class ActivityTag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: String,
    val sortOrder: Int,
    val isArchived: Boolean = false
)
