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

// ── Màu dùng chung từ AppColors ───────────────────────────────────────────────
private val MicCircleBg   = Color(0xFFEEEEEE)   // vòng tròn mic màu trắng xám
private val MicIconColor  = Color(0xFF111111)   // icon mic đen
private val TextPrimary   = Color(0xFFFFFFFF)
private val TextMuted     = Color(0xFF888888)
private val CancelBorder  = Color(0xFF444444)

/**
 * Recording screen — OLED black, full screen (Bản Đặc Tả V38 - Phần 3).
 *
 * Layout:
 *  - Nút chuyển đổi ngôn ngữ (VN / EN / JP / DE / RU) góc trên cùng bên phải
 *  - Vòng tròn trắng lớn + icon mic đen — giữa màn hình
 *  - Dòng chữ hướng dẫn động theo ngôn ngữ đã chọn:
 *      * VN: "HÃY NÓI ĐIỀU BẠN MUỐN GHI CHÚ"
 *      * EN: "PLEASE SAY WHAT YOU WANT TO NOTE"
 *      * JP: "メモしたい内容を話してください"
 *      * DE: "BITTE SPRECHEN SIE, WAS SIE NOTIEREN MÖCHTEN"
 *      * RU: "ПОЖАЛУЙСТА, СКАЖИТЕ, ЧТО ВЫ ХОТИТЕ ЗАПИСАТЬ"
 *  - Transcript (ẩn nếu trống) — cuộn được
 *  - Nút "HỦY" — circle outlined ở dưới cùng
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

    // 5 ngôn ngữ chuẩn theo V38
    val languages = listOf(
        AppLanguage.VIETNAMESE,
        AppLanguage.ENGLISH,
        AppLanguage.JAPANESE,
        AppLanguage.GERMAN,
        AppLanguage.RUSSIAN
    )

    // Tap ngoài = lưu và thoát
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBgBlack)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onSaveAndExit() }
    ) {
        // ── Nút Chọn Ngôn Ngữ Tức Thời (VN / EN / JP / DE / RU) — góc trên phải ──
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 52.dp, end = 24.dp)
        ) {
            // Label viết tắt bấm được
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

            // Dropdown menu — nền đen tuyền, viền xám
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
                                Text(
                                    text = lang.flagEmoji,
                                    fontSize = 16.sp
                                )
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
                        colors = MenuDefaults.itemColors(
                            textColor = TextMuted
                        )
                    )
                }
            }
        }

        // ── Nội dung chính — căn giữa ────────────────────────────────────────
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

            // ── Vòng tròn mic lớn, trắng — chỉ hiện thị, không cần tap để lưu ────────
            Box {
                MicCircle(isActive = isActive, isPaused = isPaused)
            }

            Spacer(Modifier.height(36.dp))

            // ── Dòng chữ hướng dẫn động (V38 Phần 3) ─────────────────────────
            Text(
                text = when {
                    isPaused -> when (currentLanguage) {
                        AppLanguage.VIETNAMESE -> "ĐÃ TẠM DỪNG"
                        AppLanguage.ENGLISH    -> "PAUSED"
                        AppLanguage.JAPANESE   -> "一時停止中"
                        AppLanguage.GERMAN     -> "PAUSIERT"
                        AppLanguage.RUSSIAN    -> "ПРИОСТАНОВЛЕНО"
                    }
                    !isBound -> when (currentLanguage) {
                        AppLanguage.VIETNAMESE -> "ĐANG KẾT NỐI..."
                        AppLanguage.ENGLISH    -> "CONNECTING..."
                        AppLanguage.JAPANESE   -> "接続中..."
                        AppLanguage.GERMAN     -> "VERBINDEN..."
                        AppLanguage.RUSSIAN    -> "ПОДКЛЮЧЕНИЕ..."
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

            // ── Transcript (chỉ hiện khi có nội dung) ────────────────────────
            if (displayText.isNotBlank()) {
                Spacer(Modifier.height(28.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = displayText,
                        fontFamily = NotoSansFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 15.sp,
                        lineHeight = 24.sp,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // ── Nút HỦY — circle outlined ────────────────────────────────────
            val cancelText = when (currentLanguage) {
                AppLanguage.VIETNAMESE -> "HỦY"
                AppLanguage.ENGLISH    -> "CANCEL"
                AppLanguage.JAPANESE   -> "キャンセル"
                AppLanguage.GERMAN     -> "ABBRECHEN"
                AppLanguage.RUSSIAN    -> "ОТМЕНА"
            }

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

// ── Vòng tròn mic — trắng lớn, pulse khi active ────────────────────────────────

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
