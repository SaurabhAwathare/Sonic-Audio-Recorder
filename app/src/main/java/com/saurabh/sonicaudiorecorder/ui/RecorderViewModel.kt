package com.saurabh.sonicaudiorecorder.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audiorecorder01.data.local.RecordingDao
import com.saurabh.sonicaudiorecorder.data.AudioPlayer
import com.saurabh.sonicaudiorecorder.data.RecordingStateManager
import com.saurabh.sonicaudiorecorder.data.RecordingStatus
import com.saurabh.sonicaudiorecorder.data.SettingsRepository
import com.saurabh.sonicaudiorecorder.data.local.RecordingEntity
import com.saurabh.sonicaudiorecorder.service.RecordingService

import com.saurabh.sonicaudiorecorder.data.recovery.RecordingRecoveryManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import kotlin.math.log10

import com.saurabh.sonicaudiorecorder.data.waveform.WaveformRepository

@HiltViewModel
class RecorderViewModel @Inject constructor(
    private val recordingDao: RecordingDao,
    private val audioPlayer: AudioPlayer,
    private val settingsRepository: SettingsRepository,
    private val stateManager: RecordingStateManager,
    private val recoveryManager: RecordingRecoveryManager,
    val waveformRepository: WaveformRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    val isRecording: StateFlow<Boolean> = stateManager.state
        .map { it != RecordingStatus.IDLE }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isPaused: StateFlow<Boolean> = stateManager.state
        .map { it == RecordingStatus.PAUSED }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val recordingStatus: StateFlow<RecordingStatus> = stateManager.state

    private val _activeSessionFormat = MutableStateFlow<String?>(null)

    private var isStartingRecording = false

    private val _lastRecording = MutableStateFlow<RecordingEntity?>(null)
    val lastRecording: StateFlow<RecordingEntity?> = _lastRecording

    val audioFormat: StateFlow<String> = combine(
        settingsRepository.audioFormat.catch { emit("WAV") },
        _activeSessionFormat,
        recordingStatus,
        _lastRecording
    ) { defaultFormat, sessionFormat, status, lastRec ->
        when {
            status != RecordingStatus.IDLE -> sessionFormat ?: defaultFormat
            lastRec != null -> sessionFormat ?: if (lastRec.fileName.endsWith(".m4a", ignoreCase = true)) "M4A" else "WAV"
            else -> defaultFormat
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "WAV")

    val elapsedTime: StateFlow<Long> = stateManager.elapsedTime

    private val _recordings = recordingDao.getAllRecordings()
        .catch { e -> 
            Log.e(TAG, "Error fetching recordings", e)
            emit(emptyList()) 
        }
        .stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
    val recordings: StateFlow<List<RecordingEntity>> = _recordings

    val isPlaying = audioPlayer.isPlaying
    val currentPlayingPath = audioPlayer.currentPlayingPath
    val playbackPosition = audioPlayer.currentPosition
    val playbackDuration = audioPlayer.duration

    private val _amplitudeHistory = MutableStateFlow(List(80) { 0f })
    val amplitudeHistory: StateFlow<List<Float>> = _amplitudeHistory

    // DSP Tuning Parameters
    private var smoothedAmplitude = 0f
    private val smoothingFactor = 0.2f 
    private val maxRawAmplitude = 32767f 

    init {
        viewModelScope.launch(Dispatchers.IO) {
            recoveryManager.recoverInterruptedRecordings()
        }

        viewModelScope.launch {
            audioPlayer.pollPosition().collect()
        }
        
        viewModelScope.launch {
            stateManager.amplitude.collect { raw ->
                val processed = processAmplitude(raw)
                updateHistory(processed)
            }
        }

        viewModelScope.launch {
            stateManager.lastCompletedRecording.collect { recording ->
                if (recording != null) {
                    _lastRecording.value = recording
                    stateManager.clearLastCompletedRecording()
                }
            }
        }

        viewModelScope.launch {
            combine(stateManager.state, _lastRecording) { status, lastRec ->
                if (status == RecordingStatus.IDLE && lastRec == null) {
                    _activeSessionFormat.value = null
                }
            }.collect()
        }
    }

    private fun updateHistory(newAmplitude: Float) {
        _amplitudeHistory.update { current ->
            (current.drop(1) + newAmplitude)
        }
    }

    private fun processAmplitude(raw: Float): Float {
        val normalized = (raw / maxRawAmplitude).coerceIn(0f, 1f)
        val db = if (normalized > 0) 20 * log10(normalized.toDouble()).toFloat() else -100f
        val logScaled = ((db + 60) / 60).coerceIn(0f, 1f)
        val thresholded = if (logScaled < 0.1f) 0f else logScaled
        smoothedAmplitude = (thresholded * smoothingFactor) + (smoothedAmplitude * (1f - smoothingFactor))
        return smoothedAmplitude
    }

    fun startRecording() {
        if (isRecording.value || isStartingRecording) return
        isStartingRecording = true

        audioPlayer.stopPlayback()

        _lastRecording.value = null
        stateManager.clearLastCompletedRecording()
        _amplitudeHistory.value = List(80) { 0f }

        viewModelScope.launch {
            try {
                val format = try {
                    settingsRepository.audioFormat.first()
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading audio format setting", e)
                    "WAV"
                }
                _activeSessionFormat.value = format

                val location = try {
                    settingsRepository.storageLocation.first()
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading storage location setting", e)
                    "Internal"
                }

                val directory = if (location == "External") {
                    context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                } else {
                    context.filesDir
                }

                if (directory == null) {
                    Log.e(TAG, "Storage directory is null")
                    _activeSessionFormat.value = null
                    return@launch
                }

                val extension = if (format == "WAV") "wav" else "m4a"
                val fileName = "REC_${System.currentTimeMillis()}.$extension"
                val file = File(directory, fileName)

                val serviceIntent = Intent(context, RecordingService::class.java).apply {
                    action = RecordingService.ACTION_START
                    putExtra(RecordingService.EXTRA_FILE_PATH, file.absolutePath)
                    putExtra(RecordingService.EXTRA_FORMAT, format)
                }
                ContextCompat.startForegroundService(context, serviceIntent)

            } catch (e: Exception) {
                Log.e(TAG, "Failed to start recording service", e)
                _activeSessionFormat.value = null
            } finally {
                isStartingRecording = false
            }
        }
    }

    fun pauseRecording() {
        if (isRecording.value && !isPaused.value) {
            val serviceIntent = Intent(context, RecordingService::class.java).apply {
                action = RecordingService.ACTION_PAUSE
            }
            context.startService(serviceIntent)
        }
    }

    fun resumeRecording() {
        if (isRecording.value && isPaused.value) {
            val serviceIntent = Intent(context, RecordingService::class.java).apply {
                action = RecordingService.ACTION_RESUME
            }
            context.startService(serviceIntent)
        }
    }

    fun stopRecording() {
        if (!isRecording.value) return
        
        try {
            val serviceIntent = Intent(context, RecordingService::class.java).apply {
                action = RecordingService.ACTION_STOP
            }
            context.startService(serviceIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording service", e)
        }
    }

    fun resetLastRecording() {
        audioPlayer.stopPlayback()
        _lastRecording.value = null
        if (recordingStatus.value == RecordingStatus.IDLE) {
            _activeSessionFormat.value = null
        }
    }

    fun togglePlayback(recording: RecordingEntity) {
        try {
            val file = File(recording.filePath)
            if (file.exists()) {
                audioPlayer.playOrPause(file)
            } else {
                Log.e(TAG, "File does not exist: ${recording.filePath}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Playback error", e)
        }
    }

    fun seekTo(positionMs: Long) {
        audioPlayer.seekTo(positionMs)
    }

    fun shareRecording(recording: RecordingEntity) {
        val file = File(recording.filePath)
        if (!file.exists()) {
            Log.e(TAG, "File not found for sharing: ${recording.filePath}")
            return
        }

        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share recording via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing recording", e)
        }
    }

    fun renameRecording(recording: RecordingEntity, newName: String) {
        viewModelScope.launch {
            try {
                val oldFile = File(recording.filePath)
                val extension = oldFile.extension
                val newFileNameWithExt = if (newName.endsWith(".$extension")) newName else "$newName.$extension"
                val newFile = File(oldFile.parent, newFileNameWithExt)

                if (oldFile.renameTo(newFile)) {
                    val updatedRecording = recording.copy(
                        fileName = newFileNameWithExt,
                        filePath = newFile.absolutePath
                    )
                    recordingDao.updateRecording(updatedRecording)
                    if (_lastRecording.value?.id == recording.id) {
                        _lastRecording.value = updatedRecording
                    }
                } else {
                    Log.e(TAG, "Failed to rename file on disk")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error renaming recording", e)
            }
        }
    }

    fun deleteRecording(recording: RecordingEntity) {
        viewModelScope.launch {
            try {
                if (currentPlayingPath.value == recording.filePath) {
                    audioPlayer.stopPlayback()
                }
                File(recording.filePath).delete()
                recordingDao.deleteRecording(recording)
                if (_lastRecording.value?.id == recording.id) {
                    _lastRecording.value = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting recording", e)
            }
        }
    }

    companion object {
        private const val TAG = "RecorderViewModel"
    }
}
