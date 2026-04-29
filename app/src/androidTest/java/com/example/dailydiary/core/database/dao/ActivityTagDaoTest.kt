package com.example.dailydiary.core.database.dao

import com.example.dailydiary.domain.model.ActivityTag
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityTagDaoTest : DaoTestBase() {

    @Test
    fun insertAndRetrieveTag() = runTest {
        val tag = ActivityTag(name = "Work", color = "#FF0000", sortOrder = 0)
        val id = db.activityTagDao().insert(tag)
        assertTrue(id > 0)

        val tags = db.activityTagDao().getActive()
        assertEquals(1, tags.size)
        assertEquals("Work", tags[0].name)
    }

    @Test
    fun archivedTagsExcludedFromGetActive() = runTest {
        db.activityTagDao().insert(ActivityTag(name = "Active", color = "#FF0000", sortOrder = 0))
        db.activityTagDao().insert(ActivityTag(name = "Archived", color = "#00FF00", sortOrder = 1, isArchived = true))

        val active = db.activityTagDao().getActive()
        assertEquals(1, active.size)
        assertEquals("Active", active[0].name)
    }

    @Test
    fun getByIds() = runTest {
        val id1 = db.activityTagDao().insert(ActivityTag(name = "A", color = "#AAA", sortOrder = 0))
        val id2 = db.activityTagDao().insert(ActivityTag(name = "B", color = "#BBB", sortOrder = 1))
        val id3 = db.activityTagDao().insert(ActivityTag(name = "C", color = "#CCC", sortOrder = 2))

        val result = db.activityTagDao().getByIds(listOf(id1, id3))
        assertEquals(2, result.size)
        assertTrue(result.any { it.name == "A" })
        assertTrue(result.any { it.name == "C" })
    }

    @Test
    fun insertAllBulk() = runTest {
        val tags = listOf(
            ActivityTag(name = "A", color = "#AAA", sortOrder = 0),
            ActivityTag(name = "B", color = "#BBB", sortOrder = 1)
        )
        db.activityTagDao().insertAll(tags)
        assertEquals(2, db.activityTagDao().count())
    }
}
