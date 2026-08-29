package com.tatl.fastnote.ui.fileviewer

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tatl.fastnote.ui.common.AppToast
import com.tatl.fastnote.ui.recording.RecordingActivity
import com.tatl.fastnote.ui.theme.AndroidAutoNoteTheme
import com.tatl.fastnote.ui.theme.InterFontFamily
import com.tatl.fastnote.ui.theme.NotoSansFontFamily
import com.tatl.fastnote.util.FileHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ── Bảng màu giao diện chuẩn theo thiết kế ───────────────────────────────────
private val FvBgTop         = Color(0xFF1A2B39)
private val FvBgMid         = Color(0xFF12202C)
private val FvBgBottom      = Color(0xFF0C161F)
private val FvCardBg        = Color(0xFF070F17).copy(alpha = 0.85f)
private val FvCardBorder    = Color(0xFF1B2A38).copy(alpha = 0.6f)
private val FvBottomBarBg   = Color(0xFF0D1721).copy(alpha = 0.96f)
private val FvBottomBarBorder = Color(0xFF1B2B3A)
private val FvTextPrimary   = Color(0xFFF1F5F9)
private val FvTextMuted     = Color(0xFF888888)
private val FvHeaderItalic  = Color(0xFF8EA4B8)


@Composable
fun FileViewerScreen(
    startInEditMode: Boolean = false,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    var rawContent      by remember { mutableStateOf("") }
    var originalContent by remember { mutableStateOf("") }
    var isSaving        by remember { mutableStateOf(false) }
    var tfv by remember { mutableStateOf(TextFieldValue("")) }
    var showProtectToast by remember { mutableStateOf(false) }
    var autoSaveJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    // ── Load file — đảo ngược để mới nhất lên đầu ────────────────────────────
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val raw = FileHelper.readRawFile(context)
            originalContent = raw
            rawContent      = raw
            val reversed    = reverseEntries(raw)
            tfv = TextFieldValue(reversed, selection = TextRange(0))
        }
    }

    // ── Save ──────────────────────────────────────────────────────────────────
    fun doSave() {
        if (isSaving) return
        isSaving = true
        autoSaveJob?.cancel()
        scope.launch {
            val textToSave = reverseEntries(tfv.text)
            val error = withContext(Dispatchers.IO) {
                FileHelper.saveEditedRaw(context, originalContent, textToSave)
            }
            isSaving = false
            if (error == null) {
                keyboardController?.hide()
                onClose()
            } else {
                showProtectToast = true
            }
        }
    }

    // ── Focus & bàn phím khi mở — đặt trỏ lên dòng đầu tiên ───────────────────
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(80L)
        tfv = tfv.copy(selection = TextRange(0))
        try {
            focusRequester.requestFocus()
            keyboardController?.show()
        } catch (_: Exception) {}
    }

    // ── Giao diện ─────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(FvBgTop, FvBgMid, FvBgBottom)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            // Khoảng trống status bar phía trên
            Spacer(
                Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .height(12.dp)
            )

            // ── Khung nền lớn chứa toàn bộ vùng soạn thảo ─────────────────
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                color = FvCardBg,
                border = BorderStroke(0.8.dp, FvCardBorder)
            ) {
                val scrollState = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val barWidth = 3.dp.toPx()
                            val trackH   = size.height
                            val maxScroll = scrollState.maxValue.toFloat()
                            val fraction  = if (maxScroll > 0f) scrollState.value / maxScroll else 0f
                            val thumbFrac = trackH / (trackH + maxScroll).coerceAtLeast(1f)
                            val thumbH    = (trackH * thumbFrac).coerceAtLeast(32.dp.toPx())
                            val thumbTop  = (trackH - thumbH) * fraction
                            val x = size.width - barWidth - 4.dp.toPx()
                            drawRoundRect(
                                color = Color(0x18FFFFFF),
                                topLeft = Offset(x, 0f),
                                size = Size(barWidth, trackH),
                                cornerRadius = CornerRadius(barWidth / 2)
                            )
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

                            // Rule: bảo vệ header ngày giờ cố định bằng Regex (cho phép gõ thêm header mới)
                            val oldHeaders = oldLines.mapNotNull { FileHelper.extractDateHeader(it) }
                            val newHeaders = newLines.mapNotNull { FileHelper.extractDateHeader(it) }
                            if (!FileHelper.isValidHeaderPreservation(oldHeaders, newHeaders)) {
                                showProtectToast = true
                                return@BasicTextField
                            }

                            val charsRemoved = oldText.length - newText.length
                            if (charsRemoved >= 100) {
                                return@BasicTextField
                            }

                            tfv = newTfv
                            rawContent = newText

                            // ── Tự động lưu theo thời gian thực (Cứ sửa tới đâu lưu tới đó) ──
                            autoSaveJob?.cancel()
                            autoSaveJob = scope.launch(Dispatchers.IO) {
                                kotlinx.coroutines.delay(200L)
                                val textToSave = reverseEntries(newText)
                                FileHelper.saveEditedRaw(context, originalContent, textToSave)
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .focusRequester(focusRequester)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .verticalScroll(scrollState),
                        visualTransformation = remember { NoteEditorVisualTransformation() },
                        textStyle = TextStyle(
                            fontFamily = NotoSansFontFamily,
                            color = FvTextPrimary,
                            fontSize = 18.sp,
                            lineHeight = 22.sp,
                            letterSpacing = 0.1.sp
                        ),
                        cursorBrush = SolidColor(Color.White),
                        decorationBox = { innerTextField ->
                            if (tfv.text.isEmpty()) {
                                Text(
                                    text = "Chưa có ghi chú nào...",
                                    color = FvTextMuted,
                                    fontFamily = NotoSansFontFamily,
                                    fontSize = 18.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            }

            // ── Thanh công cụ dưới: [ ⌨️ Bàn phím ] và [ 💾 LƯU ] ──
            @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
            val isImeVisible = WindowInsets.isImeVisible

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = FvBottomBarBg,
                border = BorderStroke(width = 0.5.dp, color = FvBottomBarBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Nút nhỏ góc trái: Bật/tắt mở bàn phím
                    Surface(
                        onClick = {
                            if (isImeVisible) {
                                keyboardController?.hide()
                            } else {
                                focusRequester.requestFocus()
                                keyboardController?.show()
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isImeVisible) Color(0xFF1E3A8A).copy(alpha = 0.5f) else Color(0xFF223547).copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, if (isImeVisible) Color(0xFF3B82F6) else Color(0xFF354C62)),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isImeVisible) Icons.Default.KeyboardHide else Icons.Default.Keyboard,
                                contentDescription = if (isImeVisible) "Ẩn bàn phím" else "Hiện bàn phím",
                                tint = if (isImeVisible) Color(0xFF93C5FD) else Color(0xFFCBD5E1),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Nút LƯU bên phải
                    Surface(
                        onClick = { doSave() },
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E3A8A).copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, Color(0xFF3B82F6)),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 28.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Lưu",
                                tint = Color(0xFF60A5FA),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (isSaving) "Đang lưu..." else "LƯU",
                                color = Color(0xFF93C5FD),
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }

        // -- AppToast: cảnh báo khi có gắng xóa dòng thời gian --
        AppToast(
            visible = showProtectToast,
            message = "Không thể xóa dòng thời gian ghi chú",
            durationMs = 2000L,
            onDismiss = { showProtectToast = false }
        )
    }
}

// ── Đảo ngược thứ tự các entry trong file ghi chú ────────────────────────────

/**
 * Dao nguoc thu tu cac entry trong file ghi chu.
 * Format: moi entry la 1 dong "- Thu/Chu..." dau bang dong trong.
 * Ket qua: moi nhat len dau.
 */
private fun reverseEntries(raw: String): String {
    if (raw.isBlank()) return raw

    val blocks = raw.split("\n\n")
        .map { it.trim() }
        .filter { it.isNotBlank() }

    return blocks.reversed().joinToString("\n\n")
}

// ── Visual Transformation định dạng dòng ghi chú trong khung soạn thảo giống hệt Home ──
private class NoteEditorVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val rawStr = text.text
        if (rawStr.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }
        val annotated = buildAnnotatedString {
            val lines = rawStr.lines()
            for (i in lines.indices) {
                val line = lines[i]
                val trimmed = line.trimStart()
                val match = FileHelper.DATE_HEADER_REGEX.find(trimmed)
                val start = length
                append(line)
                val end = length

                if (match != null) {
                    val colonIdx = line.indexOf(':')
                    val headerEnd = if (colonIdx != -1) start + colonIdx + 1 else end
                    addStyle(
                        SpanStyle(
                            color = FvHeaderItalic,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Normal
                        ),
                        start,
                        headerEnd
                    )
                    if (headerEnd < end) {
                        addStyle(
                            SpanStyle(
                                color = Color(0xFFF1F5F9),
                                fontWeight = FontWeight.Normal
                            ),
                            headerEnd,
                            end
                        )
                    }
                } else {
                    addStyle(
                        SpanStyle(
                            color = Color(0xFFF1F5F9),
                            fontWeight = FontWeight.Normal
                        ),
                        start,
                        end
                    )
                }

                if (i < lines.size - 1) {
                    append("\n")
                }
            }
        }
        return TransformedText(annotated, OffsetMapping.Identity)
    }
}

// ── PREVIEW CHO ANDROID STUDIO COMPOSE ──────────────────────────────────────

@Preview(name = "FileViewer - Chế độ Soạn thảo (Editor)", showBackground = true, backgroundColor = 0xFF000000, widthDp = 390, heightDp = 844)
@Composable
fun FileViewerScreenPreview() {
    AndroidAutoNoteTheme {
        var tfv by remember {
            mutableStateOf(
                TextFieldValue(
                    "- Thứ năm, ngày 20-08-2026 lúc 19.45: Tôi đang thử nghiệm video này cho ai xem\n\n" +
                    "- Thứ năm, ngày 20-08-2026 lúc 07.12: Sáng nay vợ chở hai con đi bệnh viện Yersin để khám chân cho Tuấn Anh và đại tràng cho vợ.\n\n" +
                    "- Thứ năm, ngày 20-08-2026 lúc 06.22: Vừa có ý tưởng hay: Ý TƯỞNG PHÁT TRIỂN: TRẠM KÝ ÂM ĐA PHƯƠNG TIỆN\n\n" +
                    "Cốt lõi ý tưởng:\n" +
                    "Biến Z-FN thành trạm xử lý thông tin siêu tốc từ mọi định dạng."
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(FvBgTop, FvBgMid, FvBgBottom)
                    )
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(Modifier.height(12.dp))

                // Khung nền lớn chứa toàn bộ vùng soạn thảo
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = FvCardBg,
                    border = BorderStroke(0.8.dp, FvCardBorder)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        BasicTextField(
                            value = tfv,
                            onValueChange = { tfv = it },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            visualTransformation = remember { NoteEditorVisualTransformation() },
                            textStyle = TextStyle(
                                fontFamily = NotoSansFontFamily,
                                color = FvTextPrimary,
                                fontSize = 18.sp,
                                lineHeight = 22.sp,
                                letterSpacing = 0.1.sp
                            ),
                            cursorBrush = SolidColor(Color.White)
                        )
                    }
                }

                // Bottom bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = FvBottomBarBg,
                    border = BorderStroke(width = 0.5.dp, color = FvBottomBarBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = {},
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF223547).copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, Color(0xFF354C62)),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Keyboard,
                                    contentDescription = "Bàn phím",
                                    tint = Color(0xFFCBD5E1),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Surface(
                            onClick = {},
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E3A8A).copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, Color(0xFF3B82F6)),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 28.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = "Lưu",
                                    tint = Color(0xFF60A5FA),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "LƯU",
                                    color = Color(0xFF93C5FD),
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
