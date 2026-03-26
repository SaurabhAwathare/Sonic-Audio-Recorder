package com.saurabh.sonicaudiorecorder.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saurabh.sonicaudiorecorder.data.local.RecordingEntity
import com.saurabh.sonicaudiorecorder.ui.theme.AudioRecorder01Theme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private val SonicPurple = Color(0xFF7F00FF)
private val SonicPink = Color(0xFFE100FF)
private val SonicCyan = Color(0xFF00E5FF)
private val SonicGradient = Brush.linearGradient(listOf(SonicPurple, SonicPink))

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

    RecorderScreenContent(
        recordings = recordings,
        isPlaying = isPlaying,
        currentPlayingPath = currentPlayingPath,
        playbackPosition = playbackPosition,
        playbackDuration = playbackDuration,
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopBar(onNavigateToSettings = onNavigateToSettings)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            VisualizerSection()
            
            TimerText(durationMs = playbackPosition)
            
            Spacer(modifier = Modifier.height(64.dp))
            
            RecordButton(onClick = onNavigateToRecording)
            
            Spacer(modifier = Modifier.height(72.dp))
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), Color.Transparent)
                        ),
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                    )
            ) {
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
    }

    recordingToDelete?.let { recording ->
        AlertDialog(
            onDismissRequest = {
            },
            title = { Text("Delete Recording") },
            text = { Text("Are you sure you want to delete '${recording.fileName}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteRecording(recording)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { }) {
                    Text("Cancel")
                }
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
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(onNavigateToSettings: () -> Unit) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                "Sonic Recorder",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 1.sp
                )
            )
        },
        actions = {
            IconButton(onClick = onNavigateToSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

@Composable
private fun VisualizerSection() {
    val infiniteTransition = rememberInfiniteTransition(label = "visualizer")
    val animationPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 40.dp)) {
            val barCount = 40
            val spacing = 4.dp.toPx()
            val barWidth = (size.width - (barCount - 1) * spacing) / barCount
            val centerY = size.height / 2
            
            repeat(barCount) { i ->
                val x = i * (barWidth + spacing)
                val wave = kotlin.math.sin(i.toDouble() * 0.5 + animationPhase).toFloat()
                val heightFactor = 0.2f + (0.4f * (wave + 1f) / 2f)
                val barHeight = size.height * heightFactor
                
                drawRoundRect(
                    brush = SonicGradient,
                    topLeft = Offset(x, centerY - barHeight / 2),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                )
            }
        }
    }
}

@Composable
private fun TimerText(durationMs: Long) {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
    val millis = (durationMs / 10) % 100
    
    Text(
        text = String.format(Locale.getDefault(), "%02d:%02d:%02d", minutes, seconds, millis),
        style = MaterialTheme.typography.displayMedium.copy(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onBackground,
            letterSpacing = 2.sp
        ),
        modifier = Modifier.drawBehind {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(SonicPurple.copy(alpha = 0.1f), Color.Transparent),
                    center = center,
                    radius = size.width
                )
            )
        }
    )
}

@Composable
private fun RecordButton(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .size(110.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = 30.dp,
                shape = CircleShape,
                spotColor = SonicPurple,
                ambientColor = SonicPurple
            )
            .background(SonicGradient, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Record",
            tint = Color.White,
            modifier = Modifier.size(52.dp)
        )
    }
}

@Composable
private fun EmptyState() {
    val infiniteTransition = rememberInfiniteTransition(label = "emptyState")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
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
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            letterSpacing = 1.sp
        )
        Text(
            text = "Tap the mic to start your first session",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
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
        contentPadding = PaddingValues(24.dp),
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
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), Color.Transparent)
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
                            if (isActive) SonicGradient else Brush.linearGradient(listOf(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = if (isActive) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recording.fileName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 17.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${formatDuration(recording.durationMs)} • ${dateFormat.format(Date(recording.timestamp))}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    )
                }
                
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Share", color = MaterialTheme.colorScheme.onSurface) },
                            onClick = { showMenu = false; onShare() },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) }
                        )
                        DropdownMenuItem(
                            text = { Text("Rename", color = MaterialTheme.colorScheme.onSurface) },
                            onClick = { showMenu = false; onRename() },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = Color.Red) },
                            onClick = { showMenu = false; onDelete() },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isActive) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    MiniWaveform(isActive = isPlaying)
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Slider(
                        value = playbackPosition.toFloat(),
                        onValueChange = { onSeek(it.toLong()) },
                        valueRange = 0f..(playbackDuration.coerceAtLeast(1L).toFloat()),
                        colors = SliderDefaults.colors(
                            thumbColor = SonicCyan,
                            activeTrackColor = SonicCyan,
                            inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatDuration(playbackPosition),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = formatDuration(playbackDuration),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniWaveform(isActive: Boolean) {
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    Canvas(modifier = Modifier.fillMaxWidth().height(32.dp)) {
        val barCount = 50
        val spacing = 2.dp.toPx()
        val barWidth = (size.width - (barCount - 1) * spacing) / barCount
        val centerY = size.height / 2
        
        repeat(barCount) { i ->
            val x = i * (barWidth + spacing)
            val randomHeight = if (isActive) (0.2f + (0.8f * Math.random().toFloat())) else 0.1f
            val barHeight = size.height * randomHeight
            
            drawRoundRect(
                color = if (isActive) SonicCyan.copy(alpha = 0.6f) else onSurfaceColor.copy(alpha = 0.1f),
                topLeft = Offset(x, centerY - barHeight / 2),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
        }
    }
}

private fun formatDuration(timeMs: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(timeMs)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(timeMs) % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

@Preview
@Composable
fun RecorderScreenPreview() {
    AudioRecorder01Theme {
        RecorderScreenContent(
            recordings = listOf(
                RecordingEntity(1, "Deep Focus Mix.wav", "/path/1", 125000, System.currentTimeMillis(), 1024L),
                RecordingEntity(2, "Song Idea #4.wav", "/path/2", 45000, System.currentTimeMillis() - 3600000, 2048L),
                RecordingEntity(3, "Interview_01.wav", "/path/3", 300000, System.currentTimeMillis() - 86400000, 512L)
            ),
            isPlaying = false,
            currentPlayingPath = null,
            playbackPosition = 0,
            playbackDuration = 0,
            onNavigateToSettings = {},
            onNavigateToRecording = {},
            onTogglePlayback = {},
            onSeek = {},
            onDeleteRecording = {},
            onRenameRecording = { _, _ -> },
            onShareRecording = {}
        )
    }
}
