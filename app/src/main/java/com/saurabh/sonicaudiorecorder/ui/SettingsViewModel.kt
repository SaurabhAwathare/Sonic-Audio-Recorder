package com.saurabh.sonicaudiorecorder.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saurabh.sonicaudiorecorder.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository
) : ViewModel() {

    val audioFormat: StateFlow<String> = repository.audioFormat.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "WAV"
    )

    val themeMode: StateFlow<String> = repository.themeMode.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "System Default"
    )

    val storageLocation: StateFlow<String> = repository.storageLocation.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "Internal"
    )

    val internalStoragePath: String
        get() = repository.getInternalStoragePath()

    val externalStoragePath: String
        get() = repository.getExternalStoragePath()

    fun updateAudioFormat(format: String) {
        viewModelScope.launch { repository.updateAudioFormat(format) }
    }

    fun updateThemeMode(mode: String) {
        viewModelScope.launch { repository.updateThemeMode(mode) }
    }

    fun updateStorageLocation(location: String) {
        viewModelScope.launch { repository.updateStorageLocation(location) }
    }
}
