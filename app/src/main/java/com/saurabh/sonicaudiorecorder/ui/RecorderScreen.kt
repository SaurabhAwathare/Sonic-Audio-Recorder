package com.saurabh.sonicaudiorecorder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saurabh.sonicaudiorecorder.R
import com.saurabh.sonicaudiorecorder.data.local.RecordingEntity
import com.saurabh.sonicaudiorecorder.data.waveform.WaveformRepository
import com.saurabh.sonicaudiorecorder.ui.components.EmptyRecordingsState
import com.saurabh.sonicaudiorecorder.ui.components.MainRecordButton
import com.saurabh.sonicaudiorecorder.ui.components.RecordingItemCard
import com.saurabh.sonicaudiorecorder.ui.components.WaveformVisualizer

@Composable
fun RecorderScreen(
    viewModel: RecorderViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToRecording: () -> Unit
) {
    val recordings by viewModel.recordings.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPlayingPath by viewModel.currentPlayingPath.collectAsState()
    val playbackPosition by viewModel.playbackPosition.collectAsState()
    val playbackDuration by viewModel.playbackDuration.collectAsState()
    val amplitudeHistory by viewModel.amplitudeHistory.collectAsState()

    RecorderScreenContent(
        recordings = recordings,
        isPlaying = isPlaying,
        currentPlayingPath = currentPlayingPath,
        playbackPosition = playbackPosition,
        playbackDuration = playbackDuration,
        amplitudeHistory = amplitudeHistory,
        waveformRepository = viewModel.waveformRepository,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToRecording = onNavigateToRecording,
        onTogglePlayback = { viewModel.togglePlayback(it) },
        onSeek = { viewModel.seekTo(it) },
        onDeleteRecording = { viewModel.deleteRecording(it) },
        onRenameRecording = { recording, newName -> viewModel.renameRecording(recording, newName) },
        onShareRecording = { viewModel.shareRecording(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecorderScreenContent(
    recordings: List<RecordingEntity>,
    isPlaying: Boolean,
    currentPlayingPath: String?,
    playbackPosition: Long,
    playbackDuration: Long,
    amplitudeHistory: List<Float>,
    waveformRepository: WaveformRepository? = null,
    onNavigateToSettings: () -> Unit,
    onNavigateToRecording: () -> Unit,
    onTogglePlayback: (RecordingEntity) -> Unit,
    onSeek: (Long) -> Unit,
    onDeleteRecording: (RecordingEntity) -> Unit,
    onRenameRecording: (RecordingEntity, String) -> Unit,
    onShareRecording: (RecordingEntity) -> Unit
) {
    var recordingToDelete by remember { mutableStateOf<RecordingEntity?>(null) }
    var recordingToRename by remember { mutableStateOf<RecordingEntity?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Waveform visualizer as reactive ambient background
        WaveformVisualizer(
            amplitudes = amplitudeHistory,
            modifier = Modifier.fillMaxSize()
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                // Sonic Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.home_header_title),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    IconButton(
                        onClick = onNavigateToSettings
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings_title),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Prominent Recording Action Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    MainRecordButton(
                        onClick = onNavigateToRecording,
                        labelAlpha = 1f
                    )
                }

                // Saved Recordings Section Header & Count
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Saved Recordings",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Text(
                            text = "${recordings.size}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Recordings List or Empty State
                if (recordings.isEmpty()) {
                    EmptyRecordingsState(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                } else {
                    RecordingList(
                        recordings = recordings,
                        isPlaying = isPlaying,
                        currentPlayingPath = currentPlayingPath,
                        playbackPosition = playbackPosition,
                        playbackDuration = playbackDuration,
                        waveformRepository = waveformRepository,
                        onTogglePlayback = onTogglePlayback,
                        onSeek = onSeek,
                        onDeleteRequest = { recordingToDelete = it },
                        onRenameRequest = { recordingToRename = it },
                        onShareRequest = onShareRecording,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    // DIALOGS
    recordingToDelete?.let { recording ->
        AlertDialog(
            onDismissRequest = { recordingToDelete = null },
            title = { Text("Delete Recording") },
            text = { Text("Are you sure you want to delete '${getRecordingDisplayTitle(recording)}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteRecording(recording)
                        recordingToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { recordingToDelete = null }) { Text("Cancel") }
            }
        )
    }

    recordingToRename?.let { recording ->
        var newName by remember { mutableStateOf(getRecordingDisplayTitle(recording)) }
        AlertDialog(
            onDismissRequest = { recordingToRename = null },
            title = { Text("Rename Recording") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("New name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newName.isNotBlank()) {
                            onRenameRecording(recording, newName)
                            recordingToRename = null
                        }
                    }
                ) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { recordingToRename = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun RecordingList(
    recordings: List<RecordingEntity>,
    isPlaying: Boolean,
    currentPlayingPath: String?,
    playbackPosition: Long,
    playbackDuration: Long,
    modifier: Modifier = Modifier,
    waveformRepository: WaveformRepository? = null,
    onTogglePlayback: (RecordingEntity) -> Unit,
    onSeek: (Long) -> Unit,
    onDeleteRequest: (RecordingEntity) -> Unit,
    onRenameRequest: (RecordingEntity) -> Unit,
    onShareRequest: (RecordingEntity) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp, start = 20.dp, end = 20.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(recordings, key = { it.id }) { recording ->
            val isThisItemActive = currentPlayingPath == recording.filePath

            RecordingItemCard(
                recording = recording,
                isPlaying = isPlaying && isThisItemActive,
                isActive = isThisItemActive,
                playbackPosition = if (isThisItemActive) playbackPosition else 0L,
                playbackDuration = if (isThisItemActive) playbackDuration else recording.durationMs,
                waveformRepository = waveformRepository,
                onPlayClick = { onTogglePlayback(recording) },
                onSeek = onSeek,
                onDelete = { onDeleteRequest(recording) },
                onRename = { onRenameRequest(recording) },
                onShare = { onShareRequest(recording) }
            )
        }
    }
}
