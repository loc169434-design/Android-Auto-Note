package com.example.androidautonote.ui.fileviewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidautonote.util.FileHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ── Color palette ─────────────────────────────────────────────────────────────
private val BgDark       = Color(0xFF0A1A0F)
private val BgSurface    = Color(0xFF142B1A)
private val TextPrimary  = Color(0xFFECF5EE)
private val TextMuted    = Color(0xFF8CB89A)
private val AccentGreen  = Color(0xFF4CAF50)
private val RedHighlight = Color(0xFFFF5252)
private val RedBg        = Color(0x33FF5252)

// ── Mode enum ─────────────────────────────────────────────────────────────────
private enum class ViewerMode { VIEW, SEARCH, EDIT }

/**
 * Full note viewer with three modes:
 *   VIEW   — scrollable list of entries from fileguidi.txt, newest first
 *   SEARCH — same list but with red-highlighted matches (case-insensitive)
 *   EDIT   — raw TextField editing raw.txt with save constraints
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewerScreen(
    startInEditMode: Boolean = false,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var mode    by remember { mutableStateOf(if (startInEditMode) ViewerMode.EDIT else ViewerMode.VIEW) }
    var entries by remember { mutableStateOf<List<FileHelper.NoteEntry>>(emptyList()) }
    var searchQuery     by remember { mutableStateOf("") }
    var rawContent      by remember { mutableStateOf("") }
    var originalContent by remember { mutableStateOf("") }
    var isSaving        by remember { mutableStateOf(false) }

    // ── Load data ──────────────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            entries = FileHelper.parseEntries(context)
            rawContent = FileHelper.readRawFile(context)
            originalContent = rawContent
        }
    }

    // ── Save helper ────────────────────────────────────────────────────────────
    fun doSave() {
        if (isSaving) return
        isSaving = true
        scope.launch {
            val error = withContext(Dispatchers.IO) {
                FileHelper.saveEditedRaw(context, originalContent, rawContent)
            }
            if (error == null) {
                // Reload entries after save
                entries = withContext(Dispatchers.IO) { FileHelper.parseEntries(context) }
                originalContent = rawContent
                mode = ViewerMode.VIEW
                snackbar.showSnackbar("✅ Đã lưu và sao lưu fileguidi.txt")
            } else {
                snackbar.showSnackbar("⚠️ $error")
            }
            isSaving = false
        }
    }

    Scaffold(
        containerColor = BgDark,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (mode == ViewerMode.SEARCH) "Tìm kiếm" else "SỔ GHI CHÚ",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgSurface),
                navigationIcon = {
                    IconButton(onClick = {
                        when (mode) {
                            ViewerMode.SEARCH -> { mode = ViewerMode.VIEW; searchQuery = "" }
                            ViewerMode.EDIT   -> { mode = ViewerMode.VIEW; rawContent = originalContent }
                            ViewerMode.VIEW   -> onClose()
                        }
                    }) {
                        Icon(Icons.Default.Close, "Đóng", tint = TextMuted)
                    }
                },
                actions = {
                    when (mode) {
                        ViewerMode.VIEW -> {
                            IconButton(onClick = { mode = ViewerMode.SEARCH }) {
                                Icon(Icons.Default.Search, "Tìm kiếm", tint = AccentGreen)
                            }
                            IconButton(onClick = { mode = ViewerMode.EDIT }) {
                                Icon(Icons.Default.Edit, "Sửa", tint = AccentGreen)
                            }
                        }
                        ViewerMode.SEARCH -> {
                            IconButton(onClick = { mode = ViewerMode.VIEW; searchQuery = "" }) {
                                Icon(Icons.Default.Close, "Đóng tìm kiếm", tint = TextMuted)
                            }
                        }
                        ViewerMode.EDIT -> {
                            TextButton(onClick = { doSave() }, enabled = !isSaving) {
                                Text("Lưu", color = AccentGreen, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { mode = ViewerMode.VIEW; rawContent = originalContent }) {
                                Icon(Icons.Default.Close, "Hủy sửa", tint = TextMuted)
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BgDark)
        ) {
            // ── Search bar ───────────────────────────────────────────────────────
            if (mode == ViewerMode.SEARCH) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = {
                        Text("Nhập từ cần tìm...", color = TextMuted)
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentGreen,
                        unfocusedBorderColor = TextMuted,
                        cursorColor = AccentGreen
                    ),
                    textStyle = TextStyle(color = TextPrimary)
                )
            }

            // ── Content ──────────────────────────────────────────────────────────
            when (mode) {
                ViewerMode.VIEW, ViewerMode.SEARCH -> {
                    NoteListView(
                        entries = entries,
                        searchQuery = if (mode == ViewerMode.SEARCH) searchQuery else ""
                    )
                }
                ViewerMode.EDIT -> {
                    EditView(
                        content = rawContent,
                        onContentChange = { rawContent = it },
                        onShowError = { msg ->
                            scope.launch { snackbar.showSnackbar(msg) }
                        }
                    )
                }
            }
        }
    }
}

// ── Note list view ────────────────────────────────────────────────────────────

@Composable
private fun NoteListView(
    entries: List<FileHelper.NoteEntry>,
    searchQuery: String
) {
    if (entries.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Chưa có ghi chú nào.\nHãy ghi âm từ widget!",
                color = TextMuted,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        return
    }

    // Filter for search
    val filtered = if (searchQuery.isBlank()) entries
    else entries.filter {
        it.content.contains(searchQuery, ignoreCase = true) ||
        it.header.contains(searchQuery, ignoreCase = true)
    }

    // Apply password masking line-by-line on the full text lines
    val displayLines = buildList {
        filtered.forEach { entry ->
            add("- ${entry.header}: ${entry.content}")
            add("") // blank separator
        }
    }
    val maskedLines = FileHelper.maskSensitive(displayLines)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(12.dp)) }
        items(maskedLines) { line ->
            if (line.isBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
            } else {
                Text(
                    text = highlight(line, searchQuery),
                    style = TextStyle(
                        color = TextPrimary,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                )
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

// ── Edit view ─────────────────────────────────────────────────────────────────

private val IS_DATE_LINE: (String) -> Boolean = { line ->
    val t = line.trimStart()
    t.startsWith("- Thứ") || t.startsWith("- Chủ")
}

@Composable
private fun EditView(
    content: String,
    onContentChange: (String) -> Unit,
    onShowError: (String) -> Unit
) {
    // TextFieldValue tracks both text AND cursor/selection
    var tfv by remember { mutableStateOf(TextFieldValue(content, selection = TextRange(content.length))) }

    // Sync when content is loaded from file the first time
    LaunchedEffect(content) {
        if (content.isNotEmpty() && tfv.text != content) {
            tfv = TextFieldValue(content, selection = TextRange(content.length))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .imePadding()
    ) {
        // Legend
        Text(
            text = "⚠️ Dòng ngày tháng cố định — không thể sửa/xóa.  Tối đa xóa 2 dòng mỗi lần.",
            color = TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        BasicTextField(
            value = tfv,
            onValueChange = { newTfv ->
                val oldText = tfv.text
                val newText = newTfv.text

                if (newText == oldText) {
                    // Only selection/cursor moved — always allow
                    tfv = newTfv
                    return@BasicTextField
                }

                val oldLines = oldText.lines()
                val newLines = newText.lines()

                // ─ Rule 1: date header lines must stay intact ─────────
                val oldHeaders = oldLines.filter(IS_DATE_LINE)
                val newHeaders = newLines.filter(IS_DATE_LINE)
                val dateViolation = oldHeaders.size != newHeaders.size ||
                    oldHeaders.zip(newHeaders).any { (a, b) -> a != b }

                if (dateViolation) {
                    onShowError("⛔ Không được xóa hoặc sửa dòng ngày tháng cố định")
                    // Reject: tfv stays unchanged — TextField reverts automatically
                    return@BasicTextField
                }

                // ─ Rule 2: no bulk deletion of 3+ lines ──────────
                val linesRemoved = oldLines.size - newLines.size
                if (linesRemoved >= 3) {
                    onShowError("⛔ Không thể xóa $linesRemoved dòng cùng lúc (tối đa 2 dòng)")
                    // Reject
                    return@BasicTextField
                }

                // ─ Valid change ─────────────────────────────
                tfv = newTfv
                onContentChange(newText)
            },
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            textStyle = TextStyle(
                color = TextPrimary,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                fontFamily = FontFamily.Monospace
            ),
            cursorBrush = SolidColor(AccentGreen)
        )
    }
}

// ── Search highlight ──────────────────────────────────────────────────────────

private fun highlight(text: String, query: String): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(text)
    return buildAnnotatedString {
        val lower = text.lowercase()
        val lq    = query.lowercase()
        var last  = 0
        var idx   = lower.indexOf(lq, last)
        while (idx != -1) {
            append(text.substring(last, idx))
            pushStyle(SpanStyle(color = RedHighlight, background = RedBg, fontWeight = FontWeight.Bold))
            append(text.substring(idx, idx + query.length))
            pop()
            last = idx + query.length
            idx  = lower.indexOf(lq, last)
        }
        append(text.substring(last))
    }
}
