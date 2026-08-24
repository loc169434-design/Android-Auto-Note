package com.tatl.fastnote.ui.recording

import android.annotation.SuppressLint
import android.content.res.Configuration
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tatl.fastnote.R
import com.tatl.fastnote.data.user.AppLanguage
import com.tatl.fastnote.data.user.LanguageManager
import com.tatl.fastnote.service.VoiceRecordingService
import com.tatl.fastnote.ui.common.AppToast
import com.tatl.fastnote.ui.theme.AppBgBlack
import com.tatl.fastnote.ui.theme.InterFontFamily
import com.tatl.fastnote.ui.theme.NotoSansFontFamily
import java.util.Locale

// Mau dung chung
private val MicCircleBg  = Color(0xFFEEEEEE)
private val MicIconColor = Color(0xFF111111)
private val TextPrimary  = Color(0xFFFFFFFF)
private val TextMuted    = Color(0xFF888888)
private val CancelBorder = Color(0xFF444444)

/**
 * Recording screen -- OLED black, full screen.
 *
 * Layout:
 *  - Nut LUU (theo ngon ngu) -- goc tren trai
 *  - Chon ngon ngu -- goc tren phai
 *  - Vong tron mic lon -- giua man hinh
 *  - Transcript cuon duoc, auto-scroll moi nhat len tren
 *  - Nut HUY -- duoi cung
 *
 * Locale strategy: CompositionLocalProvider overrides LocalContext with a
 * locale-aware context built from currentLanguage. This makes stringResource()
 * immediately return strings in the selected language WITHOUT recreating the
 * Activity. No activity restart = no "app exit" feel.
 */
@SuppressLint("LocalContextConfigurationRead")
@Composable
fun RecordingScreen(
    service: VoiceRecordingService?,
    isBound: Boolean,
    showSavedToast: Boolean = false,
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

    val baseContext = LocalContext.current
    var showLangMenu by remember { mutableStateOf(false) }

    // 5 ngon ngu chuan
    val languages = listOf(
        AppLanguage.VIETNAMESE,
        AppLanguage.ENGLISH,
        AppLanguage.JAPANESE,
        AppLanguage.GERMAN,
        AppLanguage.RUSSIAN
    )

    // ── Tao locale context theo ngon ngu duoc chon ──────────────────────────
    // createConfigurationContext() tao context moi voi locale chinh xac.
    // CompositionLocalProvider(LocalContext provides ...) override context cho
    // toan bo cay composable ben duoi, nen stringResource() tra ve dung ngon ngu
    // MA KHONG CAN recreate() Activity.
    val localizedContext = remember(currentLanguage) {
        val config = Configuration(baseContext.resources.configuration)
        config.setLocale(Locale(currentLanguage.code))
        baseContext.createConfigurationContext(config)
    }

    CompositionLocalProvider(LocalContext provides localizedContext) {

        // Lay text trong locale hien tai (tu dong cap nhat khi currentLanguage thay doi)
        val saveText   = stringResource(R.string.btn_save_upper)
        val cancelText = stringResource(R.string.btn_cancel_upper)
        val savedMessage = stringResource(R.string.str_saved)

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

            // ── Nut Chon Ngon Ngu -- goc tren phai ──────────────────────────
            // Pill button: toan bo vung pill la vung bam + ripple.
            // clip() truoc clickable() de ripple bi cat gon trong pill.
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 44.dp, end = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF1C1C1C))
                        .clickable { showLangMenu = true }
                        .defaultMinSize(minWidth = 88.dp, minHeight = 48.dp)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = currentLanguage.flagEmoji,
                            fontSize = 16.sp
                        )
                        Text(
                            text = langLabel,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            letterSpacing = 0.8.sp,
                            color = Color(0xFFCCCCCC)
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color(0xFF888888),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                // Dropdown ngon ngu
                DropdownMenu(
                    expanded = showLangMenu,
                    onDismissRequest = { showLangMenu = false },
                    modifier = Modifier
                        .background(Color(0xFF1A1A1A))
                        .clip(RoundedCornerShape(12.dp))
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
                                        text = "${lang.shortCode} — ${lang.displayName}",
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
                                    tint = Color(0xFF7CFC7C),
                                    modifier = Modifier.size(14.dp)
                                )
                            }) else null,
                            onClick = {
                                if (!isSelected) {
                                    // Luu preference + cap nhat StateFlow
                                    LanguageManager.setLanguage(baseContext, lang)
                                    // Cap nhat ngon ngu nhan dang giong noi trong service
                                    service?.updateLanguageAndRestart()
                                    // Khong can recreate() — CompositionLocalProvider
                                    // tu dong recompose voi locale moi ngay lap tuc
                                }
                                showLangMenu = false
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = TextMuted,
                                trailingIconColor = Color.White
                            ),
                            modifier = Modifier.background(
                                if (isSelected) Color(0xFF252525) else Color.Transparent
                            )
                        )
                    }
                }
            }

            // ── Noi dung chinh -- can giua ──────────────────────────────────
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

                // -- Nut LUU lon o giua (nhan de luu) --
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onSaveAndExit() }
                ) {
                    MicCircle(isActive = isActive, isPaused = isPaused)
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = saveText,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        letterSpacing = 2.sp,
                        color = Color.White
                    )
                }

                Spacer(Modifier.height(36.dp))

                // -- Dong chu huong dan --
                Text(
                    text = when {
                        isPaused -> stringResource(R.string.str_paused)
                        !isBound -> stringResource(R.string.str_connecting)
                        else     -> currentLanguage.promptText
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
                                    color = Color(0x22FFFFFF),
                                    topLeft = Offset(x, 0f),
                                    size = Size(barWidth, trackHeight),
                                    cornerRadius = CornerRadius(barWidth / 2)
                                )
                                drawRoundRect(
                                    color = Color(0x88FFFFFF.toInt()),
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

            // -- Custom Toast: hien khi luu xong --
            AppToast(
                visible = showSavedToast,
                message = savedMessage,
                durationMs = 1400L,
                onDismiss = { /* Activity tu finish */ }
            )
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
            contentDescription = if (isActive) "Đang ghi âm" else "Mic",
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