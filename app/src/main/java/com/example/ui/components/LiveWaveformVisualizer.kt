package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun LiveWaveformVisualizer(
    isListening: Boolean,
    rmsLevel: Float,
    modifier: Modifier = Modifier,
    barCount: Int = 18,
    maxHeight: Dp = 48.dp,
    minHeight: Dp = 6.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")

    val anim1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )

    val anim2 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )

    val anim3 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.tertiary
    val inactiveColor = MaterialTheme.colorScheme.outlineVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(maxHeight),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val anims = listOf(anim1, anim2, anim3, anim2, anim1, anim3)
        for (i in 0 until barCount) {
            val animFactor = anims[i % anims.size]
            val heightFraction = if (isListening) {
                (0.15f + (rmsLevel * 0.7f) + (animFactor * 0.25f)).coerceIn(0.1f, 1.0f)
            } else {
                0.08f
            }

            val currentHeight = minHeight + (maxHeight - minHeight) * heightFraction

            val barBrush = if (isListening) {
                Brush.verticalGradient(
                    colors = listOf(
                        primaryColor,
                        secondaryColor
                    )
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(
                        inactiveColor,
                        inactiveColor.copy(alpha = 0.5f)
                    )
                )
            }

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(currentHeight)
                    .clip(RoundedCornerShape(4.dp))
                    .background(barBrush)
            )
        }
    }
}
