package com.saurabh.sonicaudiorecorder.data

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AudioPlayer"

@Singleton
class AudioPlayer @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private var _exoPlayer: ExoPlayer? = null
    val player: Player? get() = _exoPlayer

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentPlayingPath = MutableStateFlow<String?>(null)
    val currentPlayingPath: StateFlow<String?> = _currentPlayingPath

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    private fun ensurePlayer() {
        if (_exoPlayer == null) {
            _exoPlayer = ExoPlayer.Builder(context).build().apply {
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                        Log.d(TAG, "Playback state changed: isPlaying = $isPlaying")
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        when (state) {
                            Player.STATE_READY -> {
                                _duration.value = duration
                                Log.d(TAG, "Player STATE_READY. Duration: $duration ms")
                            }
                            Player.STATE_ENDED -> {
                                _isPlaying.value = false
                                _currentPlayingPath.value = null
                                _currentPosition.value = 0L
                                Log.d(TAG, "Player STATE_ENDED")
                            }
                            Player.STATE_BUFFERING -> Log.d(TAG, "Player STATE_BUFFERING")
                            Player.STATE_IDLE -> Log.d(TAG, "Player STATE_IDLE")
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Log.e(TAG, "Player Error: ${error.message}", error)
                    }
                })
            }
            Log.d(TAG, "ExoPlayer initialized")
        }
    }

    fun playOrPause(file: File) {
        ensurePlayer()
        if (_currentPlayingPath.value == file.absolutePath) {
            if (_isPlaying.value) {
                Log.i(TAG, "Pausing playback: ${file.name}")
                _exoPlayer?.pause()
            } else {
                Log.i(TAG, "Resuming playback: ${file.name}")
                _exoPlayer?.play()
            }
        } else {
            Log.i(TAG, "Starting playback: ${file.absolutePath}")
            val uri = Uri.fromFile(file)
            val mediaItem = MediaItem.fromUri(uri)
            _exoPlayer?.setMediaItem(mediaItem)
            _exoPlayer?.prepare()
            _exoPlayer?.play()
            _currentPlayingPath.value = file.absolutePath
        }
    }

    fun seekTo(positionMs: Long) {
        Log.d(TAG, "Seeking to: $positionMs ms")
        _exoPlayer?.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    fun pollPosition() = flow {
        while (true) {
            if (_isPlaying.value) {
                _currentPosition.value = _exoPlayer?.currentPosition ?: 0L
            }
            emit(_currentPosition.value)
            delay(500)
        }
    }

    fun stopPlayback() {
        Log.i(TAG, "Stopping playback")
        _exoPlayer?.stop()
        _isPlaying.value = false
        _currentPlayingPath.value = null
        _currentPosition.value = 0L
    }

    fun release() {
        Log.d(TAG, "Releasing ExoPlayer")
        _exoPlayer?.release()
        _exoPlayer = null
    }
}
