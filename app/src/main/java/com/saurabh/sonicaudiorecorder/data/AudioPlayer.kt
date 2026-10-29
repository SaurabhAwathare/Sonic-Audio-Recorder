package com.saurabh.sonicaudiorecorder.data

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.saurabh.sonicaudiorecorder.service.PlaybackService
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
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    val player: Player? get() = mediaController

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentPlayingPath = MutableStateFlow<String?>(null)
    val currentPlayingPath: StateFlow<String?> = _currentPlayingPath

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            Log.d(TAG, "Playback state changed: isPlaying = $isPlaying")
        }

        override fun onPlaybackStateChanged(state: Int) {
            val controller = mediaController
            when (state) {
                Player.STATE_READY -> {
                    val dur = controller?.duration ?: 0L
                    _duration.value = if (dur > 0) dur else 0L
                    Log.d(TAG, "Player STATE_READY. Duration: ${_duration.value} ms")
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

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _currentPlayingPath.value = mediaItem?.localConfiguration?.uri?.path
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "Player Error: ${error.message}", error)
        }
    }

    private fun ensureController(onReady: ((MediaController) -> Unit)? = null) {
        mediaController?.let {
            onReady?.invoke(it)
            return
        }

        if (controllerFuture == null) {
            try {
                val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
                val future = MediaController.Builder(context, sessionToken).buildAsync()
                controllerFuture = future
                future.addListener({
                    try {
                        val controller = future.get()
                        mediaController = controller
                        controller.addListener(playerListener)
                        _isPlaying.value = controller.isPlaying
                        val dur = controller.duration
                        if (dur > 0) _duration.value = dur
                        controller.currentMediaItem?.localConfiguration?.uri?.path?.let { path ->
                            _currentPlayingPath.value = path
                        }
                        Log.d(TAG, "MediaController connected successfully")
                        onReady?.invoke(controller)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to connect MediaController", e)
                        controllerFuture = null
                    }
                }, ContextCompat.getMainExecutor(context))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize MediaController Builder", e)
                controllerFuture = null
            }
        } else {
            controllerFuture?.addListener({
                mediaController?.let { onReady?.invoke(it) }
            }, ContextCompat.getMainExecutor(context))
        }
    }

    fun playOrPause(file: File) {
        ensureController { controller ->
            if (_currentPlayingPath.value == file.absolutePath) {
                if (controller.isPlaying) {
                    Log.i(TAG, "Pausing playback: ${file.name}")
                    controller.pause()
                } else {
                    Log.i(TAG, "Resuming playback: ${file.name}")
                    controller.play()
                }
            } else {
                Log.i(TAG, "Starting playback: ${file.absolutePath}")
                val uri = Uri.fromFile(file)
                val mediaItem = MediaItem.fromUri(uri)
                _currentPlayingPath.value = file.absolutePath
                controller.setMediaItem(mediaItem)
                controller.prepare()
                controller.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        Log.d(TAG, "Seeking to: $positionMs ms")
        mediaController?.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    fun pollPosition() = flow {
        while (true) {
            if (_isPlaying.value) {
                _currentPosition.value = mediaController?.currentPosition ?: 0L
            }
            emit(_currentPosition.value)
            delay(500)
        }
    }

    fun stopPlayback() {
        Log.i(TAG, "Stopping playback")
        mediaController?.stop()
        _isPlaying.value = false
        _currentPlayingPath.value = null
        _currentPosition.value = 0L
    }

    fun release() {
        Log.d(TAG, "Releasing MediaController")
        mediaController?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        mediaController = null
        _isPlaying.value = false
        _currentPlayingPath.value = null
        _currentPosition.value = 0L
    }
}
