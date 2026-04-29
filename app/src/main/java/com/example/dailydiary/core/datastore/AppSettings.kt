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
