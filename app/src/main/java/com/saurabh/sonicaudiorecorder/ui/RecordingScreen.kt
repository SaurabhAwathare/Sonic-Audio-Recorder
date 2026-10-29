package com.saurabh.sonicaudiorecorder.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saurabh.sonicaudiorecorder.R
import com.saurabh.sonicaudiorecorder.data.RecordingStatus
import com.saurabh.sonicaudiorecorder.data.local.RecordingEntity
import com.saurabh.sonicaudiorecorder.ui.components.RecordingBarVisualizer
import com.saurabh.sonicaudiorecorder.ui.theme.SonicGradient
import com.saurabh.sonicaudiorecorder.ui.theme.TimerTextStyle
import com.saurabh.sonicaudiorecorder.ui.theme.sonicColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingScreen(
    viewModel: RecorderViewModel,
    onBack: () -> Unit
) {
    val isRecording by viewModel.isRecording.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val recordingStatus by viewModel.recordingStatus.collectAsState()
    val audioFormat by viewModel.audioFormat.collectAsState()
    val elapsedTime by viewModel.elapsedTime.collectAsState()
    val amplitudeHistory by viewModel.amplitudeHistory.collectAsState()
    val lastRecording by viewModel.lastRecording.collectAsState()

    var showPostRecordingSheet by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    
    val sheetState = rememberModalBottomSheetState()

    // Start recording automatically if not actively recording
    LaunchedEffect(Unit) {
        if (!isRecording) {
            viewModel.startRecording()
        }
    }

    BackHandler {
        if (isRecording) {
            viewModel.stopRecording()
        }
        viewModel.resetLastRecording()
        onBack()
    }

    // Show bottom sheet once recording is saved
    LaunchedEffect(lastRecording) {
        if (lastRecording != null && !showRenameDialog) {
            showPostRecordingSheet = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        StatusFormatBadge(
                            recordingStatus = recordingStatus,
                            audioFormat = audioFormat
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { 
                            if (isRecording) viewModel.stopRecording()
                            viewModel.resetLastRecording()
                            onBack()
                        },
                        modifier = Modifier.semantics {
                            contentDescription = "Close recording screen"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.recording_close_desc),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    // Balancing space for centered status badge
                    Spacer(modifier = Modifier.width(48.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Timer Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 32.dp, bottom = 16.dp)
            ) {
                TabularTimerDisplay(elapsedTimeMs = elapsedTime)
            }

            // Central Visualizer Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                RecordingBarVisualizer(
                    amplitudes = amplitudeHistory,
                    isPaused = isPaused || recordingStatus == RecordingStatus.PAUSED,
                    barCount = 24,
                    maxHeight = 160.dp
                )
            }

            // Bottom Control Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 64.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = isRecording,
                    transitionSpec = {
                        if (targetState) {
                            (slideInVertically { it } + fadeIn()).togetherWith(slideOutVertically { -it } + fadeOut())
                        } else {
                            (slideInVertically { -it } + fadeIn()).togetherWith(slideOutVertically { it } + fadeOut())
                        }.using(SizeTransform(clip = false))
                    },
                    label = "ControlTransition"
                ) { recordingActive ->
                    if (!recordingActive && !showPostRecordingSheet) {
                        RecordButton(onClick = { viewModel.startRecording() })
                    } else if (recordingActive) {
                        val isFinalizing = recordingStatus == RecordingStatus.FINALIZING
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(36.dp)
                        ) {
                            // Pause / Resume Control
                            LabeledControlCircle(
                                icon = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                label = if (isPaused) {
                                    stringResource(R.string.recording_control_resume)
                                } else {
                                    stringResource(R.string.recording_control_pause)
                                },
                                onClick = {
                                    if (isPaused) viewModel.resumeRecording() else viewModel.pauseRecording()
                                },
                                backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                                iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                enabled = !isFinalizing
                            )

                            // Stop / Save Control
                            LabeledControlCircle(
                                icon = Icons.Default.Stop,
                                label = stringResource(R.string.recording_control_stop),
                                onClick = { viewModel.stopRecording() },
                                backgroundColor = MaterialTheme.colorScheme.primary,
                                iconColor = MaterialTheme.colorScheme.onPrimary,
                                enabled = !isFinalizing,
                                size = 76.dp,
                                iconSize = 34.dp
                            )
                        }
                    }
                }
            }
        }
    }

    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPlayingPath by viewModel.currentPlayingPath.collectAsState()
    val playbackPosition by viewModel.playbackPosition.collectAsState()
    val playbackDuration by viewModel.playbackDuration.collectAsState()

    if (showPostRecordingSheet && lastRecording != null) {
        ModalBottomSheet(
            onDismissRequest = {
                showPostRecordingSheet = false
                viewModel.resetLastRecording()
                onBack()
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = {
                Box(
                    Modifier
                        .padding(vertical = 12.dp)
                        .width(32.dp)
                        .height(4.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), CircleShape)
                )
            }
        ) {
            val rec = lastRecording!!
            val isThisItemPlaying = isPlaying && currentPlayingPath == rec.filePath
            val activePos = if (currentPlayingPath == rec.filePath) playbackPosition else 0L
            val activeDur = if (currentPlayingPath == rec.filePath && playbackDuration > 0) playbackDuration else rec.durationMs

            PostRecordingReviewSheetContent(
                recording = rec,
                audioFormat = audioFormat,
                isPlaying = isThisItemPlaying,
                playbackPosition = activePos,
                playbackDuration = activeDur,
                onPlayToggle = { viewModel.togglePlayback(rec) },
                onSeek = { viewModel.seekTo(it) },
                onShare = { viewModel.shareRecording(rec) },
                onRename = { showRenameDialog = true },
                onDelete = {
                    viewModel.deleteRecording(rec)
                    showPostRecordingSheet = false
                    onBack()
                },
                onDone = {
                    showPostRecordingSheet = false
                    viewModel.resetLastRecording()
                    onBack()
                }
            )
        }
    }

    if (showRenameDialog && lastRecording != null) {
        RenameRecordingDialog(
            currentName = getRecordingDisplayTitle(lastRecording!!),
            onDismiss = { showRenameDialog = false },
            onSave = { newName ->
                viewModel.renameRecording(lastRecording!!, newName)
                showRenameDialog = false
            }
        )
    }
}

@Composable
fun PostRecordingReviewSheetContent(
    recording: RecordingEntity,
    audioFormat: String,
    isPlaying: Boolean,
    playbackPosition: Long,
    playbackDuration: Long,
    onPlayToggle: () -> Unit,
    onSeek: (Long) -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.sonicColors.successState,
            modifier = Modifier.size(52.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.post_recording_title),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
        val formattedDate = remember(recording.timestamp) {
            formatTimestamp(recording.timestamp)
        }
        if (formattedDate.isNotEmpty()) {
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Recording Details & Preview Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // File Name + Format Badge Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = getRecordingDisplayTitle(recording),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text(
                            text = if (audioFormat.equals("M4A", ignoreCase = true)) "M4A" else "WAV",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Metadata Subtitle: Duration • File Size
                val durationStr = remember(recording.durationMs) { formatDuration(recording.durationMs) }
                val sizeStr = remember(recording.fileSize) { formatFileSize(recording.fileSize) }
                Text(
                    text = "$durationStr \u2022 $sizeStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Playback Preview Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(SonicGradient)
                            .clickable(onClick = onPlayToggle)
                            .semantics {
                                contentDescription = if (isPlaying) "Pause preview" else "Play preview"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        val effectiveDuration = if (playbackDuration > 0) playbackDuration else recording.durationMs
                        val progressFraction = if (effectiveDuration > 0) {
                            (playbackPosition.toFloat() / effectiveDuration.toFloat()).coerceIn(0f, 1f)
                        } else 0f

                        Slider(
                            value = progressFraction,
                            onValueChange = { fraction ->
                                onSeek((fraction * effectiveDuration).toLong())
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.sonicColors.waveformPlayed,
                                activeTrackColor = MaterialTheme.sonicColors.waveformPlayed,
                                inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.height(24.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatDuration(playbackPosition),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.sonicColors.waveformPlayed
                            )
                            Text(
                                text = formatDuration(effectiveDuration),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Actions Row (Share, Rename, Delete)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            PostRecordingAction(
                icon = Icons.Default.Share,
                label = stringResource(R.string.post_recording_share),
                onClick = onShare
            )
            PostRecordingAction(
                icon = Icons.Default.Edit,
                label = stringResource(R.string.post_recording_rename),
                onClick = onRename
            )
            PostRecordingAction(
                icon = Icons.Default.Delete,
                label = stringResource(R.string.post_recording_delete),
                isError = true,
                onClick = onDelete
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Done Button
        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = stringResource(R.string.post_recording_done),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            )
        }
    }
}

fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

fun formatFileSize(fileSizeBytes: Long): String {
    if (fileSizeBytes <= 0) return "0 B"
    val kb = fileSizeBytes / 1024f
    val mb = kb / 1024f
    return when {
        mb >= 1.0f -> String.format(Locale.US, "%.1f MB", mb)
        kb >= 1.0f -> String.format(Locale.US, "%.0f KB", kb)
        else -> "$fileSizeBytes B"
    }
}

fun formatTimestamp(timestampMs: Long): String {
    if (timestampMs <= 0) return ""
    val formatter = SimpleDateFormat("MMM dd, yyyy \u2022 hh:mm a", Locale.getDefault())
    return formatter.format(Date(timestampMs))
}

@Composable
fun StatusFormatBadge(
    recordingStatus: RecordingStatus,
    audioFormat: String
) {
    val statusText = when (recordingStatus) {
        RecordingStatus.RECORDING -> stringResource(R.string.recording_status_recording)
        RecordingStatus.PAUSED -> stringResource(R.string.recording_status_paused)
        RecordingStatus.FINALIZING -> stringResource(R.string.recording_status_finalizing)
        RecordingStatus.IDLE -> stringResource(R.string.recording_status_finalizing)
    }

    val formatText = if (audioFormat.equals("M4A", ignoreCase = true)) {
        stringResource(R.string.recording_format_m4a_badge)
    } else {
        stringResource(R.string.recording_format_wav_badge)
    }

    val statusDotColor = when (recordingStatus) {
        RecordingStatus.RECORDING -> MaterialTheme.sonicColors.recordingActive
        RecordingStatus.PAUSED -> MaterialTheme.sonicColors.recordingPaused
        RecordingStatus.FINALIZING -> MaterialTheme.colorScheme.primary
        RecordingStatus.IDLE -> MaterialTheme.colorScheme.primary
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusDotColor)
            )

            Text(
                text = statusText,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "•",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = formatText,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TabularTimerDisplay(elapsedTimeMs: Long) {
    val formattedTime = remember(elapsedTimeMs) {
        formatTimer(elapsedTimeMs)
    }

    Text(
        text = formattedTime,
        style = TimerTextStyle,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center
    )
}

fun formatTimer(elapsedTimeMs: Long): String {
    val totalSeconds = elapsedTimeMs / 1000
    val tenths = (elapsedTimeMs % 1000) / 100
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format(Locale.US, "%02d:%02d:%02d.%d", hours, minutes, seconds, tenths)
    } else {
        String.format(Locale.US, "%02d:%02d.%d", minutes, seconds, tenths)
    }
}

@Composable
fun LabeledControlCircle(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    backgroundColor: Color,
    iconColor: Color,
    enabled: Boolean = true,
    size: Dp = 64.dp,
    iconSize: Dp = 28.dp
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.4f))
                .clickable(enabled = enabled, onClick = onClick)
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) iconColor else iconColor.copy(alpha = 0.4f),
                modifier = Modifier.size(iconSize)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = if (enabled) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
        )
    }
}

@Composable
fun RecordButton(onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(SonicGradient)
                .clickable(onClick = onClick)
                .semantics { contentDescription = "Start recording" },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Mic, "Record", tint = Color.White, modifier = Modifier.size(36.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.recording_status_recording),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun RenameRecordingDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(currentName, TextRange(currentName.length)))
    }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Recording") },
        text = {
            Column {
                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = { textFieldValue = it },
                    label = { Text("File name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    isError = textFieldValue.text.isBlank()
                )
                if (textFieldValue.text.isBlank()) {
                    Text(
                        text = "Name cannot be empty",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = textFieldValue.text.isNotBlank(),
                onClick = { onSave(textFieldValue.text) }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun PostRecordingAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isError: Boolean = false
) {
    val contentColor = if (isError) MaterialTheme.sonicColors.errorState else MaterialTheme.colorScheme.onSurface
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = contentColor.copy(alpha = 0.8f), modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = contentColor)
    }
}
