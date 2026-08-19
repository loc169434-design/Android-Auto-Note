package com.tatl.fastnote.ui.recording

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tatl.fastnote.data.user.AppLanguage
import com.tatl.fastnote.data.user.LanguageManager
import com.tatl.fastnote.service.VoiceRecordingService
import com.tatl.fastnote.ui.theme.AppBgBlack
import com.tatl.fastnote.ui.theme.InterFontFamily
import com.tatl.fastnote.ui.theme.NotoSansFontFamily

// Mau dung chung
private val MicCircleBg  = Color(0xFFEEEEEE)
private val MicIconColor = Color(0xFF111111)
private val TextPrimary  = Color(0xFFFFFFFF)
private val TextMuted    = Color(0xFF888888)
private val CancelBorder = Color(0xFF444444)
private val SaveBorder   = Color(0xFF555555)

/**
 * Recording screen -- OLED black, full screen.
 *
 * Layout:
 *  - Nut LUU (theo ngon ngu) -- goc tren trai
 *  - Chon ngon ngu -- goc tren phai
 *  - Vong tron mic lon -- giua man hinh
 *  - Transcript cuon duoc, auto-scroll moi nhat len tren
 *  - Nut HUY -- duoi cung
 */
@Composable
fun RecordingScreen(
    service: VoiceRecordingService?,
    isBound: Boolean,
    onCancel: () -> Unit,
    onSaveAndExit: () -> Unit
) {
    val recognizedText by (service?.recognizedText
        ?: kotlinx.coroutines.flow.MutableStateFlow("")).collectAsState()
    val partialText by (service?.partialText
        ?: kotlinx.coroutines.flow.MutableStateFlow("")).collectAsState()
    val isListening by (service?.isListening
        ?: kotlinx.coroutines.flow.MutableStateFlow(false)).collectAsState()
    val isPaused by (service?.isPaused
        ?: kotlinx.coroutines.flow.MutableStateFlow(false)).collectAsState()

    val currentLanguage by LanguageManager.currentLanguage.collectAsState()
    val langLabel = currentLanguage.shortCode

    val displayText = buildString {
        if (recognizedText.isNotBlank()) append(recognizedText)
        if (partialText.isNotBlank()) {
            if (isNotBlank()) append(" ")
            append(partialText)
        }
    }
    val isActive = isListening && !isPaused

    val context = LocalContext.current
    var showLangMenu by remember { mutableStateOf(false) }

    // 5 ngon ngu chuan
    val languages = listOf(
        AppLanguage.VIETNAMESE,
        AppLanguage.ENGLISH,
        AppLanguage.JAPANESE,
        AppLanguage.GERMAN,
        AppLanguage.RUSSIAN
    )

    // Van ban nut LUU theo ngon ngu
    val saveText = when (currentLanguage) {
        AppLanguage.VIETNAMESE -> "Lu01afU"
        AppLanguage.ENGLISH    -> "SAVE"
        AppLanguage.JAPANESE   -> "u4fddu5b58"
        AppLanguage.GERMAN     -> "SPEICHERN"
        AppLanguage.RUSSIAN    -> "u0421u041eu0425u0420u0410u041du0418u0422u042c"
    }

    // Van ban nut HUY theo ngon ngu
    val cancelText = when (currentLanguage) {
        AppLanguage.VIETNAMESE -> "Hu1ee6Y"
        AppLanguage.ENGLISH    -> "CANCEL"
        AppLanguage.JAPANESE   -> "u30adu30e3u30f3u30bbu30eb"
        AppLanguage.GERMAN     -> "ABBRECHEN"
        AppLanguage.RUSSIAN    -> "u041eu0422u041cu0415u041du0410"
    }

    // Tap ngoai = luu va thoat
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBgBlack)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onSaveAndExit() }
    ) {
        // -- Nut LUU -- goc tren trai --
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 52.dp, start = 24.dp)
        ) {
            Text(
                text = saveText,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                letterSpacing = 1.sp,
                color = Color.White,
                modifier = Modifier
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onSaveAndExit() }
            )
        }

        // -- Nut Chon Ngon Ngu -- goc tren phai --
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 52.dp, end = 24.dp)
        ) {
            Text(
                text = langLabel,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                letterSpacing = 1.sp,
                color = TextMuted,
                modifier = Modifier
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { showLangMenu = true }
            )

            // Dropdown ngon ngu
            DropdownMenu(
                expanded = showLangMenu,
                onDismissRequest = { showLangMenu = false },
                modifier = Modifier.background(Color(0xFF1A1A1A))
            ) {
                languages.forEach { lang ->
                    val isSelected = lang == currentLanguage
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(text = lang.flagEmoji, fontSize = 16.sp)
                                Text(
                                    text = "${lang.shortCode} - ${lang.displayName}",
                                    fontFamily = InterFontFamily,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = 14.sp,
                                    color = if (isSelected) Color.White else TextMuted
                                )
                            }
                        },
                        trailingIcon = if (isSelected) ({
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }) else null,
                        onClick = {
                            LanguageManager.setLanguage(context, lang)
                            service?.updateLanguageAndRestart()
                            showLangMenu = false
                        },
                        colors = MenuDefaults.itemColors(textColor = TextMuted)
                    )
                }
            }
        }

        // -- Noi dung chinh -- can giua --
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { /* block propagation */ },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.weight(1f))

            // -- Vong tron mic lon --
            Box {
                MicCircle(isActive = isActive, isPaused = isPaused)
            }

            Spacer(Modifier.height(36.dp))

            // -- Dong chu huong dan --
            Text(
                text = when {
                    isPaused -> when (currentLanguage) {
                        AppLanguage.VIETNAMESE -> "u0110u00c3 Tu1ea0M Du1ebeNG"
                        AppLanguage.ENGLISH    -> "PAUSED"
                        AppLanguage.JAPANESE   -> "u4e00u6642u505cu6b62u4e2d"
                        AppLanguage.GERMAN     -> "PAUSIERT"
                        AppLanguage.RUSSIAN    -> "u041fu0420u0418u041eu0421u0422u0410u041du041eu0412u041bu0415u041du041e"
                    }
                    !isBound -> when (currentLanguage) {
                        AppLanguage.VIETNAMESE -> "u0110ANG Ku1ebeT Nu1ed0I..."
                        AppLanguage.ENGLISH    -> "CONNECTING..."
                        AppLanguage.JAPANESE   -> "u63a5u7d9au4e2d..."
                        AppLanguage.GERMAN     -> "VERBINDEN..."
                        AppLanguage.RUSSIAN    -> "u041fu041eu0414u041au041bu042eu0427u0415u041du0418u0415..."
                    }
                    else -> currentLanguage.promptText
                },
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                letterSpacing = 0.5.sp,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            // -- Transcript cuon duoc --
            if (displayText.isNotBlank()) {
                Spacer(Modifier.height(28.dp))
                val scrollState = rememberScrollState()
                // Auto-scroll xuong cuoi moi khi co text moi
                LaunchedEffect(displayText) {
                    scrollState.animateScrollTo(scrollState.maxValue)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .drawBehind {
                            val barWidth = 3.dp.toPx()
                            val trackHeight = size.height
                            val maxScroll = scrollState.maxValue.toFloat()
                            val scrollFraction = if (maxScroll > 0f) scrollState.value / maxScroll else 0f
                            val thumbHeightFraction = trackHeight / (trackHeight + maxScroll).coerceAtLeast(1f)
                            val thumbHeight = (trackHeight * thumbHeightFraction).coerceAtLeast(24.dp.toPx())
                            val thumbTop = (trackHeight - thumbHeight) * scrollFraction
                            val x = size.width - barWidth - 2.dp.toPx()
                            drawRoundRect(
                                color = androidx.compose.ui.graphics.Color(0x22FFFFFF),
                                topLeft = Offset(x, 0f),
                                size = Size(barWidth, trackHeight),
                                cornerRadius = CornerRadius(barWidth / 2)
                            )
                            drawRoundRect(
                                color = androidx.compose.ui.graphics.Color(0x88FFFFFF.toInt()),
                                topLeft = Offset(x, thumbTop),
                                size = Size(barWidth, thumbHeight),
                                cornerRadius = CornerRadius(barWidth / 2)
                            )
                        }
                        .verticalScroll(scrollState)
                ) {
                    Text(
                        text = displayText,
                        fontFamily = NotoSansFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 15.sp,
                        lineHeight = 24.sp,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // -- Nut HUY -- circle outlined --
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                border = BorderStroke(1.dp, CancelBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = TextMuted
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Text(
                    text = cancelText,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = if (cancelText.length > 6) 10.sp else 13.sp,
                    letterSpacing = 1.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(56.dp))
        }
    }
}

// -- Vong tron mic -- trang lon, pulse khi active --

@Composable
private fun MicCircle(isActive: Boolean, isPaused: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.06f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_scale"
    )

    Box(
        modifier = Modifier
            .size(110.dp)
            .scale(if (isActive) scale else 1f)
            .clip(CircleShape)
            .background(MicCircleBg),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (!isPaused) Icons.Default.Mic else Icons.Default.MicOff,
            contentDescription = if (isActive) "u0110ang ghi u00e2m" else "Mic",
            tint = MicIconColor,
            modifier = Modifier.size(44.dp)
        )
    }
}

// Kept for backward compat
@Composable
fun RecordingIndicator(isListening: Boolean) {
    MicCircle(isActive = isListening, isPaused = false)
}