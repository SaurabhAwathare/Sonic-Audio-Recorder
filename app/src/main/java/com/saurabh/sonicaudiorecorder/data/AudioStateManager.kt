package com.saurabh.sonicaudiorecorder.data

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AudioStateManager"

@Singleton
class AudioStateManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private var previousFilter: Int? = null

    /**
     * Check if the app has permission to change Do Not Disturb settings.
     */
    fun hasDndAccess(): Boolean {
        return notificationManager.isNotificationPolicyAccessGranted
    }

    /**
     * Enables Do Not Disturb (Priority Only) to prevent interruptions during recording.
     * Only works if DND access is granted.
     */
    fun enableSilentMode() {
        try {
            if (hasDndAccess()) {
                previousFilter = notificationManager.currentInterruptionFilter
                Log.d(TAG, "Enabling DND. Previous filter: $previousFilter")
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            } else {
                Log.w(TAG, "Cannot enable DND: Access not granted")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to enable DND", e)
        }
    }

    /**
     * Restores the previous Do Not Disturb state.
     */
    fun restoreAudioState() {
        try {
            if (hasDndAccess() && previousFilter != null) {
                Log.d(TAG, "Restoring DND filter to: $previousFilter")
                notificationManager.setInterruptionFilter(previousFilter!!)
                previousFilter = null
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to restore DND state", e)
        }
    }
}
