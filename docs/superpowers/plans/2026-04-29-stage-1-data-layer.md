# Stage 1: Data Layer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the encrypted local database layer with Room + SQLCipher, including all entities, DAOs, type converters, Android Keystore-backed key management, and the DiaryRepository.

**Architecture:** Room database encrypted with SQLCipher. Database passphrase is generated once, encrypted via Android Keystore AES key, and stored in the app's private files directory. Repository implements the interface defined in `domain/repository/` using DAOs injected via Hilt. All entities live in `domain/model/`.

**Tech Stack:** Kotlin, Room, SQLCipher for Android, Android Keystore, Hilt, Kotlin Coroutines

---

## File Structure

```
app/src/main/java/com/example/dailydiary/
  domain/
    model/
      Mood.kt                          (NEW - mood enum)
      DiaryEntry.kt                    (NEW - Room entity)
      ActivityTag.kt                   (NEW - Room entity)
      DiaryEntryTagCrossRef.kt         (NEW - Room cross-ref entity)
      Attachment.kt                    (NEW - Room entity)
      DiaryEntryWithTags.kt            (NEW - relation class)
    repository/
      DiaryRepository.kt               (NEW - interface)
  data/
    repository/
      DiaryRepositoryImpl.kt           (NEW - implementation)
  core/
    database/
      Converters.kt                    (NEW - Room type converters)
      dao/
        DiaryEntryDao.kt               (NEW - DAO)
        ActivityTagDao.kt              (NEW - DAO)
        DiaryEntryTagCrossRefDao.kt    (NEW - DAO)
      DiaryDatabase.kt                 (NEW - Room database class)
      DatabaseModule.kt                (NEW - Hilt DI module)
    security/
      KeyManager.kt                    (NEW - Keystore key management)
  app/src/androidTest/java/com/example/dailydiary/
    core/
      database/
        dao/
          DiaryEntryDaoTest.kt         (NEW - instrumented DAO test)
          ActivityTagDaoTest.kt        (NEW - instrumented DAO test)
```

---

### Task 1: Mood Enum

**Files:**
- Create: `app/src/main/java/com/example/dailydiary/domain/model/Mood.kt`

- [ ] **Step 1: Write the Mood enum**

```kotlin
package com.example.dailydiary.domain.model

import androidx.compose.ui.graphics.Color

enum class Mood(
    val id: String,
    val label: String,
    val sentimentScore: Int,
    val color: Color
) {
    VERY_HAPPY("very_happy", "很开心", 3, Color(0xFFFFB347)),
    HAPPY("happy", "开心", 2, Color(0xFFFFD700)),
    CALM("calm", "平静", 1, Color(0xFF87CEEB)),
    LOW("low", "低落", -1, Color(0xFFB0C4DE)),
    SAD("sad", "难过", -2, Color(0xFF6A5ACD)),
    ANXIOUS("anxious", "焦虑", -2, Color(0xFFDDA0DD)),
    ANGRY("angry", "生气", -2, Color(0xFFE74C3C));

    companion object {
        fun fromId(id: String): Mood = entries.first { it.id == id }
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL (or environment errors if Android SDK not configured — record explicitly).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/dailydiary/domain/model/Mood.kt
git commit -m "feat: add Mood enum with id, label, sentiment score, and color"
```

---

### Task 2: Room Entities

**Files:**
- Create: `app/src/main/java/com/example/dailydiary/domain/model/DiaryEntry.kt`
- Create: `app/src/main/java/com/example/dailydiary/domain/model/ActivityTag.kt`
- Create: `app/src/main/java/com/example/dailydiary/domain/model/DiaryEntryTagCrossRef.kt`
- Create: `app/src/main/java/com/example/dailydiary/domain/model/Attachment.kt`
- Create: `app/src/main/java/com/example/dailydiary/domain/model/DiaryEntryWithTags.kt`

- [ ] **Step 1: Write DiaryEntry entity**

```kotlin
package com.example.dailydiary.domain.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "diary_entries",
    indices = [Index(value = ["entryDate"], unique = true)]
)
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryDate: LocalDate,
    val moodId: String,
    val content: String = "",
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)
```

- [ ] **Step 2: Write ActivityTag entity**

```kotlin
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
```

- [ ] **Step 3: Write DiaryEntryTagCrossRef entity**

```kotlin
package com.example.dailydiary.domain.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "diary_entry_tag_cross_ref",
    primaryKeys = ["entryId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = DiaryEntry::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ActivityTag::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tagId")]
)
data class DiaryEntryTagCrossRef(
    val entryId: Long,
    val tagId: Long
)
```

- [ ] **Step 4: Write Attachment entity**

```kotlin
package com.example.dailydiary.domain.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = DiaryEntry::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("entryId")]
)
data class Attachment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryId: Long,
    val type: String,
    val localPath: String,
    val createdAt: Instant = Instant.now()
)
```

- [ ] **Step 5: Write DiaryEntryWithTags relation class**

```kotlin
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
```

- [ ] **Step 6: Verify compilation**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL (entities should compile standalone if Room annotation processor runs).

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/example/dailydiary/domain/model/
git commit -m "feat: add Room entities for diary entries, tags, cross-refs, and attachments"
```

---

### Task 3: Room Type Converters

**Files:**
- Create: `app/src/main/java/com/example/dailydiary/core/database/Converters.kt`

- [ ] **Step 1: Write the Converters class**

```kotlin
package com.example.dailydiary.core.database

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    @TypeConverter
    fun fromInstant(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }
}
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/dailydiary/core/database/Converters.kt
git commit -m "feat: add Room type converters for LocalDate and Instant"
```

---

### Task 4: DAOs

**Files:**
- Create: `app/src/main/java/com/example/dailydiary/core/database/dao/DiaryEntryDao.kt`
- Create: `app/src/main/java/com/example/dailydiary/core/database/dao/ActivityTagDao.kt`
- Create: `app/src/main/java/com/example/dailydiary/core/database/dao/DiaryEntryTagCrossRefDao.kt`

- [ ] **Step 1: Write DiaryEntryDao**

```kotlin
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

    data class MoodCount(val moodId: String, val count: Int)
}
```

- [ ] **Step 2: Write ActivityTagDao**

```kotlin
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
    suspend fun insertAll(tags: List<ActivityTag>)
}
```

- [ ] **Step 3: Write DiaryEntryTagCrossRefDao**

```kotlin
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
```

- [ ] **Step 4: Verify compilation**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/dailydiary/core/database/dao/
git commit -m "feat: add Room DAOs for diary entries, tags, and cross-refs"
```

---

### Task 5: Database Key Manager

**Files:**
- Create: `app/src/main/java/com/example/dailydiary/core/security/KeyManager.kt`

- [ ] **Step 1: Write KeyManager**

```kotlin
package com.example.dailydiary.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

class KeyAccessException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

@Singleton
class KeyManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val AES_KEY_ALIAS = "daily_diary_db_aes"
        private const val PASS_FILE = "db_passphrase_v1.enc"
        private const val AES_KEY_SIZE = 256
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12
    }

    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    private val passFile = File(context.filesDir, PASS_FILE)

    fun getOrCreatePassphrase(): ByteArray {
        return if (passFile.exists()) {
            decryptPassphrase(passFile.readBytes())
        } else {
            val passphrase = ByteArray(32)
            kotlin.random.Random.nextBytes(passphrase)
            passFile.writeBytes(encryptPassphrase(passphrase))
            passphrase
        }
    }

    private fun encryptPassphrase(passphrase: ByteArray): ByteArray {
        val key = getOrCreateAesKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(passphrase)
        return iv + encrypted
    }

    private fun decryptPassphrase(data: ByteArray): ByteArray {
        val key = getOrCreateAesKey()
        val iv = data.copyOfRange(0, GCM_IV_LENGTH)
        val encrypted = data.copyOfRange(GCM_IV_LENGTH, data.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return cipher.doFinal(encrypted)
    }

    private fun getOrCreateAesKey(): SecretKey {
        return if (keyStore.containsAlias(AES_KEY_ALIAS)) {
            val entry = keyStore.getEntry(AES_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
                ?: throw KeyAccessException("Failed to load AES key from Keystore")
            entry.secretKey
        } else {
            val generator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val spec = KeyGenParameterSpec.Builder(
                AES_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setKeySize(AES_KEY_SIZE)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
            generator.init(spec)
            generator.generateKey()
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/dailydiary/core/security/KeyManager.kt
git commit -m "feat: add Android Keystore-backed database passphrase manager"
```

---

### Task 6: Room Database

**Files:**
- Create: `app/src/main/java/com/example/dailydiary/core/database/DiaryDatabase.kt`

- [ ] **Step 1: Write DiaryDatabase**

```kotlin
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
```

- [ ] **Step 2: Enable schema export in app/build.gradle.kts**

In `app/build.gradle.kts`, add inside the `defaultConfig { }` block:

```kotlin
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
```

- [ ] **Step 3: Verify compilation**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL. The Room annotation processor should pick up all entities and DAOs.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/dailydiary/core/database/DiaryDatabase.kt app/build.gradle.kts
git commit -m "feat: add encrypted Room database class with schema export"
```

---

### Task 7: Hilt Database Module

**Files:**
- Create: `app/src/main/java/com/example/dailydiary/core/database/DatabaseModule.kt`

- [ ] **Step 1: Write DatabaseModule**

```kotlin
package com.example.dailydiary.core.database

import android.content.Context
import androidx.room.Room
import com.example.dailydiary.core.database.dao.ActivityTagDao
import com.example.dailydiary.core.database.dao.DiaryEntryDao
import com.example.dailydiary.core.database.dao.DiaryEntryTagCrossRefDao
import com.example.dailydiary.core.security.KeyManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.android.database.sqlcipher.SupportFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideSupportFactory(keyManager: KeyManager): SupportFactory {
        return SupportFactory(keyManager.getOrCreatePassphrase())
    }

    @Provides
    @Singleton
    fun provideDiaryDatabase(
        @ApplicationContext context: Context,
        supportFactory: SupportFactory
    ): DiaryDatabase {
        return Room.databaseBuilder(context, DiaryDatabase::class.java, "daily_diary.db")
            .openHelperFactory(supportFactory)
            .build()
    }

    @Provides
    fun provideDiaryEntryDao(db: DiaryDatabase): DiaryEntryDao = db.diaryEntryDao()

    @Provides
    fun provideActivityTagDao(db: DiaryDatabase): ActivityTagDao = db.activityTagDao()

    @Provides
    fun provideDiaryEntryTagCrossRefDao(db: DiaryDatabase): DiaryEntryTagCrossRefDao =
        db.diaryEntryTagCrossRefDao()
}
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL. Hilt annotation processor must resolve all dependency graphs.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/dailydiary/core/database/DatabaseModule.kt
git commit -m "feat: add Hilt DI module for encrypted database and DAOs"
```

---

### Task 8: DiaryRepository Interface and Implementation

**Files:**
- Create: `app/src/main/java/com/example/dailydiary/domain/repository/DiaryRepository.kt`
- Create: `app/src/main/java/com/example/dailydiary/data/repository/DiaryRepositoryImpl.kt`

- [ ] **Step 1: Write repository interface**

```kotlin
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
    suspend fun getMoodDistribution(start: LocalDate, end: LocalDate): List<Pair<Mood, Int>>
    suspend fun getActiveTags(): List<ActivityTag>
    fun observeAllTags(): Flow<List<ActivityTag>>
    suspend fun getTagsForEntry(entryId: Long): List<ActivityTag>
}
```

- [ ] **Step 2: Write repository implementation**

```kotlin
package com.example.dailydiary.data.repository

import com.example.dailydiary.core.database.dao.ActivityTagDao
import com.example.dailydiary.core.database.dao.DiaryEntryDao
import com.example.dailydiary.core.database.dao.DiaryEntryTagCrossRefDao
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
        val entry = if (existing != null) {
            existing.copy(
                moodId = mood.id,
                content = content,
                updatedAt = Instant.now()
            ).also { diaryEntryDao.update(it) }
        } else {
            DiaryEntry(
                entryDate = date,
                moodId = mood.id,
                content = content,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            ).also { diaryEntryDao.insert(it) }
        }

        if (existing != null) {
            crossRefDao.deleteByEntryId(existing.id)
            val existingId = existing.id
            crossRefDao.insertAll(tagIds.map { DiaryEntryTagCrossRef(existingId, it) })
        } else {
            val entryWithId = diaryEntryDao.getByDate(date)!!
            val newId = entryWithId.id
            crossRefDao.insertAll(tagIds.map { DiaryEntryTagCrossRef(newId, it) })
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
    ): List<Pair<Mood, Int>> {
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
}
```

- [ ] **Step 3: Add Hilt binding for DiaryRepository**

Add to `app/src/main/java/com/example/dailydiary/core/database/DatabaseModule.kt`:

```kotlin
import com.example.dailydiary.data.repository.DiaryRepositoryImpl
import com.example.dailydiary.domain.repository.DiaryRepository

// Inside the DatabaseModule object, add:

@Provides
@Singleton
fun provideDiaryRepository(impl: DiaryRepositoryImpl): DiaryRepository = impl
```

- [ ] **Step 4: Verify compilation**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL. Hilt must resolve the interface binding.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/dailydiary/domain/repository/DiaryRepository.kt app/src/main/java/com/example/dailydiary/data/repository/DiaryRepositoryImpl.kt app/src/main/java/com/example/dailydiary/core/database/DatabaseModule.kt
git commit -m "feat: add DiaryRepository interface and encrypted implementation"
```

---

### Task 9: Default Activity Tags Seeding

**Files:**
- Modify: `app/src/main/java/com/example/dailydiary/data/repository/DiaryRepositoryImpl.kt` (add seed logic)
- Create: `app/src/main/java/com/example/dailydiary/core/database/SeedDefaults.kt`

- [ ] **Step 1: Write SeedDefaults**

```kotlin
package com.example.dailydiary.core.database

import com.example.dailydiary.domain.model.ActivityTag

object SeedDefaults {
    val defaultTags = listOf(
        ActivityTag(name = "工作", color = "#FF6B6B", sortOrder = 0),
        ActivityTag(name = "学习", color = "#4ECDC4", sortOrder = 1),
        ActivityTag(name = "运动", color = "#45B7D1", sortOrder = 2),
        ActivityTag(name = "家人", color = "#96CEB4", sortOrder = 3),
        ActivityTag(name = "朋友", color = "#FFEAA7", sortOrder = 4),
        ActivityTag(name = "睡眠", color = "#DDA0DD", sortOrder = 5),
        ActivityTag(name = "饮食", color = "#FF8C69", sortOrder = 6),
        ActivityTag(name = "旅行", color = "#87CEEB", sortOrder = 7)
    )
}
```

- [ ] **Step 2: Add seed initialization to DiaryRepositoryImpl**

Add this method to `DiaryRepositoryImpl`:

```kotlin
    override suspend fun seedDefaultTagsIfNeeded() {
        if (activityTagDao.count() == 0) {
            activityTagDao.insertAll(SeedDefaults.defaultTags)
        }
    }
```

Add to `DiaryRepository` interface:

```kotlin
    suspend fun seedDefaultTagsIfNeeded()
```

- [ ] **Step 3: Verify compilation**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/dailydiary/core/database/SeedDefaults.kt app/src/main/java/com/example/dailydiary/data/repository/DiaryRepositoryImpl.kt app/src/main/java/com/example/dailydiary/domain/repository/DiaryRepository.kt
git commit -m "feat: add default activity tag seeding on first launch"
```

---

### Task 10: DAO Instrumented Tests

**Files:**
- Create: `app/src/androidTest/java/com/example/dailydiary/core/database/dao/DaoTestBase.kt`
- Create: `app/src/androidTest/java/com/example/dailydiary/core/database/dao/DiaryEntryDaoTest.kt`
- Create: `app/src/androidTest/java/com/example/dailydiary/core/database/dao/ActivityTagDaoTest.kt`

- [ ] **Step 1: Write DaoTestBase**

```kotlin
package com.example.dailydiary.core.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.dailydiary.core.database.Converters
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
            .addTypeConverter(Converters())
            .build()
    }

    @After
    fun closeDb() {
        _db?.close()
        _db = null
    }
}
```

- [ ] **Step 2: Write DiaryEntryDaoTest**

```kotlin
package com.example.dailydiary.core.database.dao

import com.example.dailydiary.domain.model.DiaryEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class DiaryEntryDaoTest : DaoTestBase() {

    @Test
    fun insertAndGetByDate() = runTest {
        val entry = DiaryEntry(
            entryDate = LocalDate.of(2026, 4, 29),
            moodId = "happy",
            content = "Today was a good day",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val id = db.diaryEntryDao().insert(entry)
        assertTrue(id > 0)

        val retrieved = db.diaryEntryDao().getByDate(LocalDate.of(2026, 4, 29))
        assertNotNull(retrieved)
        assertEquals("happy", retrieved!!.moodId)
        assertEquals("Today was a good day", retrieved.content)
    }

    @Test(expected = Exception::class)
    fun dateUniquenessConstraint() = runTest {
        val date = LocalDate.of(2026, 4, 29)
        val entry1 = DiaryEntry(entryDate = date, moodId = "happy")
        db.diaryEntryDao().insert(entry1)

        val entry2 = DiaryEntry(entryDate = date, moodId = "sad")
        db.diaryEntryDao().insert(entry2)
    }

    @Test
    fun updateExistingEntry() = runTest {
        val entry = DiaryEntry(
            entryDate = LocalDate.of(2026, 4, 29),
            moodId = "calm",
            content = "Original content"
        )
        val id = db.diaryEntryDao().insert(entry)

        val retrieved = db.diaryEntryDao().getById(id)!!
        retrieved.copy(moodId = "happy", content = "Updated content").also {
            db.diaryEntryDao().update(it)
        }

        val updated = db.diaryEntryDao().getById(id)
        assertEquals("happy", updated!!.moodId)
        assertEquals("Updated content", updated.content)
    }

    @Test
    fun searchByContent() = runTest {
        db.diaryEntryDao().insert(
            DiaryEntry(entryDate = LocalDate.of(2026, 4, 28), moodId = "calm", content = "Worked on the project")
        )
        db.diaryEntryDao().insert(
            DiaryEntry(entryDate = LocalDate.of(2026, 4, 29), moodId = "happy", content = "Completed the feature")
        )

        val results = db.diaryEntryDao().searchByContent("project")
        assertEquals(1, results.size)
        assertEquals("Worked on the project", results[0].content)
    }

    @Test
    fun getByMood() = runTest {
        db.diaryEntryDao().insert(
            DiaryEntry(entryDate = LocalDate.of(2026, 4, 28), moodId = "calm")
        )
        db.diaryEntryDao().insert(
            DiaryEntry(entryDate = LocalDate.of(2026, 4, 29), moodId = "happy")
        )
        db.diaryEntryDao().insert(
            DiaryEntry(entryDate = LocalDate.of(2026, 4, 27), moodId = "happy")
        )

        val happyEntries = db.diaryEntryDao().getByMood("happy")
        assertEquals(2, happyEntries.size)
    }

    @Test
    fun getMoodDistribution() = runTest {
        val start = LocalDate.of(2026, 4, 1)
        val end = LocalDate.of(2026, 4, 30)
        db.diaryEntryDao().insert(
            DiaryEntry(entryDate = LocalDate.of(2026, 4, 1), moodId = "happy")
        )
        db.diaryEntryDao().insert(
            DiaryEntry(entryDate = LocalDate.of(2026, 4, 2), moodId = "happy")
        )
        db.diaryEntryDao().insert(
            DiaryEntry(entryDate = LocalDate.of(2026, 4, 3), moodId = "sad")
        )

        val dist = db.diaryEntryDao().getMoodDistribution(start, end)
        assertEquals(2, dist.size)
        val happyCount = dist.find { it.moodId == "happy" }!!.count
        val sadCount = dist.find { it.moodId == "sad" }!!.count
        assertEquals(2, happyCount)
        assertEquals(1, sadCount)
    }

    @Test
    fun countByDateRange() = runTest {
        db.diaryEntryDao().insert(
            DiaryEntry(entryDate = LocalDate.of(2026, 4, 10), moodId = "calm")
        )
        db.diaryEntryDao().insert(
            DiaryEntry(entryDate = LocalDate.of(2026, 4, 20), moodId = "calm")
        )
        db.diaryEntryDao().insert(
            DiaryEntry(entryDate = LocalDate.of(2026, 5, 1), moodId = "calm")
        )

        val aprilCount = db.diaryEntryDao().countByDateRange(
            LocalDate.of(2026, 4, 1),
            LocalDate.of(2026, 4, 30)
        )
        assertEquals(2, aprilCount)
    }

    @Test
    fun emptySearchReturnsEmpty() = runTest {
        val results = db.diaryEntryDao().searchByContent("nonexistent")
        assertTrue(results.isEmpty())
    }
}
```

- [ ] **Step 3: Write ActivityTagDaoTest**

```kotlin
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
```

- [ ] **Step 4: Run instrumented tests**

```bash
./gradlew :app:connectedDebugAndroidTest --tests "com.example.dailydiary.core.database.dao.*"
```
Expected: All tests PASS. If no device/emulator available, note the limitation and confirm tests compile.

- [ ] **Step 5: Commit**

```bash
git add app/src/androidTest/
git commit -m "test: add instrumented tests for DiaryEntryDao and ActivityTagDao"
```

---

### Task 11: DataStore Preferences Module (Foundation for Settings)

**Files:**
- Create: `app/src/main/java/com/example/dailydiary/core/datastore/AppSettings.kt`

- [ ] **Step 1: Write AppSettings**

```kotlin
package com.example.dailydiary.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

@Singleton
class AppSettings @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val PRIVACY_LOCK_ENABLED = booleanPreferencesKey("privacy_lock_enabled")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_TIME = stringPreferencesKey("reminder_time")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val FIRST_LAUNCH_DONE = booleanPreferencesKey("first_launch_done")
    }

    val privacyLockEnabled: Flow<Boolean> = context.dataStore.data.map {
        it[PRIVACY_LOCK_ENABLED] ?: true
    }

    val reminderEnabled: Flow<Boolean> = context.dataStore.data.map {
        it[REMINDER_ENABLED] ?: false
    }

    val reminderTime: Flow<String> = context.dataStore.data.map {
        it[REMINDER_TIME] ?: "21:00"
    }

    val themeMode: Flow<String> = context.dataStore.data.map {
        it[THEME_MODE] ?: "system"
    }

    val firstLaunchDone: Flow<Boolean> = context.dataStore.data.map {
        it[FIRST_LAUNCH_DONE] ?: false
    }

    suspend fun setPrivacyLockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PRIVACY_LOCK_ENABLED] = enabled }
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { it[REMINDER_ENABLED] = enabled }
    }

    suspend fun setReminderTime(time: String) {
        context.dataStore.edit { it[REMINDER_TIME] = time }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[THEME_MODE] = mode }
    }

    suspend fun setFirstLaunchDone() {
        context.dataStore.edit { it[FIRST_LAUNCH_DONE] = true }
    }
}
```

- [ ] **Step 2: No separate module needed**

`AppSettings` uses `@Singleton` with `@Inject constructor` — Hilt auto-resolves it without a module. A DataStore module will be added later if needed.

- [ ] **Step 3: Verify compilation**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/dailydiary/core/datastore/
git commit -m "feat: add DataStore preferences wrapper for app settings"
```

---

## Stage 1 Validation Checklist

After all tasks complete, verify:

1. `./gradlew :app:compileDebugKotlin` succeeds.
2. `./gradlew :app:connectedDebugAndroidTest` passes all DAO tests (if device/emulator available).
3. All files exist at the paths listed in the file structure above.
4. Hilt dependency graph compiles without errors.
5. Database schema is exported to `app/schemas/`.
6. `DiaryRepository` interface covers all data access patterns needed by stages 2-6.

---

## Next Stage

After Stage 1 is complete and validated, proceed to Stage 2 (Today Entry screen) using the same planning process.
