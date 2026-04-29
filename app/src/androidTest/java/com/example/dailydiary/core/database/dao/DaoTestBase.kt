package com.example.dailydiary.core.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.dailydiary.core.database.DiaryDatabase
import org.junit.After
import org.junit.Before

abstract class DaoTestBase {
    private var _db: DiaryDatabase? = null
    val db: DiaryDatabase get() = _db!!

    @Before
    fun createDb() {
        _db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DiaryDatabase::class.java
        )
            .build()
    }

    @After
    fun closeDb() {
        _db?.close()
        _db = null
    }
}
