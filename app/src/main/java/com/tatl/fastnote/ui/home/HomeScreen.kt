package com.tatl.fastnote.ui.home

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.tatl.fastnote.ui.fileviewer.FileViewerActivity
import com.tatl.fastnote.ui.home.PinWidgetBottomSheet
import com.tatl.fastnote.util.FileHelper
import com.tatl.fastnote.util.PinWidgetHelper
import com.tatl.fastnote.util.ThemePreferences
import com.tatl.fastnote.widget.TripleActionWidgetReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.platform.LocalContext

// ── Design tokens ─────────────────────────────────────────────────────────────
private val BgDark      = Color(0xFF0A1A0F)
private val BgSurface   = Color(0xFF142B1A)
private val TextPrimary = Color(0xFFECF5EE)
private val TextMuted   = Color(0xFF7FAB8A)
private val AccentGreen = Color(0xFF4CAF50)
private val GoldColor   = Color(0xFFFFD54F)
private val RedHighlight = Color(0xFFFF5252)
private val RedBg        = Color(0x33FF5252)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onRecordClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAIShareClick: () -> Unit = {},
    onPremiumClick: () -> Unit = {},
    isPremium: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // ── Widget prompt state (preserved) ───────────────────────────────────────
    val hasPinnedWidget by ThemePreferences.hasPinnedWidget.collectAsState()
    val currentHasPinned by rememberUpdatedState(hasPinnedWidget)
    var showManualPinPrompt by remember { mutableStateOf(false) }
    var widgetActiveNow by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableIntStateOf(0) }

    // ── File entries ──────────────────────────────────────────────────────────
    var fileEntries by remember { mutableStateOf<List<FileHelper.NoteEntry>>(emptyList()) }

    // ── Search ────────────────────────────────────────────────────────────────
    var searchActive by remember { mutableStateOf(false) }
    var searchQuery  by remember { mutableStateOf("") }

    // ── Lifecycle: refresh data + widget check on every resume ────────────────
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

    // Load file entries on every resume
    LaunchedEffect(refreshKey) {
        withContext(Dispatchers.IO) {
            fileEntries = FileHelper.parseEntries(context)
        }
    }

    // ── Widget prompt logic ───────────────────────────────────────────────────
    val widgetWasRemoved     = hasPinnedWidget && !widgetActiveNow
    val shouldShowWidgetPrompt = !hasPinnedWidget || widgetWasRemoved || showManualPinPrompt
    val isPromptMandatory    = !hasPinnedWidget || widgetWasRemoved

    // ── Filtered + masked display ──────────────────────────────────────────────
    val filteredEntries = if (searchQuery.isBlank()) fileEntries
    else fileEntries.filter {
        it.content.contains(searchQuery, ignoreCase = true) ||
        it.header.contains(searchQuery, ignoreCase = true)
    }

    val displayLines = buildList {
        filteredEntries.forEach { entry ->
            add("- ${entry.header}: ${entry.content}")
            add("") // blank line separator
        }
    }
    val maskedLines = FileHelper.maskSensitive(displayLines)

    // ── UI ────────────────────────────────────────────────────────────────────
    Scaffold(
        containerColor = BgDark,
        topBar = {
            TopAppBar(
                title = {
                    if (searchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Tìm kiếm ghi chú...", color = TextMuted, fontSize = 14.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = AccentGreen,
                                unfocusedBorderColor = TextMuted.copy(alpha = 0.5f),
                                cursorColor = AccentGreen
                            ),
                            textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        )
                    } else {
                        Text(
                            text = "GHI CHÚ HÀNG NGÀY",
                            color = TextPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgSurface),
                actions = {
                    // Search toggle
                    IconButton(onClick = {
                        searchActive = !searchActive
                        if (!searchActive) searchQuery = ""
                    }) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Tìm kiếm",
                            tint = if (searchActive) AccentGreen else TextMuted
                        )
                    }
                    // Edit — opens FileViewerActivity in edit mode
                    IconButton(onClick = {
                        context.startActivity(
                            Intent(context, FileViewerActivity::class.java).apply {
                                putExtra(FileViewerActivity.EXTRA_START_EDIT, true)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        )
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Sửa file", tint = AccentGreen)
                    }
                    // Premium / Crown button
                    TextButton(onClick = onPremiumClick) {
                        Text(
                            text = if (isPremium) "👑" else "⭐",
                            fontSize = 18.sp
                        )
                    }
                    // Widget button
                    IconButton(onClick = { showManualPinPrompt = true }) {
                        Icon(Icons.Default.Widgets, contentDescription = "Widget", tint = TextMuted)
                    }
                    // Settings
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Cài đặt", tint = TextMuted)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onRecordClick,
                containerColor = AccentGreen,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Mic, contentDescription = "Ghi chú mới", tint = Color.White)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BgDark)
        ) {
            // ── Big title ───────────────────────────────────────────────────────
            Text(
                text = "SỔ GHI CHÚ HÀNG NGÀY",
                color = TextPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                lineHeight = 28.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            )

            // Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(AccentGreen.copy(alpha = 0.3f))
            )

            Spacer(modifier = Modifier.height(4.dp))

            // ── Note list ────────────────────────────────────────────────────────
            if (fileEntries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Chưa có ghi chú nào.\nHãy nói từ widget 🎤",
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp
                    )
                }
            } else if (filteredEntries.isEmpty() && searchQuery.isNotBlank()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Không tìm thấy\n\"$searchQuery\"",
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    items(maskedLines) { line ->
                        if (line.isBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                        } else {
                            Text(
                                text = highlightText(line, searchQuery),
                                style = TextStyle(
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp
                                )
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(88.dp)) }
                }
            }
        }
    }

    // ── Widget prompt ─────────────────────────────────────────────────────────
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
