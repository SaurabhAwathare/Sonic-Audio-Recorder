package com.saurabh.sonicaudiorecorder.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.audiorecorder01.data.local.RecordingDao
import com.saurabh.sonicaudiorecorder.MainActivity
import com.saurabh.sonicaudiorecorder.R
import com.saurabh.sonicaudiorecorder.data.AudioRecorder
import com.saurabh.sonicaudiorecorder.data.AudioStateManager
import com.saurabh.sonicaudiorecorder.data.RecordingStateManager
import com.saurabh.sonicaudiorecorder.data.RecordingStatus
import com.saurabh.sonicaudiorecorder.data.SettingsRepository
import com.saurabh.sonicaudiorecorder.data.local.RecordingEntity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

private const val TAG = "RecordingService"

@AndroidEntryPoint
class RecordingService : Service(), AudioRecorder.RecordingErrorListener {

    @Inject
    lateinit var audioRecorder: AudioRecorder

    @Inject
    lateinit var recordingDao: RecordingDao

    @Inject
    lateinit var stateManager: RecordingStateManager

    @Inject
    lateinit var audioStateManager: AudioStateManager

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Volatile
    private var activeSessionId = 0L

    private var currentFile: File? = null
    private var startTimeMillis: Long = 0L
    private var totalPausedTime: Long = 0L
    private var lastPauseTimestamp: Long = 0L
    private var isPaused = false
    private var currentRecordingId: Int? = null

    @Volatile
    private var isStopping = false

    private var lastStartId = -1

    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Log.w(TAG, "Audio focus lost, pausing recording")
                pauseRecording()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d(TAG, "Audio focus gained")
            }
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private val updateTimerRunnable = object : Runnable {
        override fun run() {
            val elapsedTime = if (isPaused) {
                lastPauseTimestamp - startTimeMillis - totalPausedTime
            } else {
                SystemClock.elapsedRealtime() - startTimeMillis - totalPausedTime
            }

            stateManager.updateElapsedTime(elapsedTime)

            if (!isPaused) {
                stateManager.updateAmplitude(audioRecorder.getAmplitude())
            }
            handler.postDelayed(this, 100)
        }
    }

    companion object {
        const val CHANNEL_ID = "recording_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val EXTRA_FILE_PATH = "EXTRA_FILE_PATH"
        const val EXTRA_FORMAT = "EXTRA_FORMAT"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "RecordingService Created")
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
        audioRecorder.setErrorListener(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        this.lastStartId = startId
        Log.d(TAG, "onStartCommand: action = ${intent?.action}, startId = $startId")
        if (intent == null) {
            Log.w(TAG, "onStartCommand: null intent received, stopping service")
            stopSelf(startId)
            return START_NOT_STICKY
        }

        when (intent.action) {
            ACTION_START -> {
                val filePath = intent.getStringExtra(EXTRA_FILE_PATH)
                val format = intent.getStringExtra(EXTRA_FORMAT) ?: "WAV"
                if (filePath != null) {
                    startRecording(File(filePath), format)
                } else {
                    Log.e(TAG, "ACTION_START called with null filePath")
                }
            }
            ACTION_PAUSE -> pauseRecording()
            ACTION_RESUME -> resumeRecording()
            ACTION_STOP -> stopRecording()
        }
        return START_NOT_STICKY
    }

    @SuppressLint("InlinedApi")
    private fun startRecording(file: File, format: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "startRecording: RECORD_AUDIO permission not granted")
            onError("Microphone permission required")
            return
        }

        if (isStopping || stateManager.state.value == RecordingStatus.FINALIZING) {
            Log.w(TAG, "Cannot start new recording while previous session is finalizing")
            return
        }

        if (requestAudioFocus()) {
            val sessionId = synchronized(this) {
                activeSessionId++
            }
            Log.i(TAG, "Starting recording session #$sessionId. File: ${file.name}, Format: $format")
            currentFile = file
            startTimeMillis = SystemClock.elapsedRealtime()
            totalPausedTime = 0L
            isPaused = false
            isStopping = false

            stateManager.clearLastCompletedRecording()
            stateManager.updateState(RecordingStatus.RECORDING)

            serviceScope.launch(Dispatchers.IO) {
                try {
                    val id = recordingDao.insertRecording(
                        RecordingEntity(
                            fileName = file.name,
                            filePath = file.absolutePath,
                            durationMs = 0L,
                            timestamp = System.currentTimeMillis(),
                            fileSize = 0L,
                            status = "PENDING"
                        )
                    )
                    currentRecordingId = id.toInt()
                    Log.d(TAG, "Created pending DB entry with ID: $id for session #$sessionId")
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.e(TAG, "Failed to insert pending DB entry", e)
                }
            }

            serviceScope.launch {
                try {
                    if (settingsRepository.autoSilentMode.first()) {
                        Log.d(TAG, "Auto Silent Mode is ON, enabling DND")
                        audioStateManager.enableSilentMode()
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.e(TAG, "Failed to check auto silent mode setting", e)
                }
            }

            val notification = createNotification()

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start foreground service", e)
            }

            serviceScope.launch(Dispatchers.IO) {
                val started = audioRecorder.start(file, format)
                if (!started) {
                    withContext(Dispatchers.Main) {
                        onError("Failed to start hardware recording")
                    }
                }
            }
            handler.post(updateTimerRunnable)
        } else {
            Log.e(TAG, "Could not acquire audio focus")
            Toast.makeText(this, "Microphone busy", Toast.LENGTH_SHORT).show()
            stopSelf(lastStartId)
        }
    }

    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            audioManager.requestAudioFocus(audioFocusRequest!!) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
    }

    private fun pauseRecording() {
        if (!isPaused) {
            Log.i(TAG, "Pausing recording")
            audioRecorder.pause()
            isPaused = true
            lastPauseTimestamp = SystemClock.elapsedRealtime()
            stateManager.updateState(RecordingStatus.PAUSED)
            updateNotification()
        }
    }

    private fun resumeRecording() {
        if (isPaused) {
            if (requestAudioFocus()) {
                Log.i(TAG, "Resuming recording")
                audioRecorder.resume()
                isPaused = false
                totalPausedTime += SystemClock.elapsedRealtime() - lastPauseTimestamp
                stateManager.updateState(RecordingStatus.RECORDING)
                updateNotification()
            } else {
                Toast.makeText(this, "Cannot resume: Mic busy", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopRecording() {
        synchronized(this) {
            if (isStopping) {
                Log.w(TAG, "stopRecording called while already stopping")
                return
            }
            isStopping = true
        }

        val sessionToFinalize = activeSessionId
        val fileToFinalize = currentFile
        val recordingIdToFinalize = currentRecordingId

        Log.i(TAG, "Stopping recording service for session #$sessionToFinalize")
        handler.removeCallbacks(updateTimerRunnable)
        abandonAudioFocus()

        stateManager.updateState(RecordingStatus.FINALIZING)

        serviceScope.launch {
            withContext(Dispatchers.IO + NonCancellable) {
                try {
                    val stopResult = runCatching { audioRecorder.stop() }.getOrElse { Result.failure(it) }

                    if (stopResult.isSuccess && fileToFinalize != null && recordingIdToFinalize != null && fileToFinalize.exists() && fileToFinalize.length() > 44) {
                        val finalDuration = if (isPaused) {
                            lastPauseTimestamp - startTimeMillis - totalPausedTime
                        } else {
                            SystemClock.elapsedRealtime() - startTimeMillis - totalPausedTime
                        }

                        Log.d(TAG, "Finalizing DB entry ID: $recordingIdToFinalize for session #$sessionToFinalize")
                        val completedEntity = RecordingEntity(
                            id = recordingIdToFinalize,
                            fileName = fileToFinalize.name,
                            filePath = fileToFinalize.absolutePath,
                            durationMs = finalDuration,
                            timestamp = System.currentTimeMillis(),
                            fileSize = fileToFinalize.length(),
                            status = "COMPLETED"
                        )
                        val updateResult = runCatching { recordingDao.updateRecording(completedEntity) }
                        if (updateResult.isSuccess) {
                            Log.i(TAG, "Recording saved and status marked as COMPLETED.")
                            stateManager.notifyRecordingCompleted(completedEntity)
                        } else {
                            Log.e(TAG, "Failed to update recording entity in DB", updateResult.exceptionOrNull())
                            runCatching {
                                recordingDao.updateRecording(completedEntity.copy(status = "FAILED"))
                            }
                            stateManager.notifyRecordingFailed("Database update failed")
                        }
                    } else {
                        Log.w(TAG, "File empty or invalid. Marking DB entry ID: $recordingIdToFinalize as FAILED")
                        if (recordingIdToFinalize != null && fileToFinalize != null) {
                            runCatching {
                                recordingDao.updateRecording(
                                    RecordingEntity(
                                        id = recordingIdToFinalize,
                                        fileName = fileToFinalize.name,
                                        filePath = fileToFinalize.absolutePath,
                                        durationMs = 0,
                                        timestamp = System.currentTimeMillis(),
                                        fileSize = 0,
                                        status = "FAILED"
                                    )
                                )
                            }
                        }
                        stateManager.notifyRecordingFailed("Recording empty or invalid")
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.e(TAG, "Error finalizing recording", e)
                    stateManager.notifyRecordingFailed(e.message ?: "Finalization error")
                } finally {
                    audioStateManager.restoreAudioState()
                    if (activeSessionId == sessionToFinalize) {
                        stateManager.updateState(RecordingStatus.IDLE)
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf(lastStartId)
                        isStopping = false
                    }
                }
            }
        }
    }

    override fun onError(message: String) {
        handler.post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
            stopRecording()
        }
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val pauseResumeAction = if (isPaused) {
            val resumeIntent = Intent(this, RecordingService::class.java).apply { action = ACTION_RESUME }
            val resumePendingIntent = PendingIntent.getService(this, 2, resumeIntent, PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Action.Builder(android.R.drawable.ic_media_play, "Resume", resumePendingIntent).build()
        } else {
            val pauseIntent = Intent(this, RecordingService::class.java).apply { action = ACTION_PAUSE }
            val pausePendingIntent = PendingIntent.getService(this, 3, pauseIntent, PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Action.Builder(android.R.drawable.ic_media_pause, "Pause", pausePendingIntent).build()
        }

        val stopIntent = Intent(this, RecordingService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE)
        val stopAction = NotificationCompat.Action.Builder(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent).build()

        val elapsedTime = if (isPaused) {
            lastPauseTimestamp - startTimeMillis - totalPausedTime
        } else {
            SystemClock.elapsedRealtime() - startTimeMillis - totalPausedTime
        }
        val baseTime = System.currentTimeMillis() - elapsedTime

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(if (isPaused) "Recording Paused" else "Recording Audio...")
            .setContentText(if (isPaused) "Tap to resume" else "Recording in progress")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setColor(ContextCompat.getColor(this, R.color.recording_red))
            .setColorized(true)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setUsesChronometer(!isPaused)
            .setWhen(baseTime)
            .setShowWhen(true)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0, 1))
            .addAction(pauseResumeAction)
            .addAction(stopAction)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Audio Recording",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
                description = "Shows recording status and controls"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.w(TAG, "Task removed (app swiped away). Saving current recording state...")
        stopRecording()
    }

    override fun onDestroy() {
        Log.d(TAG, "RecordingService Destroyed")
        handler.removeCallbacks(updateTimerRunnable)
        audioRecorder.setErrorListener(null)
        audioStateManager.restoreAudioState()
        abandonAudioFocus()
        serviceScope.cancel()
        super.onDestroy()
    }
}
