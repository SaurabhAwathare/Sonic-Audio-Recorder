package com.saurabh.sonicaudiorecorder.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.saurabh.sonicaudiorecorder.ui.theme.SonicGradient
import com.saurabh.sonicaudiorecorder.ui.theme.SonicPurple

@Composable
fun MainRecordButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = modifier
            .size(110.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = 12.dp,
                shape = CircleShape,
                ambientColor = SonicPurple.copy(alpha = 0.5f),
                spotColor = SonicPurple
            )
            .clip(CircleShape)
            .background(SonicGradient)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Inner circle glass effect
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f))
        )
        
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Start Recording",
            tint = Color.White,
            modifier = Modifier.size(44.dp)
        )
    }
}
