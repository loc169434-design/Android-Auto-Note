package com.tatl.fastnote.ui.home

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tatl.fastnote.R
import com.tatl.fastnote.ui.fileviewer.FileViewerActivity
import com.tatl.fastnote.ui.theme.AppBgBlack
import com.tatl.fastnote.ui.theme.InterFontFamily
import com.tatl.fastnote.ui.theme.NotoSansFontFamily
import com.tatl.fastnote.util.FileHelper
import com.tatl.fastnote.util.PinWidgetHelper
import com.tatl.fastnote.util.ThemePreferences
import com.tatl.fastnote.widget.TripleActionWidgetReceiver
import com.tatl.fastnote.widget.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Màu dùng từ bảng màu chung ────────────────────────────────────────────────
private val HomeTextPrimary  = Color(0xFFFFFFFF)
private val HomeTextMuted    = Color(0xFF888888)
private val HomeIconColor    = Color(0xFFCCCCCC)
private val HomeSearchActive = Color(0xFF4CAF50)
private val RedHighlight     = Color(0xFFFF5252)
private val RedBg            = Color(0x33FF5252)

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onRecordClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAIShareClick: () -> Unit = {},
    onPremiumClick: () -> Unit = {},
    onComputerClick: () -> Unit = {},
    isPremium: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    // ── State ─────────────────────────────────────────────────────────────────
    val hasPinnedWidget by ThemePreferences.hasPinnedWidget.collectAsState()
    val currentHasPinned by rememberUpdatedState(hasPinnedWidget)
    var showManualPinPrompt by remember { mutableStateOf(false) }
    var showCreateManualDialog by remember { mutableStateOf(false) }
    var widgetActiveNow by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var fileEntries by remember { mutableStateOf<List<FileHelper.NoteEntry>>(emptyList()) }
    var searchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshKey++
                widgetActiveNow = if (currentHasPinned) {
                    PinWidgetHelper.isWidgetActive(context, TripleActionWidgetReceiver::class.java)
                } else false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(hasPinnedWidget) {
        widgetActiveNow = if (hasPinnedWidget) {
            PinWidgetHelper.isWidgetActive(context, TripleActionWidgetReceiver::class.java)
        } else false
    }

    LaunchedEffect(refreshKey) {
        withContext(Dispatchers.IO) {
            fileEntries = FileHelper.parseEntries(context)
        }
    }

    // ── Widget prompt logic ───────────────────────────────────────────────────
    val widgetWasRemoved = hasPinnedWidget && !widgetActiveNow
    val shouldShowWidgetPrompt = !hasPinnedWidget || widgetWasRemoved || showManualPinPrompt
    val isPromptMandatory = !hasPinnedWidget || widgetWasRemoved

    // ── Filter + mask ─────────────────────────────────────────────────────────
    val filteredEntries = if (searchQuery.isBlank()) fileEntries
    else fileEntries.filter {
        it.content.contains(searchQuery, ignoreCase = true) ||
        it.header.contains(searchQuery, ignoreCase = true)
    }
    val maskedEntries = filteredEntries  // masking applied per-entry below

    // ─────────────────────────────────────────────────────────────────────────
    //  ROOT: toàn màn hình đen, không Scaffold
    // ─────────────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBgBlack)
    ) {

        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top bar: icon máy tính + vương miện ──────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Trái: icon máy tính → gửi PC (gate premium)
                IconButton(
                    onClick = onComputerClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.Computer,
                        contentDescription = "Widget",
                        tint = HomeIconColor,
                        modifier = Modifier.size(44.dp)
                    )
                }

                // Phải: ic_premium → premium
                IconButton(
                    onClick = onPremiumClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_premium),
                        contentDescription = "Premium",
                        tint = HomeIconColor,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            // ── Search bar (chỉ hiện khi active) ─────────────────────────────
            if (searchActive) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    placeholder = {
                        Text(
                            "Tìm kiếm...",
                            color = HomeTextMuted,
                            fontFamily = NotoSansFontFamily,
                            fontSize = 14.sp
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = HomeTextPrimary,
                        unfocusedTextColor = HomeTextPrimary,
                        focusedBorderColor = HomeSearchActive,
                        unfocusedBorderColor = Color(0xFF333333),
                        cursorColor = HomeTextPrimary
                    ),
                    textStyle = TextStyle(
                        fontFamily = NotoSansFontFamily,
                        fontSize = 14.sp,
                        color = HomeTextPrimary
                    ),
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, null, tint = HomeTextMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(Modifier.height(8.dp))
            }

            // ── Danh sách ghi chú ─────────────────────────────────────────────
            if (fileEntries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Chưa có ghi chú nào.\nHãy nói từ widget 🎤",
                        color = HomeTextMuted,
                        fontFamily = NotoSansFontFamily,
                        fontSize = 15.sp,
                        lineHeight = 24.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (filteredEntries.isEmpty() && searchQuery.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Không tìm thấy\n\"$searchQuery\"",
                        color = HomeTextMuted,
                        fontFamily = NotoSansFontFamily,
                        fontSize = 15.sp,
                        lineHeight = 24.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    item { Spacer(Modifier.height(8.dp)) }
                    items(maskedEntries) { entry ->
                        val rawLine = "- ${entry.header}: ${entry.content}"
                        val maskedLine = FileHelper.maskSensitive(listOf(rawLine)).firstOrNull() ?: rawLine
                        // Hiển thị dạng [timestamp] content — đúng như ảnh
                        NoteEntryItem(
                            text = "• " + maskedLine.removePrefix("- "),
                            searchQuery = searchQuery
                        )
                        Spacer(Modifier.height(28.dp))
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }

        // ── Bottom bar: bút + kính lúp ────────────────────────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Trái: bút chì → mở editor / tạo ghi chú thủ công
            IconButton(
                onClick = { showCreateManualDialog = true },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Tạo ghi chú",
                    tint = HomeIconColor,
                    modifier = Modifier.size(44.dp)
                )
            }

            // Giữa: mic → ghi âm
            IconButton(
                onClick = onRecordClick,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = "Ghi âm",
                    tint = HomeTextMuted,
                    modifier = Modifier.size(44.dp)
                )
            }

            // Phải: kính lúp → tìm kiếm
            IconButton(
                onClick = {
                    searchActive = !searchActive
                    if (!searchActive) searchQuery = ""
                },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Tìm kiếm",
                    tint = if (searchActive) HomeSearchActive else HomeIconColor,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        // ── Widget prompt overlay ─────────────────────────────────────────────
        if (shouldShowWidgetPrompt) {
            PinWidgetBottomSheet(
                isMandatory = isPromptMandatory,
                onDismiss = {
                    if (widgetWasRemoved) ThemePreferences.setWidgetPinned(true)
                    showManualPinPrompt = false
                }
            )
        }
    }

    // ── Dialog tạo ghi chú thủ công ──────────────────────────────────────────
    if (showCreateManualDialog) {
        CreateManualNoteDialog(
            onSave = { title, content ->
                if (viewModel.createNote(title, content)) {
                    coroutineScope.launch {
                        WidgetUpdater.updateAllWidgets(context)
                    }
                    showCreateManualDialog = false
                }
            },
            onDismiss = { showCreateManualDialog = false }
        )
    }
}

// ── Note entry item ───────────────────────────────────────────────────────────

@Composable
private fun NoteEntryItem(text: String, searchQuery: String) {
    Text(
        text = highlightText(text, searchQuery),
        style = TextStyle(
            fontFamily = NotoSansFontFamily,
            color = HomeTextPrimary,
            fontSize = 15.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.1.sp
        )
    )
}

// ── Create manual note dialog ─────────────────────────────────────────────────

@Composable
private fun CreateManualNoteDialog(
    onSave: (title: String, content: String) -> Unit,
    onDismiss: () -> Unit
) {
    val defaultPrefix = stringResource(R.string.default_note_title_prefix)
    val defaultTimestamp = remember {
        val now = Date()
        val formattedDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(now)
        "$defaultPrefix $formattedDate"
    }

    var titleText by remember { mutableStateOf(defaultTimestamp) }
    var contentText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        title = {
            Text(
                stringResource(R.string.dialog_new_note_title),
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                color = HomeTextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = {
                        Text("Tiêu đề", fontFamily = NotoSansFontFamily,
                            fontSize = 13.sp, color = HomeTextMuted)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = HomeTextPrimary,
                        unfocusedTextColor = HomeTextPrimary,
                        focusedBorderColor = HomeSearchActive,
                        unfocusedBorderColor = Color(0xFF333333),
                        cursorColor = HomeTextPrimary
                    ),
                    textStyle = TextStyle(fontFamily = NotoSansFontFamily, color = HomeTextPrimary)
                )
                OutlinedTextField(
                    value = contentText,
                    onValueChange = { contentText = it },
                    label = {
                        Text(stringResource(R.string.placeholder_note_content),
                            fontFamily = NotoSansFontFamily, fontSize = 13.sp, color = HomeTextMuted)
                    },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = HomeTextPrimary,
                        unfocusedTextColor = HomeTextPrimary,
                        focusedBorderColor = HomeSearchActive,
                        unfocusedBorderColor = Color(0xFF333333),
                        cursorColor = HomeTextPrimary
                    ),
                    textStyle = TextStyle(fontFamily = NotoSansFontFamily, color = HomeTextPrimary)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(titleText, contentText) },
                enabled = contentText.isNotBlank() || titleText.isNotBlank()
            ) {
                Text(
                    stringResource(R.string.btn_save),
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    color = HomeSearchActive
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.btn_cancel),
                    fontFamily = InterFontFamily,
                    color = HomeTextMuted
                )
            }
        }
    )
}

// ── Search highlight ──────────────────────────────────────────────────────────

private fun highlightText(text: String, query: String): AnnotatedString {
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
