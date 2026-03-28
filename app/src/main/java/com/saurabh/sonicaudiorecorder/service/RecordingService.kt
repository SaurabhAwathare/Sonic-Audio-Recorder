package com.saurabh.sonicaudiorecorder.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.saurabh.sonicaudiorecorder.MainActivity
import com.saurabh.sonicaudiorecorder.data.AudioRecorder
import com.example.audiorecorder01.data.local.RecordingDao
import com.saurabh.sonicaudiorecorder.data.local.RecordingEntity
import com.saurabh.sonicaudiorecorder.data.RecordingStateManager
import com.saurabh.sonicaudiorecorder.data.RecordingStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class RecordingService : Service() {

    @Inject
    lateinit var audioRecorder: AudioRecorder

    @Inject
    lateinit var recordingDao: RecordingDao

    @Inject
    lateinit var stateManager: RecordingStateManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private var currentFile: File? = null
    private var startTimeMillis: Long = 0L
    private var totalPausedTime: Long = 0L
    private var lastPauseTimestamp: Long = 0L
    private var isPaused = false

    private val handler = Handler(Looper.getMainLooper())
    private val updateTimerRunnable = object : Runnable {
        override fun run() {
            val elapsedTime = if (isPaused) {
                lastPauseTimestamp - startTimeMillis - totalPausedTime
            } else {
                System.currentTimeMillis() - startTimeMillis - totalPausedTime
            }
            
            stateManager.updateElapsedTime(elapsedTime)
            
            if (!isPaused) {
                stateManager.updateAmplitude(audioRecorder.getAmplitude())
                updateNotification()
            }
            handler.postDelayed(this, 100) // Update state more frequently for UI
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
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val filePath = intent.getStringExtra(EXTRA_FILE_PATH)
                val format = intent.getStringExtra(EXTRA_FORMAT) ?: "WAV"
                if (filePath != null) {
                    startRecording(File(filePath), format)
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
        currentFile = file
        startTimeMillis = System.currentTimeMillis()
        totalPausedTime = 0L
        isPaused = false
        
        stateManager.updateState(RecordingStatus.RECORDING)
        
        val notification = createNotification()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        
        audioRecorder.start(file, format)
        handler.post(updateTimerRunnable)
    }

    private fun pauseRecording() {
        if (!isPaused) {
            audioRecorder.pause()
            isPaused = true
            lastPauseTimestamp = System.currentTimeMillis()
            stateManager.updateState(RecordingStatus.PAUSED)
            updateNotification()
        }
    }

    private fun resumeRecording() {
        if (isPaused) {
            audioRecorder.resume()
            isPaused = false
            totalPausedTime += System.currentTimeMillis() - lastPauseTimestamp
            stateManager.updateState(RecordingStatus.RECORDING)
            updateNotification()
        }
    }

    private fun stopRecording() {
        handler.removeCallbacks(updateTimerRunnable)
        audioRecorder.stop()
        stateManager.updateState(RecordingStatus.IDLE)
        
        currentFile?.let { file ->
            serviceScope.launch(Dispatchers.IO) {
                val finalDuration = if (isPaused) {
                    lastPauseTimestamp - startTimeMillis - totalPausedTime
                } else {
                    System.currentTimeMillis() - startTimeMillis - totalPausedTime
                }
                
                recordingDao.insertRecording(
                    RecordingEntity(
                        fileName = file.name,
                        filePath = file.absolutePath,
                        durationMs = finalDuration,
                        timestamp = System.currentTimeMillis(),
                        fileSize = file.length()
                    )
                )
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
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
            System.currentTimeMillis() - startTimeMillis - totalPausedTime
        }
        
        val timerString = formatElapsedTime(elapsedTime)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(if (isPaused) "Recording Paused" else "Recording Audio...")
            .setContentText("Duration: $timerString")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0, 1))
            .addAction(pauseResumeAction)
            .addAction(stopAction)
            .build()
    }

    private fun formatElapsedTime(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Audio Recording",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacks(updateTimerRunnable)
        stateManager.reset()
        serviceScope.cancel()
        super.onDestroy()
    }
}
