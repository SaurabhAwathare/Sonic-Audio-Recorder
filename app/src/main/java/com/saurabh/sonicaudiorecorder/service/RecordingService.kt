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
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.saurabh.sonicaudiorecorder.MainActivity
import com.saurabh.sonicaudiorecorder.R
import com.saurabh.sonicaudiorecorder.data.AudioRecorder
import com.saurabh.sonicaudiorecorder.data.local.RecordingDao
import com.saurabh.sonicaudiorecorder.data.local.RecordingEntity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class RecordingService : Service() {

    @Inject
    lateinit var audioRecorder: AudioRecorder

    @Inject
    lateinit var recordingDao: RecordingDao

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private var currentFile: File? = null
    private var startTimeMillis: Long = 0L

    companion object {
        const val CHANNEL_ID = "recording_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
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
            ACTION_STOP -> stopRecording()
        }
        return START_NOT_STICKY
    }

    @SuppressLint("InlinedApi")
    private fun startRecording(file: File, format: String) {
        currentFile = file
        startTimeMillis = System.currentTimeMillis()
        
        val notification = createNotification()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        
        audioRecorder.start(file, format)
    }

    private fun stopRecording() {
        audioRecorder.stop()
        
        currentFile?.let { file ->
            serviceScope.launch(Dispatchers.IO) {
                val duration = System.currentTimeMillis() - startTimeMillis
                recordingDao.insertRecording(
                    RecordingEntity(
                        fileName = file.name,
                        filePath = file.absolutePath,
                        durationMs = duration,
                        timestamp = System.currentTimeMillis(),
                        fileSize = file.length()
                    )
                )
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        // Intent to stop recording from notification action
        val stopIntent = Intent(this, RecordingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Recording Audio...")
            .setContentText("Sonic Recorder is capturing audio in background")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now) // Reverted until icon file is physically present
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .build()
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
        serviceScope.cancel()
        super.onDestroy()
    }
}
