package com.example.androidautonote.ui.recording

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidautonote.service.VoiceRecordingService
import com.example.androidautonote.util.DateUtils
import kotlin.math.PI
import kotlin.math.sin

/**
 * Recording dialog UI — redesigned with waveform animation,
 * glowing red mic button, and polished controls.
 */
@Composable
fun RecordingScreen(
    service: VoiceRecordingService?,
    isBound: Boolean,
    onCancel: () -> Unit,
    onPause: () -> Unit = {},
    onResume: () -> Unit = {},
    onManualStop: () -> Unit = {}
) {
    val recognizedText by (service?.recognizedText
        ?: kotlinx.coroutines.flow.MutableStateFlow("")).collectAsState()
    val partialText by (service?.partialText
        ?: kotlinx.coroutines.flow.MutableStateFlow("")).collectAsState()
    val isListening by (service?.isListening
        ?: kotlinx.coroutines.flow.MutableStateFlow(false)).collectAsState()
    val isPaused by (service?.isPaused
        ?: kotlinx.coroutines.flow.MutableStateFlow(false)).collectAsState()
    val seconds by (service?.recordingSeconds
        ?: kotlinx.coroutines.flow.MutableStateFlow(0)).collectAsState()

    val isActive = isListening && !isPaused

    val displayText = buildString {
        if (recognizedText.isNotBlank()) append(recognizedText)
        if (partialText.isNotBlank()) {
            if (isNotBlank()) append(". ")
            append(partialText)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onCancel() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { /* Block dismiss */ },
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // === Header ===
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ghi chú nhanh",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // === Timer ===
                Text(
                    text = DateUtils.formatDuration(seconds),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // === Waveform + Mic button ===
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Animated waveform behind mic
                    AudioWaveform(
                        isActive = isActive,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Glowing mic button
                    GlowingMicButton(isActive = isActive)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // === Text display area ===
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (displayText.isBlank()) {
                        Text(
                            text = "Văn bản đang được ghi âm...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 22.sp
                        )
                    }
                }

                // === Status text ===
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when {
                        !isBound -> "Đang khởi tạo..."
                        isPaused -> "⏸ Tạm dừng"
                        isListening -> "🎙 Đang nghe... (tự lưu khi dừng nói)"
                        else -> "Sẵn sàng"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // === Control buttons ===
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cancel
                    ControlButton(
                        icon = Icons.Default.Close,
                        label = "Hủy",
                        backgroundColor = Color(0xFFFFCDD2),
                        iconColor = Color(0xFFC62828),
                        size = 52,
                        onClick = onCancel
                    )

                    // Pause / Resume
                    ControlButton(
                        icon = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        label = if (isPaused) "Tiếp" else "Dừng",
                        backgroundColor = Color(0xFFE8EAF6),
                        iconColor = Color(0xFF3949AB),
                        size = 60,
                        onClick = { if (isPaused) onResume() else onPause() }
                    )

                    // Stop & Save
                    ControlButton(
                        icon = Icons.Default.Stop,
                        label = "Xong",
                        backgroundColor = Color(0xFFC8E6C9),
                        iconColor = Color(0xFF2E7D32),
                        size = 52,
                        onClick = onManualStop
                    )
                }
            }
        }
    }
}

// ============================================================
// Animated Audio Waveform
// ============================================================

@Composable
fun AudioWaveform(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")

    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase1"
    )
    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase2"
    )
    val phase3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase3"
    )

    val waveAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.15f,
        animationSpec = tween(500),
        label = "wave_alpha"
    )

    val waveAmplitude by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.2f,
        animationSpec = tween(500),
        label = "wave_amp"
    )

    Canvas(modifier = modifier.alpha(waveAlpha)) {
        val centerY = size.height / 2f
        val width = size.width

        // Wave colors — purple/blue gradient like the reference image
        val waveColors = listOf(
            Color(0xFF7C4DFF).copy(alpha = 0.4f),  // Deep purple
            Color(0xFF448AFF).copy(alpha = 0.3f),  // Blue
            Color(0xFFB388FF).copy(alpha = 0.25f)  // Light purple
        )

        val phases = listOf(phase1, phase2, phase3)
        val amplitudes = listOf(30f, 22f, 18f)
        val frequencies = listOf(1.5f, 2.2f, 3f)
        val strokeWidths = listOf(3f, 2.5f, 2f)

        waveColors.forEachIndexed { index, color ->
            val path = Path()
            val amp = amplitudes[index] * waveAmplitude
            val freq = frequencies[index]
            val phase = phases[index]

            path.moveTo(0f, centerY)
            for (x in 0..width.toInt() step 2) {
                val xRatio = x / width
                // Envelope — fade at edges, strong in center
                val envelope = sin(xRatio * PI.toFloat()) * 1.2f
                val y = centerY + sin(xRatio * freq * 2 * PI.toFloat() + phase) * amp * envelope
                path.lineTo(x.toFloat(), y)
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(width = strokeWidths[index] * (if (isActive) 1.5f else 1f), cap = StrokeCap.Round)
            )
        }
    }
}

// ============================================================
// Glowing Mic Button with pulse rings
// ============================================================

@Composable
fun GlowingMicButton(isActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")

    // Outer pulse ring 1
    val ring1Scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.8f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1"
    )
    val ring1Alpha by infiniteTransition.animateFloat(
        initialValue = if (isActive) 0.5f else 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1_alpha"
    )

    // Outer pulse ring 2 (delayed)
    val ring2Scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 2.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2"
    )
    val ring2Alpha by infiniteTransition.animateFloat(
        initialValue = if (isActive) 0.3f else 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2_alpha"
    )

    // Mic button breathe
    val micScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_breathe"
    )

    val micColor = if (isActive) Color(0xFFE53935) else Color(0xFF9E9E9E)
    val micBgColor = if (isActive) Color(0xFFFFCDD2) else Color(0xFFE0E0E0)

    Box(contentAlignment = Alignment.Center) {
        // Pulse ring 2 (outer)
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .scale(ring2Scale)
                    .alpha(ring2Alpha)
                    .background(Color(0xFFE53935).copy(alpha = 0.15f), CircleShape)
            )
        }

        // Pulse ring 1
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .scale(ring1Scale)
                    .alpha(ring1Alpha)
                    .background(Color(0xFFE53935).copy(alpha = 0.25f), CircleShape)
            )
        }

        // Main mic circle
        Box(
            modifier = Modifier
                .size(72.dp)
                .scale(micScale)
                .shadow(
                    elevation = if (isActive) 12.dp else 4.dp,
                    shape = CircleShape,
                    ambientColor = if (isActive) Color(0xFFE53935) else Color.Transparent,
                    spotColor = if (isActive) Color(0xFFE53935) else Color.Transparent
                )
                .background(
                    brush = if (isActive) {
                        Brush.radialGradient(
                            colors = listOf(Color(0xFFFF5252), Color(0xFFE53935))
                        )
                    } else {
                        Brush.radialGradient(
                            colors = listOf(Color(0xFFBDBDBD), Color(0xFF9E9E9E))
                        )
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Mic,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = Color.White
            )
        }
    }
}

// ============================================================
// Control Button Component
// ============================================================

@Composable
private fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    backgroundColor: Color,
    iconColor: Color,
    size: Int,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(size.dp)
                .shadow(4.dp, CircleShape)
                .background(backgroundColor, CircleShape)
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size((size * 0.5f).dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}
