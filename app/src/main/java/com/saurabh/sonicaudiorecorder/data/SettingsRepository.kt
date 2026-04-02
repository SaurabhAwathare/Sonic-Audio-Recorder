package com.saurabh.sonicaudiorecorder.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val AUDIO_FORMAT = stringPreferencesKey("audio_format")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val STORAGE_LOCATION = stringPreferencesKey("storage_location")
        val AUTO_SILENT_MODE = booleanPreferencesKey("auto_silent_mode")
    }

    val audioFormat: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.AUDIO_FORMAT] ?: "WAV"
    }

    val themeMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.THEME_MODE] ?: "System Default"
    }

    val storageLocation: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.STORAGE_LOCATION] ?: "Internal"
    }

    val autoSilentMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.AUTO_SILENT_MODE] ?: false
    }

    suspend fun updateAudioFormat(format: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUDIO_FORMAT] = format
        }
    }

    suspend fun updateThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode
        }
    }

    suspend fun updateStorageLocation(location: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.STORAGE_LOCATION] = location
        }
    }

}
