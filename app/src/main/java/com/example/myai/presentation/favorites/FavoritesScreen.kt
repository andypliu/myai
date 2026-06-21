package com.example.myai.presentation.favorites

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.layout
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.HeartBroken
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myai.data.local.MessageFeedback
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sin
import kotlin.random.Random

private data class MagicParticle(
    val x: Float,
    val y: Float,
    val speed: Float,
    val size: Float,
    val color: Color,
    val drift: Float
)

@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel
) {
    val lovedMessages by viewModel.lovedMessages.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header with colored background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Loved Messages",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        if (lovedMessages.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No loved messages yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(lovedMessages, key = { it.messageId }) { message ->
                    LovedMessageItem(
                        message = message,
                        onFeedback = { isLiked ->
                            if (isLiked == null) viewModel.removeFavorite(message.messageId)
                            else viewModel.toggleFeedback(message.messageId, isLiked)
                        },
                        modifier = Modifier.animateItem(
                            fadeOutSpec = tween(durationMillis = 6000), // Keep bubbles alive while shifting
                            placementSpec = tween(durationMillis = 300) // Snappy, immediate shift up
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun LovedMessageItem(
    message: MessageFeedback,
    onFeedback: (Boolean?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showFeedbackPopup by remember { mutableStateOf(false) }
    var isRemoving by remember { mutableStateOf(false) }
    var pendingFeedback by remember { mutableStateOf<Boolean?>(null) }

    // Animation progress for dissolving effect - ultra-long magic burst
    val dissolveProgress by animateFloatAsState(
        targetValue = if (isRemoving) 1f else 0f,
        animationSpec = tween(durationMillis = 8000, easing = LinearEasing),
        label = "dissolveProgress"
    )

    // Trigger feedback/shift immediately after the card fades out (around 2.0s)
    // This removes the "white space" waiting period while keeping bubbles visible
    var feedbackTriggered by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(isRemoving, dissolveProgress) {
        if (isRemoving && dissolveProgress >= 0.25f && !feedbackTriggered) {
            feedbackTriggered = true
            onFeedback(pendingFeedback)
        }
    }

    // Generate stable random particles with colors and sizes
    val density = LocalDensity.current
    val particles = remember(density) {
        with(density) {
            List(150) {
                MagicParticle(
                    x = Random.nextFloat(),
                    y = Random.nextFloat(),
                    speed = Random.nextFloat() * 2.0f + 1.0f, // Even slower float for 8s duration
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
            .fillMaxWidth()
            .graphicsLayer(clip = false)
    ) {
        // The main content dissolves with a jittery, fading effect
        Column(
            modifier = Modifier.graphicsLayer {
                // Content fades out (first 25% of 8s = 2.0s)
                val contentAlpha = (1f - (dissolveProgress / 0.25f)).coerceIn(0f, 1f)
                alpha = if (isRemoving) contentAlpha else 1f
                
                if (isRemoving && contentAlpha > 0) {
                    // Magic jitter/vibration
                    translationX = (Random.nextFloat() - 0.5f) * 12f * dissolveProgress
                    translationY = (Random.nextFloat() - 0.5f) * 12f * dissolveProgress
                    // Subtle expansion as it turns into particles
                    scaleX = 1f + (dissolveProgress * 0.1f)
                    scaleY = 1f + (dissolveProgress * 0.1f)
                }
            }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(message.messageId) {
                        detectTapGestures(
                            onLongPress = {
                                if (!isRemoving) {
                                    showFeedbackPopup = !showFeedbackPopup
                                }
                            }
                        )
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = message.model,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val sdf = remember { SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()) }
                            val dateString = sdf.format(Date(message.timestamp))
                            Text(
                                text = dateString,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Feedback selection popup
            if (showFeedbackPopup) {
                Row(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            pendingFeedback = false
                            isRemoving = true
                            showFeedbackPopup = false
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.HeartBroken,
                            contentDescription = "Dislove",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            pendingFeedback = null
                            isRemoving = true
                            showFeedbackPopup = false
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Magic spots layer - sparkles and floats upward for the full 5 seconds
        if (isRemoving) {
            Canvas(modifier = Modifier.matchParentSize()) {
                particles.forEachIndexed { index, p ->
                    // Sparkle effect using sine wave
                    val sparkle = (sin(dissolveProgress * 15f + index.toFloat()) + 1f) / 2f
                    
                    // Fade in quickly, then fade out slowly
                    val pAlpha = if (dissolveProgress < 0.1f) (dissolveProgress / 0.1f) 
                                else (1f - dissolveProgress).coerceIn(0f, 1f)
                    
                    // Upward floating motion with drift
                    val xPos = (p.x + p.drift * dissolveProgress) * size.width
                    val yPos = (p.y - p.speed * dissolveProgress) * size.height
                    
                    // Main glowy particle
                    drawCircle(
                        color = p.color.copy(alpha = pAlpha * (0.4f + 0.6f * sparkle)),
                        radius = p.size * (1f - dissolveProgress * 0.3f),
                        center = Offset(xPos, yPos)
                    )
                    
                    // Outer magical halo
                    if (sparkle > 0.7f) {
                        drawCircle(
                            color = p.color.copy(alpha = pAlpha * 0.2f),
                            radius = p.size * 3f,
                            center = Offset(xPos, yPos)
                        )
                    }
                }
            }
        }
    }
}
