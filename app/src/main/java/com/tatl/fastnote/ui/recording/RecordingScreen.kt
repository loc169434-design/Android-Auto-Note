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
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tatl.fastnote.R
import com.tatl.fastnote.data.user.AppLanguage
import com.tatl.fastnote.data.user.LanguageManager
import com.tatl.fastnote.service.VoiceRecordingService
import com.tatl.fastnote.ui.common.AppToast
import com.tatl.fastnote.ui.theme.AndroidAutoNoteTheme
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
 */
@Composable
fun RecordingScreen(
    service: VoiceRecordingService?,
    isBound: Boolean,
    isPremiumUser: Boolean = false,
    showSavedToast: Boolean = false,
    hasExistingNotes: Boolean = false,
    onCancel: () -> Unit,
    onSaveAndExit: () -> Unit,
    onUpgradeClick: () -> Unit = {}
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
    val baseContext = LocalContext.current

    RecordingScreenContent(
        recognizedText = recognizedText,
        partialText = partialText,
        isListening = isListening,
        isPaused = isPaused,
        isBound = isBound,
        isPremiumUser = isPremiumUser,
        showSavedToast = showSavedToast,
        hasExistingNotes = hasExistingNotes,
        currentLanguage = currentLanguage,
        onLanguageSelect = { lang ->
            LanguageManager.setLanguage(baseContext, lang)
            service?.updateLanguageAndRestart()
        },
        onCancel = onCancel,
        onSaveAndExit = onSaveAndExit,
        onUpgradeClick = onUpgradeClick
    )
}

/**
 * Pure UI composable cho RecordingScreen — hoàn toàn độc lập với Service,
 * giúp xem trước (Preview) và chỉnh sửa giao diện cực kỳ nhanh chóng trên Android Studio.
 */
@SuppressLint("LocalContextConfigurationRead")
@Composable
fun RecordingScreenContent(
    recognizedText: String = "",
    partialText: String = "",
    isListening: Boolean = true,
    isPaused: Boolean = false,
    isBound: Boolean = true,
    isPremiumUser: Boolean = false,
    showSavedToast: Boolean = false,
    hasExistingNotes: Boolean = false,
    currentLanguage: AppLanguage = AppLanguage.VIETNAMESE,
    onLanguageSelect: (AppLanguage) -> Unit = {},
    onCancel: () -> Unit = {},
    onSaveAndExit: () -> Unit = {},
    onUpgradeClick: () -> Unit = {}
) {
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
    val boldRegex = Regex("\\*\\*.*\\*\\*")

    // 5 ngon ngu chuan
    val languages = listOf(
        AppLanguage.VIETNAMESE,
        AppLanguage.ENGLISH,
        AppLanguage.JAPANESE,
        AppLanguage.GERMAN,
        AppLanguage.RUSSIAN
    )

    // ── Tao locale context theo ngon ngu duoc chon ──────────────────────────
    val localizedContext = remember(currentLanguage) {
        val config = Configuration(baseContext.resources.configuration)
        try {
            config.setLocale(Locale.forLanguageTag(currentLanguage.code))
        } catch (_: Exception) {}
        baseContext.createConfigurationContext(config)
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        androidx.compose.ui.platform.LocalConfiguration provides localizedContext.resources.configuration
    ) {

        // Lay text trong locale hien tai (tu dong cap nhat khi currentLanguage thay doi)
        val saveText     = localizedContext.resources.getString(R.string.btn_save_upper)
        val cancelText   = localizedContext.resources.getString(R.string.btn_cancel_upper)
        val savedMessage = localizedContext.resources.getString(R.string.str_saved)
        val pausedText   = localizedContext.resources.getString(R.string.str_paused)
        val connectingText = localizedContext.resources.getString(R.string.str_connecting)

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

            // ── Noi dung chinh -- can giua ──────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { /* block propagation */ },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(Modifier.weight(1f))

                // -- Vong tron Mic o giua (nhan de luu & thoat) --
                Box(
                    modifier = Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onSaveAndExit() },
                    contentAlignment = Alignment.Center
                ) {
                    MicCircle(isActive = isActive, isPaused = isPaused)
                }

                Spacer(Modifier.height(20.dp))

                // -- Text block: ĐANG LẮNG NGHE + subtitle --
                if (isPaused || !isBound) {
                    // Trạng thái đặc biệt: tạm dừng hoặc chưa kết nối
                    Text(
                        text = if (isPaused) pausedText else connectingText,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 15.sp,
                        letterSpacing = 0.4.sp,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                } else {
                    // Header: ĐANG LẮNG NGHE…
                    Text(
                        text = currentLanguage.listeningText,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 1.5.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(
                                includeFontPadding = false // Removes the system font padding
                            ),
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.Both // Trims extra space from top and bottom
                            )
                        )
                    )
                    Spacer(Modifier.height(7.dp))

                    // Subtitle: thay đổi theo lần đầu hay không
                    Text(
                        text = if (!hasExistingNotes)
                            currentLanguage.firstNotePromptText
                        else
                            currentLanguage.promptText,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center,
                        lineHeight = 19.sp,
                        style = TextStyle(
                            // 2. Remove the legacy Android font padding
                            platformStyle = PlatformTextStyle(
                                includeFontPadding = false
                            ),
                            // 3. Trim the remaining line height behavior whitespace
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.Both
                            )
                        )
                    )
                }

                // -- Transcript cuon duoc --
                if (displayText.isNotBlank()) {
                    Spacer(Modifier.height(20.dp))
                    val scrollState = rememberScrollState()
                    LaunchedEffect(displayText) {
                        scrollState.animateScrollTo(scrollState.maxValue)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(125.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0F172A).copy(alpha = 0.6f))
                            .border(BorderStroke(1.dp, Color(0xFF1E293B)), RoundedCornerShape(16.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .verticalScroll(scrollState)
                    ) {
                        Text(
                            text = displayText,
                            fontFamily = NotoSansFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 15.sp,
                            lineHeight = 23.sp,
                            color = Color(0xFFF1F5F9),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                // -- Dòng thông báo đếm ngược / Khóa App (Từ ngày 29 trở đi) --
                val shouldShowBanner = com.tatl.fastnote.billing.TrialManager.shouldShowMicBanner(localizedContext, isPremiumUser)
                val bannerText = com.tatl.fastnote.billing.TrialManager.getMicBannerMessage(localizedContext)
                val isExpired = com.tatl.fastnote.billing.TrialManager.isTrialExpired(localizedContext)

                if (shouldShowBanner && bannerText != null) {
                    if (isExpired) {
                        // Ngày 31+: Card 3 dòng đầy đủ thông tin
                        val upgradeHint = localizedContext.resources.getString(R.string.str_trial_expired_upgrade_hint)
                        val noUpgradeHint = localizedContext.resources.getString(R.string.str_premium_feat_drive)
                        val titleHint = localizedContext.resources.getString(R.string.str_trial_expired_upgrade_title)
                        val titleHint1 = localizedContext.resources.getString(R.string.str_premium_feat_drive_record_title)
                        val cancelText = localizedContext.resources.getString(R.string.str_premium_feat_drive_record)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1A1A1A))
                                .border(BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.35f)), RoundedCornerShape(12.dp))
                                .clickable { onUpgradeClick() }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Tiêu đề vàng
                                Text(
                                    text = bannerText,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFFFFD700),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                // Bullet 1: Nâng cấp (1 dòng luông tuồng liền mạch, phần nâng cấp 1 lần auto bold + italic)
                                val (bullet1Prefix, bullet1Suffix) = remember(titleHint, upgradeHint) {
                                    if (upgradeHint.startsWith(": ") || upgradeHint.startsWith("：")) {
                                        val colonIndex = if (upgradeHint.startsWith(": ")) 2 else 1
                                        Pair(titleHint + upgradeHint.substring(0, colonIndex), upgradeHint.substring(colonIndex))
                                    } else if (upgradeHint.startsWith(":")) {
                                        Pair(titleHint + ": ", upgradeHint.substring(1).trimStart())
                                    } else {
                                        Pair(titleHint, upgradeHint)
                                    }
                                }
                                val bullet1Annotated = remember(bullet1Prefix, bullet1Suffix) {
                                    buildAnnotatedString {
                                        withStyle(
                                            SpanStyle(
                                                fontWeight = FontWeight.Bold,
                                                fontStyle = FontStyle.Italic,
                                                fontFamily = NotoSansFontFamily,
                                                fontSize = 12.sp,
                                                color = Color(0xFF38BDF8)
                                            )
                                        ) {
                                            append(bullet1Prefix)
                                        }
                                        withStyle(
                                            SpanStyle(
                                                fontWeight = FontWeight.Normal,
                                                fontStyle = FontStyle.Normal,
                                                fontFamily = NotoSansFontFamily,
                                                fontSize = 12.sp,
                                                color = Color(0xFF38BDF8)
                                            )
                                        ) {
                                            append(bullet1Suffix)
                                        }
                                    }
                                }
                                Text(
                                    text = bullet1Annotated,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Divider mờ
                                androidx.compose.material3.HorizontalDivider(
                                    color = Color(0xFF333333),
                                    thickness = 0.5.dp
                                )

                                // Bullet 2: Nếu không nâng cấp (1 dòng luông tuồng liền mạch, in đậm titleHint1)
                                val (bullet2Prefix, bullet2Suffix) = remember(titleHint1, cancelText) {
                                    if (cancelText.startsWith(": ") || cancelText.startsWith("：")) {
                                        val colonIndex = if (cancelText.startsWith(": ")) 2 else 1
                                        Pair(titleHint1 + cancelText.substring(0, colonIndex), cancelText.substring(colonIndex))
                                    } else if (cancelText.startsWith(":")) {
                                        Pair(titleHint1 + ": ", cancelText.substring(1).trimStart())
                                    } else {
                                        Pair(titleHint1, cancelText)
                                    }
                                }
                                val bullet2Annotated = remember(bullet2Prefix, bullet2Suffix) {
                                    buildAnnotatedString {
                                        withStyle(
                                            SpanStyle(
                                                fontWeight = FontWeight.Bold,
                                                fontStyle = FontStyle.Italic,
                                                fontFamily = NotoSansFontFamily,
                                                fontSize = 12.sp,
                                                color = Color(0xFF38BDF8)
                                            )
                                        ) {
                                            append(bullet2Prefix)
                                        }
                                        withStyle(
                                            SpanStyle(
                                                fontWeight = FontWeight.Normal,
                                                fontStyle = FontStyle.Normal,
                                                fontFamily = NotoSansFontFamily,
                                                fontSize = 12.sp,
                                                color = Color(0xFF38BDF8)
                                            )
                                        ) {
                                            append(bullet2Suffix)
                                        }
                                    }
                                }
                                Text(
                                    text = bullet2Annotated,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    } else {
                        // Ngày 29–30: banner đơn giản màu xanh
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E1E1E))
                                .border(BorderStroke(1.dp, Color(0xFF333333)), RoundedCornerShape(10.dp))
                                .clickable { onUpgradeClick() }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = bannerText,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = Color(0xFF38BDF8),
                                textAlign = TextAlign.Center,
                                lineHeight = 19.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }

                // ── Hang 2 nut hanh dong can doi o duoi: HUY (trai) & LUU (phai) ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(48.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Nút HUY (Cancel)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onCancel() }
                    ) {
                        Surface(
                            onClick = onCancel,
                            shape = CircleShape,
                            color = Color(0xFF1E293B).copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, Color(0xFF475569)),
                            modifier = Modifier.size(62.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = cancelText,
                                    tint = Color(0xFFCBD5E1),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    // Nút LƯU (Save - Biểu tượng đĩa mềm)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onSaveAndExit() }
                    ) {
                        Surface(
                            onClick = onSaveAndExit,
                            shape = CircleShape,
                            color = Color(0xFF1E293B).copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, Color(0xFF475569)),
                            modifier = Modifier.size(62.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = saveText,
                                    tint = Color(0xFFCBD5E1),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(44.dp))
            }
        }
    }
}


// -- Vòng tròn mic -- hiệu ứng "thở" chậm + 2 vòng ripple lệch pha --

@Composable
private fun MicCircle(isActive: Boolean, isPaused: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_breath")

    // Nhịp thở của vòng tròn chính: chậm, organic
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = if (isActive) 1.04f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_breath_scale"
    )

    // Hào quang 1: nhanh hơn, gần
    val halo1Scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.28f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo1_scale"
    )
    val halo1Alpha by infiniteTransition.animateFloat(
        initialValue = if (isActive) 0.28f else 0f,
        targetValue = if (isActive) 0.04f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo1_alpha"
    )

    // Hào quang 2: chậm hơn, xa hơn, lệch pha
    val halo2Scale by infiniteTransition.animateFloat(
        initialValue = 1.10f,
        targetValue = if (isActive) 1.50f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine, delayMillis = 600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo2_scale"
    )
    val halo2Alpha by infiniteTransition.animateFloat(
        initialValue = if (isActive) 0.14f else 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine, delayMillis = 600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo2_alpha"
    )

    Box(
        modifier = Modifier.size(140.dp),
        contentAlignment = Alignment.Center
    ) {
        // Vòng ripple xa (halo 2)
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(halo2Scale)
                    .clip(CircleShape)
                    .background(Color(0xFF38BDF8).copy(alpha = halo2Alpha))
            )
        }
        // Vòng ripple gần (halo 1)
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(halo1Scale)
                    .clip(CircleShape)
                    .background(Color(0xFF3B82F6).copy(alpha = halo1Alpha))
            )
        }
        // Vòng tròn chính — thở theo breathScale
        Box(
            modifier = Modifier
                .size(100.dp)
                .scale(breathScale)
                .clip(CircleShape)
                .background(MicCircleBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (!isPaused) Icons.Default.Mic else Icons.Default.MicOff,
                contentDescription = if (isActive) "Đang ghi âm" else "Mic",
                tint = MicIconColor,
                modifier = Modifier.size(42.dp)
            )
        }
    }
}

// Kept for backward compat
@Composable
fun RecordingIndicator(isListening: Boolean) {
    MicCircle(isActive = isListening, isPaused = false)
}

// ── PREVIEWS CHO ANDROID STUDIO COMPOSE ──────────────────────────────────────

@Preview(
    name = "1. Ghi âm - Sẵn sàng (Tiếng Việt)",
    showBackground = true,
    backgroundColor = 0xFF000000,
    widthDp = 390,
    heightDp = 844
)
@Composable
fun RecordingScreenPreviewDefault() {
    AndroidAutoNoteTheme {
        RecordingScreenContent(
            recognizedText = "",
            partialText = "",
            isListening = true,
            isPaused = false,
            isBound = true,
            currentLanguage = AppLanguage.VIETNAMESE
        )
    }
}

@Preview(
    name = "2. Ghi âm - Đang nhận diện văn bản",
    showBackground = true,
    backgroundColor = 0xFF000000,
    widthDp = 390,
    heightDp = 844
)
@Composable
fun RecordingScreenPreviewWithText() {
    AndroidAutoNoteTheme {
        RecordingScreenContent(
            recognizedText = "Hôm nay tôi đang kiểm tra tính năng ghi chú siêu tốc không ma sát,",
            partialText = "nhận diện giọng nói tức thì...",
            isListening = true,
            isPaused = false,
            isBound = true,
            currentLanguage = AppLanguage.VIETNAMESE
        )
    }
}

@Preview(
    name = "3. Ghi âm - Tiếng Anh (English)",
    showBackground = true,
    backgroundColor = 0xFF000000,
    widthDp = 390,
    heightDp = 844
)
@Composable
fun RecordingScreenPreviewEnglish() {
    AndroidAutoNoteTheme {
        RecordingScreenContent(
            recognizedText = "Zero-friction voice recording notes app is running smoothly.",
            partialText = "",
            isListening = true,
            isPaused = false,
            isBound = true,
            currentLanguage = AppLanguage.ENGLISH
        )
    }
}

@Preview(
    name = "4. Ghi âm - Tạm dừng (Paused)",
    showBackground = true,
    backgroundColor = 0xFF000000,
    widthDp = 390,
    heightDp = 844
)
@Composable
fun RecordingScreenPreviewPaused() {
    AndroidAutoNoteTheme {
        RecordingScreenContent(
            recognizedText = "Ghi chú đang tạm dừng",
            partialText = "",
            isListening = false,
            isPaused = true,
            isBound = true,
            currentLanguage = AppLanguage.VIETNAMESE
        )
    }
}

@Preview(
    name = "5. Ghi âm - Thông báo Đã lưu (Saved Toast)",
    showBackground = true,
    backgroundColor = 0xFF000000,
    widthDp = 390,
    heightDp = 844
)
@Composable
fun RecordingScreenPreviewSavedToast() {
    AndroidAutoNoteTheme {
        RecordingScreenContent(
            recognizedText = "",
            partialText = "",
            isListening = false,
            isPaused = false,
            isBound = true,
            showSavedToast = true,
            currentLanguage = AppLanguage.VIETNAMESE
        )
    }
}