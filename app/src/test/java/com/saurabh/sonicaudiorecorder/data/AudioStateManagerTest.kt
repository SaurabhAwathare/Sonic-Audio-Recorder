package com.saurabh.sonicaudiorecorder.data

import android.app.NotificationManager
import android.content.Context
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

class AudioStateManagerTest {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var notificationManager: NotificationManager

    private lateinit var audioStateManager: AudioStateManager

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        `when`(context.getSystemService(Context.NOTIFICATION_SERVICE)).thenReturn(notificationManager)
        audioStateManager = AudioStateManager(context)
    }

    @Test
    fun `enableSilentMode should set interruption filter when access granted`() {
        // Given
        `when`(notificationManager.isNotificationPolicyAccessGranted).thenReturn(true)
        `when`(notificationManager.currentInterruptionFilter).thenReturn(NotificationManager.INTERRUPTION_FILTER_ALL)

        // When
        audioStateManager.enableSilentMode()

        // Then
        verify(notificationManager).setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
    }

    @Test
    fun `enableSilentMode should not set interruption filter when access denied`() {
        // Given
        `when`(notificationManager.isNotificationPolicyAccessGranted).thenReturn(false)

        // When
        audioStateManager.enableSilentMode()

        // Then
        verify(notificationManager, never()).setInterruptionFilter(anyInt())
    }

    @Test
    fun `restoreAudioState should restore previous filter`() {
        // Given
        `when`(notificationManager.isNotificationPolicyAccessGranted).thenReturn(true)
        `when`(notificationManager.currentInterruptionFilter).thenReturn(NotificationManager.INTERRUPTION_FILTER_ALL)
        
        audioStateManager.enableSilentMode() // Store the previous filter

        // When
        audioStateManager.restoreAudioState()

        // Then
        verify(notificationManager).setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
    }
}
