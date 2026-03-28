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
import com.saurabh.sonicaudiorecorder.ui.theme.SonicGradient
import com.saurabh.sonicaudiorecorder.ui.theme.SonicPurple
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.abs

@Composable
fun PlaybackWaveform(
    filePath: String,
    playbackPosition: Long,
    playbackDuration: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // Asynchronously extract amplitude data to keep UI thread smooth
    var amplitudes by remember(filePath) { mutableStateOf(emptyList<Float>()) }
    
    LaunchedEffect(filePath) {
        amplitudes = withContext(Dispatchers.IO) {
            extractAmplitudes(filePath)
        }
    }
    
    val progress = if (playbackDuration > 0) playbackPosition.toFloat() / playbackDuration else 0f
    
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
                color = Color.Gray.copy(alpha = 0.1f),
                start = Offset(0f, centerY),
                end = Offset(width, centerY),
                strokeWidth = 1.dp.toPx()
            )
            return@Canvas
        }

        val path = Path()
        val step = width / (amplitudes.size - 1)
        
        // Build the waveform path (symmetrical)
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
            color = Color.Gray.copy(alpha = 0.15f),
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
                color = SonicPurple,
                start = Offset(playedWidth, 0f),
                end = Offset(playedWidth, height),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Real Audio Extraction Logic:
 * Parses a WAV file, extracts PCM 16-bit samples, and downsamples them
 * into a lightweight amplitude array for visualization.
 */
private fun extractAmplitudes(filePath: String, points: Int = 160): List<Float> {
    val file = File(filePath)
    if (!file.exists() || !filePath.endsWith(".wav")) return emptyList()

    return try {
        RandomAccessFile(file, "r").use { raf ->
            val dataSize = file.length() - 44 // Standard WAV Header size
            if (dataSize <= 0) return emptyList()

            val totalSamples = (dataSize / 2).toInt() // 16-bit = 2 bytes per sample
            val samplesPerPoint = totalSamples / points
            
            if (samplesPerPoint <= 0) return emptyList()

            val amplitudes = mutableListOf<Float>()
            val buffer = ByteArray(samplesPerPoint * 2)
            
            raf.seek(44) // Jump past header to PCM data

            for (i in 0 until points) {
                val bytesRead = raf.read(buffer)
                if (bytesRead <= 0) break
                
                // Peak-finding downsampling strategy
                var maxAmplitude = 0
                for (j in 0 until bytesRead step 2) {
                    val sample = ((buffer[j + 1].toInt() shl 8) or (buffer[j].toInt() and 0xFF)).toShort()
                    val absSample = abs(sample.toInt())
                    if (absSample > maxAmplitude) maxAmplitude = absSample
                }
                
                // Normalize relative to maximum possible 16-bit value
                val normalized = (maxAmplitude.toFloat() / 32767f).coerceIn(0f, 1f)
                amplitudes.add(normalized)
            }
            amplitudes
        }
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}
