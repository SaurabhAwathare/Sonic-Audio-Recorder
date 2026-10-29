package com.saurabh.sonicaudiorecorder.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.saurabh.sonicaudiorecorder.ui.theme.SonicPink
import com.saurabh.sonicaudiorecorder.ui.theme.SonicPurple
import com.saurabh.sonicaudiorecorder.ui.theme.SonicViolet
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun WaveformVisualizer(
    amplitudes: List<Float>,
    modifier: Modifier = Modifier,
    progress: Float = 0f,
    anchorX: Float? = null,
    anchorY: Float? = null
) {
    // SINGLE GLOBAL TIME SOURCE - Ensures animation never resets
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_waves")
    
    val globalTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(100000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "globalTime"
    )

    val gradient = Brush.linearGradient(
        colors = listOf(SonicPurple, SonicPink, SonicViolet),
        start = Offset.Zero,
        end = Offset.Infinite
    )

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            
            val currentAnchorX = anchorX ?: (width / 2f)
            val currentAnchorY = anchorY ?: (height / 2f)
            
            val isIdle = amplitudes.isEmpty() || amplitudes.all { it < 0.001f }
            
            // Calculate starting point to move with anchor
            val startX = 0f * (1f - progress) + currentAnchorX * progress
            val activeWidth = width - startX

            if (isIdle) {
                // FIXED: Ambient waves now persist in ALL states (including progress = 1.0)
                drawAmbientWaves(startX, activeWidth, currentAnchorY, globalTime, gradient, progress)
            } else {
                drawReactiveWaveform(startX, activeWidth, currentAnchorY, amplitudes, progress, gradient)
            }
        }
    }
}

private fun DrawScope.drawAmbientWaves(
    startX: Float,
    width: Float,
    centerY: Float,
    time: Float,
    gradient: Brush,
    progress: Float
) {
    // Scale amplitude down slightly as we collapse to maintain elegance in the final state
    val scale = 1f - (progress * 0.4f)
    val alphaScale = 1f - (progress * 0.2f)

    val phase1 = time * 0.5f
    val phase2 = time * 0.7f
    val phase3 = time * 0.3f

    // Layer 1: Bottom Layer
    drawWavePath(startX, width, centerY, phase1, amplitude = 50f * scale, frequency = 0.8f, gradient, alpha = 0.3f * alphaScale, strokeWidth = 4.dp.toPx())
    
    // Layer 2: Middle Layer
    drawWavePath(startX, width, centerY, phase2, amplitude = 35f * scale, frequency = 1.5f, gradient, alpha = 0.6f * alphaScale, strokeWidth = 3.dp.toPx())
    
    // Layer 3: Top Layer
    drawWavePath(startX, width, centerY, phase3, amplitude = 20f * scale, frequency = 2.2f, gradient, alpha = 0.8f * alphaScale, strokeWidth = 2.dp.toPx())
    
    // Subtle Glow Layer
    drawWavePath(startX, width, centerY, phase2, amplitude = 35f * scale, frequency = 1.5f, gradient, alpha = 0.15f * alphaScale, strokeWidth = 12.dp.toPx())
}

private fun DrawScope.drawWavePath(
    startX: Float,
    activeWidth: Float,
    centerY: Float,
    phase: Float,
    amplitude: Float,
    frequency: Float,
    brush: Brush,
    alpha: Float,
    strokeWidth: Float
) {
    val path = Path()
    val points = 80
    for (i in 0..points) {
        val p = i.toFloat() / points
        val x = startX + p * activeWidth
        val edgeTaper = sin(p * PI.toFloat())
        
        val wave = sin(phase + p * frequency * 2f * PI.toFloat()) + 
                   0.2f * sin(phase * 1.5f + p * frequency * 4f * PI.toFloat())
        
        val y = centerY + (amplitude * edgeTaper) * wave
        
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    
    drawPath(
        path = path,
        brush = brush,
        alpha = alpha,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

private fun DrawScope.drawReactiveWaveform(
    startX: Float,
    activeWidth: Float,
    currentAnchorY: Float,
    amplitudes: List<Float>,
    progress: Float,
    gradient: Brush
) {
    if (activeWidth <= 0) return
    
    val amps = amplitudes.ifEmpty { List(80) { 0f } }

    val path = Path()
    val mirrorPath = Path()
    val pointsCount = amps.size
    val stepX = activeWidth / (pointsCount - 1)
    
    val maxVisualHeight = 80.dp.toPx() 
    
    for (i in amps.indices) {
        val x = startX + i * stepX
        val amp = amps[i]
        val edgeTaper = sin(i.toFloat() / (pointsCount - 1) * PI.toFloat())
        val dynamicHeight = maxVisualHeight * amp * (1f - (progress * 0.5f))
        val yOffset = dynamicHeight * edgeTaper
        
        if (i == 0) {
            path.moveTo(x, currentAnchorY - yOffset)
            mirrorPath.moveTo(x, currentAnchorY + yOffset)
        } else {
            path.lineTo(x, currentAnchorY - yOffset)
            mirrorPath.lineTo(x, currentAnchorY + yOffset)
        }
    }
    
    drawPath(
        path = path,
        brush = gradient,
        style = Stroke(
            width = 3.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
    
    drawPath(
        path = mirrorPath,
        brush = gradient,
        alpha = 0.3f,
        style = Stroke(
            width = 2.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}
