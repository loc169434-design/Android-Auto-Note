package com.tatl.fastnote.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tatl.fastnote.R
import com.tatl.fastnote.ui.common.AppToast
import com.tatl.fastnote.ui.theme.InterFontFamily
import com.tatl.fastnote.ui.theme.NotoSansFontFamily
import com.tatl.fastnote.util.FileHelper
import com.tatl.fastnote.util.PinWidgetHelper
import com.tatl.fastnote.util.ThemePreferences
import com.tatl.fastnote.widget.TripleActionWidgetReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ── Bảng màu giao diện chuẩn theo thiết kế ───────────────────────────────────
private val HomeBgTop         = Color(0xFF1A2B39)  // Slate-blue trầm phía trên
private val HomeBgMid         = Color(0xFF12202C)  // Slate-blue đậm ở giữa
private val HomeBgBottom      = Color(0xFF0C161F)  // Xanh đêm trầm phía dưới
private val TopPillBg         = Color(0xFF223547).copy(alpha = 0.85f)
private val TopPillBorder     = Color(0xFF354C62)
private val BottomBarBg       = Color(0xFF0D1721).copy(alpha = 0.96f)
private val BottomBarBorder   = Color(0xFF1B2B3A)
private val HomeTextPrimary   = Color(0xFFFFFFFF)
private val HomeTextMuted     = Color(0xFF888888)
private val HomeHeaderItalic  = Color(0xFF8EAAB9)  // Xám xanh nghiêng cho nhãn ngày giờ
private val HomeSearchActive  = Color(0xFFFFFFFF)
private val RedHighlight      = Color(0xFFFF5252)
private val RedBg             = Color(0x33FF5252)

// Rule: dòng ngày tháng cố định
private val IS_DATE_LINE: (String) -> Boolean = { line ->
    val t = line.trimStart()
    t.startsWith("- Thứ") || t.startsWith("- Chủ") || t.startsWith("- Ngày")
}

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
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val searchFocusRequester = remember { FocusRequester() }

    // ── State ─────────────────────────────────────────────────────────────────
    val hasPinnedWidget by ThemePreferences.hasPinnedWidget.collectAsState()
    val currentHasPinned by rememberUpdatedState(hasPinnedWidget)
    var showManualPinPrompt by remember { mutableStateOf(false) }
    var widgetActiveNow by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var fileEntries by remember { mutableStateOf<List<FileHelper.NoteEntry>>(emptyList()) }
    var searchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // ── Edit Mode State ───────────────────────────────────────────────────────
    var isEditMode by remember { mutableStateOf(false) }
    var editTfv by remember { mutableStateOf(TextFieldValue("")) }
    var originalContent by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var showProtectToast by remember { mutableStateOf(false) }
    var autoSaveJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    // ── Save Function ─────────────────────────────────────────────────────────
    fun doSave() {
        if (isSaving) return
        isSaving = true
        autoSaveJob?.cancel()
        scope.launch {
            val textToSave = reverseEntries(editTfv.text)
            val error = withContext(Dispatchers.IO) {
                FileHelper.saveEditedRaw(context, originalContent, textToSave)
            }
            isSaving = false
            if (error == null) {
                keyboardController?.hide()
                withContext(Dispatchers.IO) {
                    fileEntries = FileHelper.parseEntries(context)
                    com.tatl.fastnote.sync.GoogleDriveSyncManager.sync(context)
                }
                isEditMode = false
            } else {
                showProtectToast = true
            }
        }
    }

    // ── Back Handler khi ở Edit Mode hoặc Search Mode ────────────────────────
    BackHandler(enabled = isEditMode || searchActive) {
        if (isEditMode) {
            keyboardController?.hide()
            doSave()
        } else if (searchActive) {
            focusManager.clearFocus()
            keyboardController?.hide()
            searchActive = false
            searchQuery = ""
        }
    }

    // ── Focus & Keyboard khi mở/đóng Search ──────────────────────────────────
    LaunchedEffect(searchActive) {
        if (searchActive) {
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        } else {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

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

    // ── Filter ────────────────────────────────────────────────────────────────
    val filteredEntries = if (searchQuery.isBlank()) fileEntries
    else fileEntries.filter {
        it.content.contains(searchQuery, ignoreCase = true) ||
        it.header.contains(searchQuery, ignoreCase = true)
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ROOT: Nền Slate-Blue gradient mượt mà theo ảnh thiết kế
    // ─────────────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(HomeBgTop, HomeBgMid, HomeBgBottom)
                )
            )
    ) {
        if (isEditMode) {
            // ═════════════════════════════════════════════════════════════════
            //  GIAO DIỆN CHỈNH SỬA TRƠN (EDIT MODE)
            // ═════════════════════════════════════════════════════════════════
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

                // ── Vùng soạn thảo văn bản trơn với thanh cuộn ─────────────────
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
                        value = editTfv,
                        onValueChange = { newTfv ->
                            val oldText = editTfv.text
                            val newText = newTfv.text

                            if (newText == oldText) {
                                editTfv = newTfv
                                return@BasicTextField
                            }

                            val oldLines = oldText.lines()
                            val newLines = newText.lines()

                            // Rule: bảo vệ header ngày giờ cố định
                            fun headerOnly(line: String): String {
                                val idx = line.indexOf(": ")
                                return if (idx != -1) line.substring(0, idx) else line
                            }
                            val oldHeaders = oldLines.filter(IS_DATE_LINE).map { headerOnly(it) }
                            val newHeaders = newLines.filter(IS_DATE_LINE).map { headerOnly(it) }
                            if (oldHeaders.size != newHeaders.size ||
                                oldHeaders.zip(newHeaders).any { (a, b) -> a != b }
                            ) {
                                showProtectToast = true
                                return@BasicTextField
                            }

                            // Rule: giới hạn xoá quá nhiều ký tự
                            val charsRemoved = oldText.length - newText.length
                            if (charsRemoved >= 100) {
                                return@BasicTextField
                            }

                            editTfv = newTfv

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
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                            .verticalScroll(scrollState),
                        textStyle = TextStyle(
                            fontFamily = NotoSansFontFamily,
                            color = Color(0xFFF1F5F9),
                            fontSize = 15.sp,
                            lineHeight = 26.sp,
                            letterSpacing = 0.1.sp
                        ),
                        cursorBrush = SolidColor(Color.White),
                        decorationBox = { innerTextField ->
                            if (editTfv.text.isEmpty()) {
                                Text(
                                    text = "Chưa có ghi chú nào...",
                                    color = HomeTextMuted,
                                    fontFamily = NotoSansFontFamily,
                                    fontSize = 15.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                }

                // ── Thanh công cụ dưới: [ ⌨️ Bàn phím ] - [ LƯU ] - [ 🎤 Mic xanh ] ──
                @OptIn(ExperimentalLayoutApi::class)
                val isImeVisible = WindowInsets.isImeVisible

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = BottomBarBg,
                    border = BorderStroke(width = 0.5.dp, color = BottomBarBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Nút nhỏ góc trái: Bật/Tắt ẩn hiện bàn phím
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

                        // Nút LƯU ở giữa
                        Surface(
                            onClick = { doSave() },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E3A8A).copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, Color(0xFF3B82F6)),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 24.dp),
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

                        // Nút Micro xanh lá tròn góc phải (như ảnh)
                        Surface(
                            onClick = {
                                keyboardController?.hide()
                                onRecordClick()
                            },
                            shape = CircleShape,
                            color = Color(0xFF00E676),
                            modifier = Modifier.size(46.dp),
                            shadowElevation = 4.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Ghi âm",
                                    tint = Color(0xFF003300),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }

        } else {
            // ═════════════════════════════════════════════════════════════════
            //  GIAO DIỆN BÌNH THƯỜNG (XEM DANH SÁCH GHI CHÚ)
            // ═════════════════════════════════════════════════════════════════
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }
            ) {

                // ── Top Bar: [ 💻 Gửi PC ] & [ 👑 Premium ] ───────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Trái: Nút Gửi PC dạng Pill
                    Surface(
                        onClick = onComputerClick,
                        shape = RoundedCornerShape(8.dp),
                        color = TopPillBg,
                        border = BorderStroke(1.dp, TopPillBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Computer,
                                contentDescription = "Gửi PC",
                                tint = Color(0xFF90CAF9),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Gửi PC",
                                color = HomeTextPrimary,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp
                            )
                        }
                    }

                    // Phải: Nút Premium dạng Pill
                    Surface(
                        onClick = onPremiumClick,
                        shape = RoundedCornerShape(8.dp),
                        color = TopPillBg,
                        border = BorderStroke(1.dp, TopPillBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_premium),
                                contentDescription = "Premium",
                                tint = Color(0xFFFFB800),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Premium",
                                color = Color(0xFFFFD54F),
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 15.sp
                            )
                        }
                    }
                }

                // ── Title: SỔ GHI CHÚ HÀNG NGÀY ──────────────────────────────
                Text(
                    text = "SỔ GHI CHÚ HÀNG NGÀY",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = HomeTextPrimary,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 12.dp)
                )

                // ── Search bar (chỉ hiện khi active) ─────────────────────────
                if (searchActive) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(searchFocusRequester)
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        placeholder = {
                            Text(
                                stringResource(R.string.str_search_hint),
                                color = HomeTextMuted,
                                fontFamily = NotoSansFontFamily,
                                fontSize = 14.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = HomeTextPrimary,
                            unfocusedTextColor = HomeTextPrimary,
                            focusedBorderColor = HomeSearchActive,
                            unfocusedBorderColor = Color(0xFF555555),
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

                // ── Danh sách ghi chú phẳng (cuộn vô tận không đóng card) ─────
                if (fileEntries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.str_empty_notes),
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
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            },
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
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }
                    ) {
                        item { Spacer(Modifier.height(4.dp)) }
                        items(filteredEntries) { entry ->
                            NoteEntryItem(
                                entry = entry,
                                searchQuery = searchQuery
                            )
                            Spacer(Modifier.height(24.dp))
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                    }
                }

                // ── Bottom Bar: [ ✏️ SỬA ] và [ 🔍 TÌM KIẾM ] ─────────────────
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = BottomBarBg,
                    border = BorderStroke(width = 0.5.dp, color = BottomBarBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(horizontal = 48.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Nút SỬA bên trái -> Bật trực tiếp Edit Mode
                        Row(
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    scope.launch {
                                        val raw = withContext(Dispatchers.IO) {
                                            FileHelper.readRawFile(context)
                                        }
                                        originalContent = raw
                                        val reversed = reverseEntries(raw)
                                        editTfv = TextFieldValue(reversed, selection = TextRange(0))
                                        isEditMode = true
                                    }
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "SỬA",
                                tint = Color(0xFFCBD5E1),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "SỬA",
                                color = Color(0xFFCBD5E1),
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Nút TÌM KIẾM bên phải
                        Row(
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    searchActive = !searchActive
                                    if (!searchActive) searchQuery = ""
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "TÌM KIẾM",
                                tint = if (searchActive) Color.White else Color(0xFFCBD5E1),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "TÌM KIẾM",
                                color = if (searchActive) Color.White else Color(0xFFCBD5E1),
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

            } // end Normal Mode Column
        }

        // ── Widget prompt overlay ─────────────────────────────────────────────
        if (shouldShowWidgetPrompt && !isEditMode) {
            PinWidgetBottomSheet(
                isMandatory = isPromptMandatory,
                onDismiss = {
                    if (widgetWasRemoved) ThemePreferences.setWidgetPinned(true)
                    showManualPinPrompt = false
                }
            )
        }

        // ── Toast cảnh báo bảo vệ ngày tháng ──────────────────────────────────
        AppToast(
            visible = showProtectToast,
            message = "Không thể xóa dòng thời gian ghi chú",
            durationMs = 2000L,
            onDismiss = { showProtectToast = false }
        )
    }
}

// ── Đảo ngược thứ tự các entry trong file ghi chú ────────────────────────────
private fun reverseEntries(raw: String): String {
    if (raw.isBlank()) return raw
    val blocks = raw.split("\n\n")
        .map { it.trim() }
        .filter { it.isNotBlank() }
    return blocks.reversed().joinToString("\n\n")
}

// ── Note entry item ───────────────────────────────────────────────────────────

@Composable
private fun NoteEntryItem(entry: FileHelper.NoteEntry, searchQuery: String) {
    val maskedContent = remember(entry.content) {
        val lines = entry.content.lines()
        FileHelper.maskSensitive(lines).joinToString("\n")
    }

    val annotatedString = remember(entry.header, maskedContent, searchQuery) {
        buildFormattedNoteEntry(entry.header, maskedContent, searchQuery)
    }

    Text(
        text = annotatedString,
        style = TextStyle(
            fontFamily = NotoSansFontFamily,
            fontSize = 15.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.1.sp
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

// ── Formatter hỗ trợ Markdown bold và Search highlight ───────────────────────

private fun buildFormattedNoteEntry(
    header: String,
    content: String,
    query: String
): AnnotatedString {
    return buildAnnotatedString {
        // 1. Nhãn ngày giờ: *Thứ..., ngày... lúc HH.MM: (nghiêng, màu xám xanh)
        val headerPrefix = "*$header: "
        val startHeader = length
        append(headerPrefix)
        val endHeader = length
        addStyle(
            SpanStyle(
                color = HomeHeaderItalic,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Normal
            ),
            startHeader,
            endHeader
        )

        // 2. Nội dung text (hỗ trợ **bold** markdown)
        val startContent = length
        parseMarkdownContent(content)
        val endContent = length

        // Màu mặc định cho nội dung
        addStyle(
            SpanStyle(
                color = Color(0xFFF1F5F9),
                fontWeight = FontWeight.Normal
            ),
            startContent,
            endContent
        )

        // 3. Highlight kết quả tìm kiếm
        if (query.isNotBlank()) {
            val fullText = toAnnotatedString().text
            val lowerFull = fullText.lowercase()
            val lq = query.lowercase()
            var last = 0
            var idx = lowerFull.indexOf(lq, last)
            while (idx != -1) {
                addStyle(
                    SpanStyle(
                        color = RedHighlight,
                        background = RedBg,
                        fontWeight = FontWeight.Bold
                    ),
                    idx,
                    idx + query.length
                )
                last = idx + query.length
                idx = lowerFull.indexOf(lq, last)
            }
        }
    }
}

private fun AnnotatedString.Builder.parseMarkdownContent(content: String) {
    val boldRegex = Regex("""\*\*(.*?)\*\*""")
    var lastIndex = 0
    val matches = boldRegex.findAll(content)

    for (match in matches) {
        if (match.range.first > lastIndex) {
            append(content.substring(lastIndex, match.range.first))
        }
        val boldText = match.groupValues[1]
        val start = length
        append(boldText)
        val end = length
        addStyle(
            SpanStyle(
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            start,
            end
        )
        lastIndex = match.range.last + 1
    }
    if (lastIndex < content.length) {
        append(content.substring(lastIndex))
    }
}
