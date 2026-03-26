package com.saurabh.sonicaudiorecorder.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.random.Random

@Composable
fun WaveformVisualizer(
    amplitude: Float,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    // We store a list of normalized heights (0.0f to 1.0f)
    val barHeights = remember { mutableStateListOf<Float>().apply { 
        repeat(40) { add(0.05f) } // Initial baseline
    } }
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)

    // Scaling and Smoothing Logic
    LaunchedEffect(amplitude) {
        if (isRecording) {
            // 1. Normalize and Amplify
            // Most speech is in the 1000-8000 range. Raw PCM max is 32768.
            // We use a logarithmic-style scaling or a simple multiplier to make it expressive.
            val normalized = (amplitude / 12000f).coerceIn(0.05f, 1.0f)
            
            // 2. Add to list and maintain fixed size
            barHeights.add(normalized)
            if (barHeights.size > 40) {
                barHeights.removeAt(0)
            }
        } else {
            // Idle subtle movement
            if (barHeights.any { it > 0.05f }) {
                repeat(barHeights.size) { i -> 
                    barHeights[i] = max(0.05f, barHeights[i] * 0.8f) 
                }
            }
        }
    }

    // Infinite animation for idle "breathing" effect when silent
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    Canvas(modifier = modifier
        .fillMaxWidth()
        .height(120.dp) // Increased height for better visibility
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        
        val barCount = barHeights.size
        val spacing = 6.dp.toPx()
        val barWidth = (width - (barCount - 1) * spacing) / barCount
        val minHeight = 4.dp.toPx()

        barHeights.forEachIndexed { index, rawHeight ->
            val x = index * (barWidth + spacing)
            
            // Apply breathing effect if silent/idle
            val finalHeightFactor = if (!isRecording || rawHeight <= 0.05f) {
                rawHeight * breatheScale
            } else {
                rawHeight
            }
            
            val visualHeight = max(minHeight, finalHeightFactor * height * 0.8f)
            
            // Draw rounded bars for a professional "ChatGPT-like" look
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(secondaryColor, primaryColor, secondaryColor),
                    startY = centerY - visualHeight / 2,
                    endY = centerY + visualHeight / 2
                ),
                topLeft = Offset(x, centerY - visualHeight / 2),
                size = Size(barWidth, visualHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
        }
    }
}
