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
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.tatl.fastnote.util.FileHelper
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
    val snackbar = remember { SnackbarHostState() }

    var rawContent      by remember { mutableStateOf("") }
    var originalContent by remember { mutableStateOf("") }
    var isSaving        by remember { mutableStateOf(false) }
    var tfv by remember { mutableStateOf(TextFieldValue("")) }

    // ── Load file ─────────────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            rawContent      = FileHelper.readRawFile(context)
            originalContent = rawContent
        }
        tfv = TextFieldValue(rawContent, selection = TextRange(rawContent.length))
    }

    // ── Sync tfv khi file load xong ──────────────────────────────────────────
    LaunchedEffect(rawContent) {
        if (rawContent.isNotEmpty() && tfv.text != rawContent) {
            tfv = TextFieldValue(rawContent, selection = TextRange(rawContent.length))
        }
    }

    // ── Save ──────────────────────────────────────────────────────────────────
    fun doSave() {
        if (isSaving) return
        isSaving = true
        scope.launch {
            val error = withContext(Dispatchers.IO) {
                FileHelper.saveEditedRaw(context, originalContent, tfv.text)
            }
            isSaving = false
            if (error == null) {
                onClose()
            } else {
                snackbar.showSnackbar(error, duration = SnackbarDuration.Short)
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


            // ── Editor: BasicTextField toàn màn hình ─────────────────────────
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

                    // Rule 1: bảo vệ dòng ngày tháng
                    val oldHeaders = oldLines.filter(IS_DATE_LINE)
                    val newHeaders = newLines.filter(IS_DATE_LINE)
                    if (oldHeaders.size != newHeaders.size ||
                        oldHeaders.zip(newHeaders).any { (a, b) -> a != b }
                    ) {
                        scope.launch {
                            snackbar.showSnackbar(
                                "Lá chắn kích hoạt: Dòng ngày cố định, không thể xóa",
                                duration = SnackbarDuration.Short
                            )
                        }
                        return@BasicTextField
                    }

                    // Rule 2: không xóa > 5 dòng cùng lúc
                    val linesRemoved = oldLines.size - newLines.size
                    if (linesRemoved > 5) {
                        scope.launch {
                            snackbar.showSnackbar(
                                "Lá chắn kích hoạt: Chọn quá 5 dòng không thể xóa",
                                duration = SnackbarDuration.Short
                            )
                        }
                        return@BasicTextField
                    }

                    tfv = newTfv
                    rawContent = newText
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
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
                            text = "Chưa có ghi chú nào...",
                            color = FvTextMuted,
                            fontFamily = NotoSansFontFamily,
                            fontSize = 15.sp
                        )
                    }
                    innerTextField()
                }
            )

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
                text = "XONG",
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

        // ── Snackbar "Lá chắn kích hoạt..." ─────────────────────────────────
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 60.dp)
        ) { data ->
            Snackbar(
                modifier = Modifier.padding(horizontal = 20.dp),
                containerColor = Color(0xFF1E1E1E),
                contentColor = FvTextMuted,
                content = {
                    Text(
                        text = data.visuals.message,
                        fontFamily = NotoSansFontFamily,
                        fontSize = 12.sp,
                        color = FvTextMuted
                    )
                }
            )
        }
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
