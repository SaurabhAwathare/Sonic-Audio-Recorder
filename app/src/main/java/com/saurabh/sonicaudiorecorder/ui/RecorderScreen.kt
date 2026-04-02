package com.saurabh.sonicaudiorecorder.ui

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import com.saurabh.sonicaudiorecorder.data.local.RecordingEntity
import com.saurabh.sonicaudiorecorder.ui.components.MainRecordButton
import com.saurabh.sonicaudiorecorder.ui.components.PlaybackWaveform
import com.saurabh.sonicaudiorecorder.ui.components.WaveformVisualizer
import com.saurabh.sonicaudiorecorder.ui.theme.SonicGradient
import com.saurabh.sonicaudiorecorder.ui.theme.SonicPurple
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
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
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToRecording = onNavigateToRecording,
        onTogglePlayback = { viewModel.togglePlayback(it) },
        onSeek = { viewModel.seekTo(it) },
        onDeleteRecording = { viewModel.deleteRecording(it) },
        onRenameRecording = { recording, newName -> viewModel.renameRecording(recording, newName) },
        onShareRecording = { viewModel.shareRecording(it) }
    )
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecorderScreenContent(
    recordings: List<RecordingEntity>,
    isPlaying: Boolean,
    currentPlayingPath: String?,
    playbackPosition: Long,
    playbackDuration: Long,
    amplitudeHistory: List<Float>,
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
    val scaffoldState = rememberBottomSheetScaffoldState()
    val density = LocalDensity.current
    
    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val fullHeightPx = constraints.maxHeight.toFloat()
        val screenWidthPx = constraints.maxWidth.toFloat()
        
        val peekHeightDp = 300.dp 
        val peekHeightPx = with(density) { peekHeightDp.toPx() }
        val expandedSheetTopPx = fullHeightPx * 0.2f 
        
        val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val statusBarPx = with(density) { statusBarHeight.toPx() }
        val topContainerCenterY = (statusBarPx + expandedSheetTopPx) / 2
        
        val progress by remember {
            derivedStateOf {
                val currentOffset = try { scaffoldState.bottomSheetState.requireOffset() } catch (_: Exception) { fullHeightPx - peekHeightPx }
                val totalRange = (fullHeightPx - peekHeightPx) - expandedSheetTopPx
                val currentProgress = if (totalRange > 0) ((fullHeightPx - peekHeightPx) - currentOffset) / totalRange else 0f
                currentProgress.coerceIn(0f, 1f)
            }
        }

        val initialButtonOffsetY = (-40).dp 
        val initialXCenter = screenWidthPx / 2
        val initialYCenter = (fullHeightPx / 2) + with(density) { initialButtonOffsetY.toPx() }
        
        val initialWaveformGap = with(density) { 100.dp.toPx() } 
        val initialWaveformY = initialYCenter - initialWaveformGap
        
        val buttonSize = 110.dp
        val targetScale = 0.75f
        val targetXPx = with(density) { (24.dp + (buttonSize * targetScale) / 2).toPx() }
        
        val currentAnchorX = lerp(initialXCenter, targetXPx, progress)
        val currentAnchorY = lerp(initialWaveformY, topContainerCenterY, progress)

        Box(modifier = Modifier.fillMaxSize()) {
            BottomSheetScaffold(
                scaffoldState = scaffoldState,
                sheetPeekHeight = peekHeightDp,
                sheetContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                sheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                sheetShadowElevation = 16.dp,
                sheetContent = {
                    Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f)) {
                        if (recordings.isEmpty()) {
                            EmptyState()
                        } else {
                            RecordingList(
                                recordings = recordings,
                                isPlaying = isPlaying,
                                currentPlayingPath = currentPlayingPath,
                                playbackPosition = playbackPosition,
                                playbackDuration = playbackDuration,
                                onTogglePlayback = onTogglePlayback,
                                onSeek = onSeek,
                                onDeleteRequest = { recordingToDelete = it },
                                onRenameRequest = { recordingToRename = it },
                                onShareRequest = onShareRecording
                            )
                        }
                    }
                }
            ) { /* Content handled by layering */ }

            WaveformVisualizer(
                amplitudes = amplitudeHistory,
                progress = progress,
                anchorX = currentAnchorX,
                anchorY = currentAnchorY,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 20.dp)
                    .zIndex(10f)
            ) {
                // Title "Sonic Audio Recorder" removed to hide it from the top

                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                }
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                MainRecordButton(
                    onClick = onNavigateToRecording,
                    modifier = Modifier
                        .offset(y = initialButtonOffsetY)
                        .zIndex(9f)
                        .graphicsLayer {
                            val s = lerp(1f, targetScale, progress)
                            scaleX = s
                            scaleY = s
                            
                            translationX = (targetXPx - initialXCenter) * progress
                            translationY = (topContainerCenterY - initialYCenter) * progress
                        }
                        .shadow(
                            elevation = 20.dp,
                            shape = CircleShape,
                            ambientColor = SonicPurple.copy(alpha = 0.5f),
                            spotColor = SonicPurple
                        )
                )
            }
        }
    }

    // DIALOGS
    recordingToDelete?.let { recording ->
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Delete Recording") },
            text = { Text("Are you sure you want to delete '${recording.fileName}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteRecording(recording)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { }) { Text("Cancel") }
            }
        )
    }

    recordingToRename?.let { recording ->
        var newName by remember { mutableStateOf(recording.fileName.substringBeforeLast(".")) }
        AlertDialog(
            onDismissRequest = { },
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
                        }
                    }
                ) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun EmptyState() {
    val infiniteTransition = rememberInfiniteTransition(label = "emptyState")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 100.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Headset,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Your studio is empty",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            letterSpacing = 1.sp
        )
        Text(
            text = "Tap the mic to start your first session",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.padding(top = 8.dp)
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
    onTogglePlayback: (RecordingEntity) -> Unit,
    onSeek: (Long) -> Unit,
    onDeleteRequest: (RecordingEntity) -> Unit,
    onRenameRequest: (RecordingEntity) -> Unit,
    onShareRequest: (RecordingEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp, start = 24.dp, end = 24.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(recordings, key = { it.id }) { recording ->
            val isThisItemActive = currentPlayingPath == recording.filePath
            
            RecordingItem(
                recording = recording,
                isPlaying = isPlaying && isThisItemActive,
                isActive = isThisItemActive,
                playbackPosition = if (isThisItemActive) playbackPosition else 0L,
                playbackDuration = if (isThisItemActive) playbackDuration else recording.durationMs,
                onPlayClick = { onTogglePlayback(recording) },
                onSeek = onSeek,
                onDelete = { onDeleteRequest(recording) },
                onRename = { onRenameRequest(recording) },
                onShare = { onShareRequest(recording) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordingItem(
    recording: RecordingEntity,
    isPlaying: Boolean,
    isActive: Boolean,
    playbackPosition: Long,
    playbackDuration: Long,
    onPlayClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    var showMenu by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), Color.Transparent)
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(onClick = onPlayClick)
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            if (isActive) SonicGradient else Brush.linearGradient(listOf(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))),
                            CircleShape
                        )
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = if (isActive) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recording.fileName,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${formatDuration(recording.durationMs)} \u2022 ${dateFormat.format(Date(recording.timestamp))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            onClick = {
                                showMenu = false
                                onRename()
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Share") },
                            onClick = {
                                showMenu = false
                                onShare()
                            },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isActive,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    PlaybackWaveform(
                        filePath = recording.filePath,
                        playbackPosition = playbackPosition,
                        playbackDuration = playbackDuration,
                        onSeek = onSeek,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatDuration(playbackPosition),
                            style = MaterialTheme.typography.labelSmall,
                            color = SonicPurple
                        )
                        Text(
                            text = formatDuration(playbackDuration),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val minutes = (durationMs / 1000) / 60
    val seconds = (durationMs / 1000) % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
