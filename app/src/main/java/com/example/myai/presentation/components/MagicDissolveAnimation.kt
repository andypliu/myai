package com.example.myai.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun MagicDissolveAnimation(
    isRemoving: Boolean,
    onAnimationTriggered: () -> Unit = {},
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable () -> Unit
) {
    val dissolveProgress by animateFloatAsState(
        targetValue = if (isRemoving) 1f else 0f,
        animationSpec = tween(durationMillis = 8000, easing = LinearEasing),
        label = "dissolveProgress"
    )

    var feedbackTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(isRemoving, dissolveProgress) {
        if (isRemoving && dissolveProgress >= 0.25f && !feedbackTriggered) {
            feedbackTriggered = true
            onAnimationTriggered()
        }
    }

    val density = LocalDensity.current
    val particles = remember(density) {
        with(density) {
            List(150) {
                MagicParticle(
                    x = Random.nextFloat(),
                    y = Random.nextFloat(),
                    speed = Random.nextFloat() * 2.0f + 1.0f,
                    size = Random.nextFloat() * 4.dp.toPx() + 2.dp.toPx(),
                    color = when(Random.nextInt(3)) {
                        0 -> Color(0xFFFFD700) // Gold
                        1 -> Color(0xFFAEAFFF) // Soft Blue
                        else -> Color.White
                    },
                    drift = Random.nextFloat() * 0.8f - 0.4f
                )
            }
        }
    }

    Box(
        modifier = modifier
            .graphicsLayer(clip = false),
        contentAlignment = contentAlignment
    ) {
        Box(
            modifier = Modifier.graphicsLayer {
                val contentAlpha = (1f - (dissolveProgress / 0.25f)).coerceIn(0f, 1f)
                alpha = if (isRemoving) contentAlpha else 1f

                if (isRemoving && contentAlpha > 0) {
                    translationX = (Random.nextFloat() - 0.5f) * 12f * dissolveProgress
                    translationY = (Random.nextFloat() - 0.5f) * 12f * dissolveProgress
                    scaleX = 1f + (dissolveProgress * 0.1f)
                    scaleY = 1f + (dissolveProgress * 0.1f)
                }
            }
        ) {
            content()
        }

        if (isRemoving) {
            Canvas(modifier = Modifier.matchParentSize()) {
                particles.forEachIndexed { index, p ->
                    val sparkle = (sin(dissolveProgress * 15f + index.toFloat()) + 1f) / 2f

                    val pAlpha = if (dissolveProgress < 0.1f) (dissolveProgress / 0.1f)
                    else (1f - dissolveProgress).coerceIn(0f, 1f)

                    val xPos = (p.x + p.drift * dissolveProgress) * size.width
                    val yPos = (p.y - p.speed * dissolveProgress) * size.height

                    drawCircle(
                        color = p.color.copy(alpha = pAlpha * (0.4f + 0.6f * sparkle)),
                        radius = p.size * (1f - dissolveProgress * 0.3f),
                        center = androidx.compose.ui.geometry.Offset(xPos, yPos)
                    )

                    if (sparkle > 0.7f) {
                        drawCircle(
                            color = p.color.copy(alpha = pAlpha * 0.2f),
                            radius = p.size * 3f,
                            center = androidx.compose.ui.geometry.Offset(xPos, yPos)
                        )
                    }
                }
            }
        }
    }
}