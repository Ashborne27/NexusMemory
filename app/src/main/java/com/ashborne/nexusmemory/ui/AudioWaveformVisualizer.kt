package com.ashborne.nexusmemory.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.random.Random

@Composable
fun AudioWaveformVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
    ) {
        val width = size.width
        val height = size.height
        val barWidth = 6f
        val gap = 4f
        val totalBars = (width / (barWidth + gap)).toInt()

        for (i in 0 until totalBars) {
            val x = i * (barWidth + gap)
            val factor = if (isPlaying) {
                val seed = (i + (phase * 10).toInt()) % 10
                0.2f + (seed / 10f) * 0.8f
            } else {
                0.15f
            }
            val barHeight = height * factor
            val top = (height - barHeight) / 2

            drawLine(
                color = if (isPlaying) barColor else barColor.copy(alpha = 0.3f),
                start = Offset(x, top),
                end = Offset(x, top + barHeight),
                strokeWidth = barWidth
            )
        }
    }
}
