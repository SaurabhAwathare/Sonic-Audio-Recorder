package com.saurabh.sonicaudiorecorder.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saurabh.sonicaudiorecorder.ui.components.WaveformVisualizer
import com.saurabh.sonicaudiorecorder.ui.theme.SonicGradient
import com.saurabh.sonicaudiorecorder.ui.theme.SonicPurple
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingScreen(
    viewModel: RecorderViewModel,
    onBack: () -> Unit
) {
    val isRecording by viewModel.isRecording.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val elapsedTime by viewModel.elapsedTime.collectAsState()
    val amplitudeHistory by viewModel.amplitudeHistory.collectAsState()
    val lastRecording by viewModel.lastRecording.collectAsState()

    var showPostRecordingSheet by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    
    val sheetState = rememberModalBottomSheetState()

    // Start recording automatically
    LaunchedEffect(Unit) {
        if (!isRecording) {
            viewModel.startRecording()
        }
    }

    BackHandler {
        if (isRecording) {
            viewModel.stopRecording()
        }
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
                    Text(
                        "Sound Recorder",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (isRecording) viewModel.stopRecording()
                        onBack()
                    }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = { Spacer(modifier = Modifier.width(48.dp)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                TimerDisplay(elapsedTimeMs = elapsedTime)
                Text(
                    text = if (isPaused) "Paused" else "Recording...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = (if (isPaused) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f) else SonicPurple)
                )
            }

            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                WaveformVisualizer(amplitudes = amplitudeHistory)
            }

            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 80.dp), contentAlignment = Alignment.Center) {
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
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                            ControlCircle(
                                icon = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                onClick = { if (isPaused) viewModel.resumeRecording() else viewModel.pauseRecording() },
                                backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                                iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            ControlCircle(
                                icon = Icons.Default.Stop,
                                onClick = { viewModel.stopRecording() },
                                backgroundColor = SonicPurple,
                                iconColor = Color.White,
                                size = 80.dp,
                                iconSize = 36.dp
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPostRecordingSheet && lastRecording != null) {
        ModalBottomSheet(
            onDismissRequest = {
                showPostRecordingSheet = false
                viewModel.resetLastRecording()
                onBack()
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { Box(Modifier.padding(vertical = 12.dp).width(32.dp).height(4.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), CircleShape)) }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(56.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Recording saved", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface))
                Text(lastRecording!!.fileName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                Spacer(modifier = Modifier.height(32.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    PostRecordingAction(Icons.Default.Share, "Share", onClick = { viewModel.shareRecording(lastRecording!!) })
                    PostRecordingAction(Icons.Default.Edit, "Rename", onClick = { showRenameDialog = true })
                    PostRecordingAction(Icons.Default.Delete, "Delete", isError = true, onClick = { 
                        viewModel.deleteRecording(lastRecording!!)
                        showPostRecordingSheet = false
                        onBack()
                    })
                }
            }
        }
    }

    if (showRenameDialog && lastRecording != null) {
        RenameRecordingDialog(
            currentName = lastRecording!!.fileName.substringBeforeLast("."),
            onDismiss = { showRenameDialog = false },
            onSave = { newName ->
                viewModel.renameRecording(lastRecording!!, newName)
                showRenameDialog = false
            }
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
fun PostRecordingAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit, isError: Boolean = false) {
    val contentColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(MaterialTheme.shapes.medium).clickable(onClick = onClick).padding(12.dp)) {
        Icon(imageVector = icon, contentDescription = label, tint = contentColor.copy(alpha = 0.8f), modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = contentColor)
    }
}

@Composable
fun TimerDisplay(elapsedTimeMs: Long) {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedTimeMs)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedTimeMs) % 60
    Text(String.format(Locale.getDefault(), "%d:%02d", minutes, seconds), style = MaterialTheme.typography.displayLarge.copy(fontFamily = FontFamily.Default, fontWeight = FontWeight.Light, fontSize = 80.sp), color = MaterialTheme.colorScheme.onBackground)
}

@Composable
fun RecordButton(onClick: () -> Unit) {
    Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(SonicGradient).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(Icons.Default.Mic, "Record", tint = Color.White, modifier = Modifier.size(36.dp))
    }
}

@Composable
fun ControlCircle(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, backgroundColor: Color, iconColor: Color, size: androidx.compose.ui.unit.Dp = 64.dp, iconSize: androidx.compose.ui.unit.Dp = 28.dp) {
    Box(modifier = Modifier.size(size).clip(CircleShape).background(backgroundColor).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(iconSize))
    }
}
