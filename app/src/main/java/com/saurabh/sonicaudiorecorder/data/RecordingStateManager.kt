package com.saurabh.sonicaudiorecorder.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class RecordingStatus {
    IDLE, RECORDING, PAUSED
}

@Singleton
class RecordingStateManager @Inject constructor() {
    private val _state = MutableStateFlow(RecordingStatus.IDLE)
    val state: StateFlow<RecordingStatus> = _state.asStateFlow()

    private val _elapsedTime = MutableStateFlow(0L)
    val elapsedTime: StateFlow<Long> = _elapsedTime.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    fun updateState(newState: RecordingStatus) {
        _state.value = newState
    }

    fun updateElapsedTime(time: Long) {
        _elapsedTime.value = time
    }

    fun updateAmplitude(amplitude: Float) {
        _amplitude.value = amplitude
    }

    fun reset() {
        _state.value = RecordingStatus.IDLE
        _elapsedTime.value = 0L
        _amplitude.value = 0f
    }
}
