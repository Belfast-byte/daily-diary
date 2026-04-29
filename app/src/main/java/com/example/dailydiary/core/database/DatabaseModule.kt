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
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideSupportFactory(keyManager: KeyManager): SupportOpenHelperFactory {
        return SupportOpenHelperFactory(keyManager.getOrCreatePassphrase())
    }

    @Provides
    @Singleton
    fun provideDiaryDatabase(
        @ApplicationContext context: Context,
        supportFactory: SupportOpenHelperFactory
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
