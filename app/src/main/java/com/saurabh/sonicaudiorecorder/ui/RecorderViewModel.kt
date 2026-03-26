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
import com.saurabh.sonicaudiorecorder.data.AudioPlayer
import com.saurabh.sonicaudiorecorder.data.AudioRecorder
import com.saurabh.sonicaudiorecorder.data.SettingsRepository
import com.example.audiorecorder01.data.local.RecordingDao
import com.saurabh.sonicaudiorecorder.data.local.RecordingEntity
import com.saurabh.sonicaudiorecorder.service.RecordingService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class RecorderViewModel @Inject constructor(
    private val recordingDao: RecordingDao,
    private val audioPlayer: AudioPlayer,
    private val settingsRepository: SettingsRepository,
    private val recorder: AudioRecorder,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused

    private val _elapsedTime = MutableStateFlow(0L)
    val elapsedTime: StateFlow<Long> = _elapsedTime

    private val _recordings = recordingDao.getAllRecordings()
        .catch { e -> 
            Log.e(TAG, "Error fetching recordings", e)
            emit(emptyList()) 
        }
        .stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
    val recordings: StateFlow<List<RecordingEntity>> = _recordings

    private val _lastRecording = MutableStateFlow<RecordingEntity?>(null)
    val lastRecording: StateFlow<RecordingEntity?> = _lastRecording

    val isPlaying = audioPlayer.isPlaying
    val currentPlayingPath = audioPlayer.currentPlayingPath
    val playbackPosition = audioPlayer.currentPosition
    val playbackDuration = audioPlayer.duration
    
    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude

    private var timerJob: Job? = null
    private var startTimeMillis = 0L
    private var pausedTimeMillis = 0L

    init {
        viewModelScope.launch {
            audioPlayer.pollPosition().collect()
        }
        
        viewModelScope.launch {
            while(true) {
                if (_isRecording.value && !_isPaused.value) {
                    _amplitude.value = recorder.getAmplitude()
                } else {
                    _amplitude.value = 0f
                }
                delay(100)
            }
        }
    }

    fun startRecording() {
        if (_isRecording.value) return
        _lastRecording.value = null

        viewModelScope.launch {
            val format = settingsRepository.audioFormat.first()
            val location = settingsRepository.storageLocation.first()
            val directory = if (location == "External") {
                context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            } else {
                context.filesDir
            }

            if (directory == null) {
                Log.e(TAG, "Storage directory is null")
                return@launch
            }

            val extension = if (format == "WAV") "wav" else "m4a"
            val fileName = "REC_${System.currentTimeMillis()}.$extension"
            val file = File(directory, fileName)

            try {
                _isRecording.value = true
                _isPaused.value = false
                _elapsedTime.value = 0L
                startTimeMillis = System.currentTimeMillis()
                pausedTimeMillis = 0L
                startTimer()
                
                val serviceIntent = Intent(context, RecordingService::class.java).apply {
                    action = RecordingService.ACTION_START
                    putExtra(RecordingService.EXTRA_FILE_PATH, file.absolutePath)
                    putExtra("EXTRA_FORMAT", format)
                }
                ContextCompat.startForegroundService(context, serviceIntent)
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start recording service", e)
                _isRecording.value = false
                _isPaused.value = false
                stopTimer()
            }
        }
    }

    fun pauseRecording() {
        if (_isRecording.value && !_isPaused.value) {
            recorder.pause()
            _isPaused.value = true
            pausedTimeMillis = System.currentTimeMillis()
            stopTimer()
        }
    }

    fun resumeRecording() {
        if (_isRecording.value && _isPaused.value) {
            recorder.resume()
            _isPaused.value = false
            startTimeMillis += (System.currentTimeMillis() - pausedTimeMillis)
            startTimer()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                _elapsedTime.value = System.currentTimeMillis() - startTimeMillis
                delay(100)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    fun stopRecording() {
        if (!_isRecording.value) return
        
        viewModelScope.launch {
            try {
                val serviceIntent = Intent(context, RecordingService::class.java).apply {
                    action = RecordingService.ACTION_STOP
                }
                context.startService(serviceIntent)
                
                delay(500)
                val latest = recordingDao.getAllRecordings().first().firstOrNull()
                _lastRecording.value = latest
                
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping recording service", e)
            } finally {
                _isRecording.value = false
                _isPaused.value = false
                stopTimer()
            }
        }
    }

    fun resetLastRecording() {
        _lastRecording.value = null
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

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
        stopTimer()
    }

    companion object {
        private const val TAG = "RecorderViewModel"
    }
}
