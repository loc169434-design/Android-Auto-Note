package com.tatl.fastnote.ui.fileviewer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tatl.fastnote.ui.theme.AppBgBlack
import com.tatl.fastnote.ui.theme.InterFontFamily
import com.tatl.fastnote.ui.theme.NotoSansFontFamily
import com.tatl.fastnote.ui.common.AppToast
import com.tatl.fastnote.R
import com.tatl.fastnote.util.FileHelper
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


// ── Màu từ bảng màu chung ─────────────────────────────────────────────────────
private val FvTextPrimary  = Color(0xFFFFFFFF)
private val FvTextMuted    = Color(0xFF888888)
private val FvXongColor    = Color(0xFFCCCCCC)
private val FvRedHighlight = Color(0xFFFF5252)
private val FvRedBg        = Color(0x33FF5252)

// Rule: dòng ngày tháng cố định
private val IS_DATE_LINE: (String) -> Boolean = { line ->
    val t = line.trimStart()
    t.startsWith("- Thứ") || t.startsWith("- Chủ")
}

/**
 * FileViewerScreen — thiết kế mới khớp ảnh.
 *
 * Giao diện:
 *  - Nền đen tuyệt đối, không TopAppBar
 *  - "XONG" góc trên phải → lưu + thoát
 *  - BasicTextField toàn màn hình, nội dung file thô
 *  - Snackbar nhỏ hiện ở trên khi vi phạm rule ("Lá chắn kích hoạt...")
 *  - imePadding đẩy content lên khi bàn phím mở
 */
@Composable
fun FileViewerScreen(
    startInEditMode: Boolean = false,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var rawContent      by remember { mutableStateOf("") }
    var originalContent by remember { mutableStateOf("") }
    var isSaving        by remember { mutableStateOf(false) }
    var tfv by remember { mutableStateOf(TextFieldValue("")) }
    var showProtectToast by remember { mutableStateOf(false) }

    // ── Load file — dao nguoc de moi nhat len dau ────────────────────────────
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val raw = FileHelper.readRawFile(context)
            // Tach thanh cac entry theo dau gach "- ", dao nguoc, ghep lai
            val reversed = reverseEntries(raw)
            rawContent      = reversed
            originalContent = raw  // luu ban goc de diff khi save
        }
        tfv = TextFieldValue(rawContent, selection = TextRange(0))
    }

    // ── Sync tfv khi file load xong ──────────────────────────────────────────
    LaunchedEffect(rawContent) {
        if (rawContent.isNotEmpty() && tfv.text != rawContent) {
            tfv = TextFieldValue(rawContent, selection = TextRange(0))
        }
    }

    // ── Save ──────────────────────────────────────────────────────────────────
    fun doSave() {
        if (isSaving) return
        isSaving = true
        scope.launch {
            // tfv.text dang o thu tu dao nguoc (moi->cu), can dao lai ve goc (cu->moi) truoc khi luu
            val textToSave = reverseEntries(tfv.text)
            val error = withContext(Dispatchers.IO) {
                FileHelper.saveEditedRaw(context, originalContent, textToSave)
            }
            isSaving = false
            if (error == null) {
                onClose()
            } else {
                showProtectToast = true
            }
        }
    }

    // ── Root ──────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBgBlack)
            .imePadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Khoảng trống status bar (XONG thực sự ở Box overlay bên dưới)
            Spacer(
                Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .height(48.dp) // chiều cao top bar
            )


            // ── Editor: BasicTextField voi scrollbar ─────────────────────────
            val scrollState = rememberScrollState()
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .drawBehind {
                        val barWidth = 3.dp.toPx()
                        val trackH   = size.height
                        val maxScroll = scrollState.maxValue.toFloat()
                        val fraction  = if (maxScroll > 0f) scrollState.value / maxScroll else 0f
                        val thumbFrac = trackH / (trackH + maxScroll).coerceAtLeast(1f)
                        val thumbH    = (trackH * thumbFrac).coerceAtLeast(32.dp.toPx())
                        val thumbTop  = (trackH - thumbH) * fraction
                        val x = size.width - barWidth - 4.dp.toPx()
                        // Track
                        drawRoundRect(
                            color = Color(0x18FFFFFF),
                            topLeft = Offset(x, 0f),
                            size = Size(barWidth, trackH),
                            cornerRadius = CornerRadius(barWidth / 2)
                        )
                        // Thumb
                        drawRoundRect(
                            color = Color(0x66FFFFFF),
                            topLeft = Offset(x, thumbTop),
                            size = Size(barWidth, thumbH),
                            cornerRadius = CornerRadius(barWidth / 2)
                        )
                    }
            ) {
                BasicTextField(
                    value = tfv,
                    onValueChange = { newTfv ->
                        val oldText = tfv.text
                        val newText = newTfv.text

                        if (newText == oldText) {
                            tfv = newTfv
                            return@BasicTextField
                        }

                        val oldLines = oldText.lines()
                        val newLines = newText.lines()

                        // Rule: bao ve dong thoi gian, khong cho xoa/sua PHAN HEADER
                        // Chi so sanh phan truoc ": " (thoi gian), KHONG so sanh phan content sau ": "
                        fun headerOnly(line: String): String {
                            val idx = line.indexOf(": ")
                            return if (idx != -1) line.substring(0, idx) else line
                        }
                        val oldHeaders = oldLines.filter(IS_DATE_LINE).map { headerOnly(it) }
                        val newHeaders = newLines.filter(IS_DATE_LINE).map { headerOnly(it) }
                        if (oldHeaders.size != newHeaders.size ||
                            oldHeaders.zip(newHeaders).any { (a, b) -> a != b }
                        ) {
                            return@BasicTextField
                        }

                        // Rule: gioi han xoa nhieu qua mot lan (boi den qua nhieu)
                        // Moi entry la 1 dong dai, nen dem ky tu thay vi dong
                        // ~200 ky tu ~ khoang 2-3 dong hien thi tren man hinh
                        val charsRemoved = oldText.length - newText.length
                        if (charsRemoved >= 100) {
                            return@BasicTextField
                        }

                        tfv = newTfv
                        rawContent = newText
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .verticalScroll(scrollState),
                    textStyle = TextStyle(
                        fontFamily = NotoSansFontFamily,
                        color = FvTextPrimary,
                        fontSize = 15.sp,
                        lineHeight = 26.sp,
                        letterSpacing = 0.1.sp
                    ),
                    cursorBrush = SolidColor(FvTextPrimary),
                    decorationBox = { innerTextField ->
                        if (tfv.text.isEmpty()) {
                            Text(
                                text = "Chua co ghi chu nao...",
                                color = FvTextMuted,
                                fontFamily = NotoSansFontFamily,
                                fontSize = 15.sp
                            )
                        }
                        innerTextField()
                    }
                )
            }

            // Nav bar padding ở dưới cùng
            Spacer(
                Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .height(8.dp)
            )
        }

        // ── "XONG" clickable — overlay góc trên phải ──────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 8.dp, end = 20.dp)
                .height(44.dp)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.btn_done),
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                letterSpacing = 1.5.sp,
                color = if (isSaving) FvTextMuted else FvXongColor,
                modifier = Modifier.clickable(
                    enabled = !isSaving,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = { doSave() }
                )
            )
        }

        // -- AppToast: canh bao khi co gang xoa dong thoi gian --
        AppToast(
            visible = showProtectToast,
            message = "Không thể xóa dòng thời gian ghi chú",
            durationMs = 2000L,
            onDismiss = { showProtectToast = false }
        )
    }
}

// ── Search highlight (giữ lại để dùng nếu cần) ─────────────────────────────────
private fun highlight(text: String, query: String): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(text)
    return buildAnnotatedString {
        val lower = text.lowercase()
        val lq    = query.lowercase()
        var last  = 0
        var idx   = lower.indexOf(lq, last)
        while (idx != -1) {
            append(text.substring(last, idx))
            pushStyle(SpanStyle(color = FvRedHighlight, background = FvRedBg, fontWeight = FontWeight.Bold))
            append(text.substring(idx, idx + query.length))
            pop()
            last = idx + query.length
            idx  = lower.indexOf(lq, last)
        }
        append(text.substring(last))
    }
}

/**
 * Dao nguoc thu tu cac entry trong file ghi chu.
 * Format: moi entry la 1 dong "- Thu/Chu..." dau bang dong trong.
 * Ket qua: moi nhat len dau.
 */
private fun reverseEntries(raw: String): String {
    if (raw.isBlank()) return raw

    // Tach thanh cac block ngan cach boi dong trong
    // Moi block thuong la: "\n\n- Thu ba, ngay...: text"
    // Split bang "\n\n" de giu nguyen structure
    val blocks = raw.split("\n\n")
        .map { it.trim() }
        .filter { it.isNotBlank() }

    // Dao nguoc: moi nhat len dau
    return blocks.reversed().joinToString("\n\n")
}
