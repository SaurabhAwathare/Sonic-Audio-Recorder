package com.saurabh.sonicaudiorecorder.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.saurabh.sonicaudiorecorder.data.waveform.M4aWaveformExtractor
import com.saurabh.sonicaudiorecorder.data.waveform.WavWaveformExtractor
import com.saurabh.sonicaudiorecorder.data.waveform.WaveformRepository
import com.saurabh.sonicaudiorecorder.ui.theme.SonicGradient
import com.saurabh.sonicaudiorecorder.ui.theme.sonicColors

private val fallbackRepository by lazy {
    WaveformRepository(WavWaveformExtractor(), M4aWaveformExtractor())
}

@Composable
fun PlaybackWaveform(
    filePath: String,
    playbackPosition: Long,
    playbackDuration: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    waveformRepository: WaveformRepository? = null
) {
    // Asynchronously extract amplitude data via WaveformRepository
    var amplitudes by remember(filePath) { mutableStateOf(emptyList<Float>()) }
    
    LaunchedEffect(filePath, waveformRepository) {
        val repo = waveformRepository ?: fallbackRepository
        amplitudes = repo.getWaveform(filePath)
    }
    
    val progress = if (playbackDuration > 0) playbackPosition.toFloat() / playbackDuration else 0f
    
    val unplayedColor = MaterialTheme.sonicColors.waveformUnplayed
    val playheadColor = MaterialTheme.sonicColors.playheadCursor

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .pointerInput(playbackDuration) {
                detectTapGestures { offset ->
                    if (playbackDuration > 0) {
                        val seekProgress = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeek((seekProgress * playbackDuration).toLong())
                    }
                }
            }
            .pointerInput(playbackDuration) {
                detectDragGestures { change, _ ->
                    if (playbackDuration > 0) {
                        val seekProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                        onSeek((seekProgress * playbackDuration).toLong())
                    }
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        
        if (amplitudes.isEmpty()) {
            // Draw a subtle flat line while loading or if no data
            drawLine(
                color = unplayedColor.copy(alpha = 0.5f),
                start = Offset(0f, centerY),
                end = Offset(width, centerY),
                strokeWidth = 1.dp.toPx()
            )
            return@Canvas
        }

        val path = Path()
        val step = width / (amplitudes.size - 1)
        
        // Build the symmetrical waveform path
        for (i in amplitudes.indices) {
            val x = i * step
            val amp = amplitudes[i].coerceIn(0.01f, 1f)
            val yOffset = (amp * (height / 2.2f))
            val y = centerY - yOffset
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        
        for (i in amplitudes.indices.reversed()) {
            val x = i * step
            val amp = amplitudes[i].coerceIn(0.01f, 1f)
            val yOffset = (amp * (height / 2.2f))
            val y = centerY + yOffset
            path.lineTo(x, y)
        }
        path.close()

        // 1. Draw Background (Faded "unplayed" state)
        drawPath(
            path = path,
            color = unplayedColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // 2. Draw Foreground (Highlighted "played" state)
        val playedWidth = width * progress
        if (playedWidth > 0) {
            drawContext.canvas.save()
            drawContext.canvas.clipRect(0f, 0f, playedWidth, height)
            drawPath(
                path = path,
                brush = SonicGradient,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawContext.canvas.restore()
        }
        
        // 3. Playhead Cursor
        if (progress > 0 && progress < 1f) {
            drawLine(
                color = playheadColor,
                start = Offset(playedWidth, 0f),
                end = Offset(playedWidth, height),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}
