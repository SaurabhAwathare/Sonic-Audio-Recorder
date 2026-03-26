@file:Suppress("AddExplicitTargetToParameterAnnotation", "AddExplicitTargetToParameterAnnotation",
    "AddExplicitTargetToParameterAnnotation", "AddExplicitTargetToParameterAnnotation"
)

package com.saurabh.sonicaudiorecorder.data

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var exoPlayer: ExoPlayer? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentPlayingPath = MutableStateFlow<String?>(null)
    val currentPlayingPath: StateFlow<String?> = _currentPlayingPath

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    fun playOrPause(file: File) {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context).build().apply {
                addListener(object : androidx.media3.common.Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == androidx.media3.common.Player.STATE_READY) {
                            _duration.value = duration
                        }
                        if (state == androidx.media3.common.Player.STATE_ENDED) {
                            _isPlaying.value = false
                            _currentPlayingPath.value = null
                            _currentPosition.value = 0L
                        }
                    }
                })
            }
        }

        if (_currentPlayingPath.value == file.absolutePath) {
            if (_isPlaying.value) {
                exoPlayer?.pause()
            } else {
                exoPlayer?.play()
            }
        } else {
            val uri = Uri.fromFile(file)
            val mediaItem = MediaItem.fromUri(uri)
            exoPlayer?.setMediaItem(mediaItem)
            exoPlayer?.prepare()
            exoPlayer?.play()
            _currentPlayingPath.value = file.absolutePath
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    fun pollPosition() = flow {
        while (true) {
            if (_isPlaying.value) {
                _currentPosition.value = exoPlayer?.currentPosition ?: 0L
            }
            emit(_currentPosition.value)
            delay(500)
        }
    }

    fun stopPlayback() {
        exoPlayer?.stop()
        _isPlaying.value = false
        _currentPlayingPath.value = null
        _currentPosition.value = 0L
    }

    fun release() {
        exoPlayer?.release()
        exoPlayer = null
    }
}
