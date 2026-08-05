package com.example.androidautonote.ui.preview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidautonote.data.db.NoteEntity
import com.example.androidautonote.ui.theme.AndroidAutoNoteTheme
import com.example.androidautonote.util.DateUtils

// ============================================================
// Sample data
// ============================================================

private val previewNotes = listOf(
    NoteEntity(1, "Hoàn thành báo cáo dự án trước 5 giờ",
        "Hôm nay tôi cần phải hoàn thành báo cáo dự án trước 5 giờ chiều và gửi email cho khách hàng. Nhớ đính kèm file Excel thống kê doanh thu tháng 7.",
        System.currentTimeMillis() - 3600_000, System.currentTimeMillis() - 3600_000),
    NoteEntity(2, "Meeting với team backend về API",
        "Thảo luận về API mới cho tính năng notification. Cần review lại endpoint /api/v2/notifications và thêm pagination.",
        System.currentTimeMillis() - 7200_000, System.currentTimeMillis() - 7200_000),
    NoteEntity(3, "Ý tưởng thiết kế app mới",
        "Thêm dark mode, custom theme cho user. Cân nhắc thêm widget hiển thị ghi chú mới nhất trên home screen. Tham khảo Google Keep.",
        System.currentTimeMillis() - 10800_000, System.currentTimeMillis() - 10800_000),
    NoteEntity(4, "Mua sắm cuối tuần",
        "Sữa, trứng, bánh mì, rau xà lách, thịt gà, nước mắm. Nhớ mua thêm bột giặt và nước rửa chén.",
        System.currentTimeMillis() - 86400_000, System.currentTimeMillis() - 86400_000),
)

// ============================================================
// FULL HOME SCREEN PREVIEW (standalone, no ViewModel)
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Preview(
    name = "Home - Timeline (Light)",
    showBackground = true,
    showSystemUi = true,
    device = "id:pixel_5"
)
@Composable
private fun HomeScreenPreview() {
    AndroidAutoNoteTheme(darkTheme = false) {
        var expandedId by remember { mutableStateOf<Long?>(1L) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CalendarToday, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                            Text("Auto Note", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        }
                    },
                    actions = {
                        IconButton(onClick = {}) { Icon(Icons.AutoMirrored.Filled.Article, "Mở file") }
                        IconButton(onClick = {}) { Icon(Icons.Default.AutoAwesome, "AI", tint = MaterialTheme.colorScheme.primary) }
                        IconButton(onClick = {}) { Icon(Icons.Default.Settings, "Cài đặt") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = {}, containerColor = MaterialTheme.colorScheme.primary) {
                    Icon(Icons.Default.Mic, "Ghi chú mới", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        ) { innerPadding ->
            Column(Modifier.fillMaxSize().padding(innerPadding)) {
                // Search bar
                PreviewSearchBar()

                LazyColumn(Modifier.fillMaxSize()) {
                    item { Spacer(Modifier.height(4.dp)) }

                    // Date header: Hôm nay
                    item { PreviewDateHeader("Hôm nay — 06/08/2026") }

                    // Notes
                    item {
                        PreviewTimelineNote(
                            previewNotes[0], "08:30",
                            isExpanded = expandedId == 1L,
                            onClick = { expandedId = if (expandedId == 1L) null else 1L }
                        )
                    }
                    item {
                        PreviewTimelineNote(
                            previewNotes[1], "10:15",
                            isExpanded = expandedId == 2L,
                            onClick = { expandedId = if (expandedId == 2L) null else 2L }
                        )
                    }
                    item {
                        PreviewTimelineNote(
                            previewNotes[2], "14:00",
                            isExpanded = expandedId == 3L,
                            onClick = { expandedId = if (expandedId == 3L) null else 3L }
                        )
                    }

                    // Date header: Hôm qua
                    item { PreviewDateHeader("Hôm qua — 05/08/2026") }

                    item {
                        PreviewTimelineNote(
                            previewNotes[3], "09:00",
                            isExpanded = expandedId == 4L,
                            onClick = { expandedId = if (expandedId == 4L) null else 4L }
                        )
                    }

                    item { Spacer(Modifier.height(88.dp)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(
    name = "Home - Timeline (Dark)",
    showBackground = true,
    showSystemUi = true,
    device = "id:pixel_5",
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun HomeScreenDarkPreview() {
    AndroidAutoNoteTheme(darkTheme = true) {
        var expandedId by remember { mutableStateOf<Long?>(2L) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CalendarToday, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                            Text("Auto Note", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        }
                    },
                    actions = {
                        IconButton(onClick = {}) { Icon(Icons.AutoMirrored.Filled.Article, "Mở file") }
                        IconButton(onClick = {}) { Icon(Icons.Default.AutoAwesome, "AI", tint = MaterialTheme.colorScheme.primary) }
                        IconButton(onClick = {}) { Icon(Icons.Default.Settings, "Cài đặt") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = {}, containerColor = MaterialTheme.colorScheme.primary) {
                    Icon(Icons.Default.Mic, "Ghi chú mới", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        ) { innerPadding ->
            Column(Modifier.fillMaxSize().padding(innerPadding)) {
                PreviewSearchBar()
                LazyColumn(Modifier.fillMaxSize()) {
                    item { Spacer(Modifier.height(4.dp)) }
                    item { PreviewDateHeader("Hôm nay — 06/08/2026") }
                    item { PreviewTimelineNote(previewNotes[0], "08:30", false, {}) }
                    item { PreviewTimelineNote(previewNotes[1], "10:15", true, {}) }
                    item { PreviewTimelineNote(previewNotes[2], "14:00", false, {}) }
                    item { PreviewDateHeader("Hôm qua — 05/08/2026") }
                    item { PreviewTimelineNote(previewNotes[3], "09:00", false, {}) }
                    item { Spacer(Modifier.height(88.dp)) }
                }
            }
        }
    }
}

// ============================================================
// EMPTY STATE PREVIEW
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Preview(
    name = "Home - Empty State",
    showBackground = true,
    showSystemUi = true,
    device = "id:pixel_5"
)
@Composable
private fun HomeEmptyPreview() {
    AndroidAutoNoteTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CalendarToday, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                            Text("Auto Note", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        }
                    },
                    actions = {
                        IconButton(onClick = {}) { Icon(Icons.AutoMirrored.Filled.Article, "Mở file") }
                        IconButton(onClick = {}) { Icon(Icons.Default.AutoAwesome, "AI", tint = MaterialTheme.colorScheme.primary) }
                        IconButton(onClick = {}) { Icon(Icons.Default.Settings, "Cài đặt") }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = {}, containerColor = MaterialTheme.colorScheme.primary) {
                    Icon(Icons.Default.Mic, "Ghi chú mới", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        ) { innerPadding ->
            Column(Modifier.fillMaxSize().padding(innerPadding)) {
                PreviewSearchBar()
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.NoteAlt, null, Modifier.size(80.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Chưa có ghi chú nào.\nBấm 🎤 để bắt đầu!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            }
        }
    }
}

// ============================================================
// EDIT MODE PREVIEW
// ============================================================

@Preview(
    name = "Timeline Note - Edit Mode",
    showBackground = true,
    widthDp = 380
)
@Composable
private fun TimelineNoteEditPreview() {
    AndroidAutoNoteTheme {
        Surface {
            Column(Modifier.padding(8.dp)) {
                PreviewTimelineNoteEditing(
                    note = previewNotes[0],
                    timeText = "08:30"
                )
            }
        }
    }
}

@Preview(
    name = "Timeline Note - Edit Empty (Error)",
    showBackground = true,
    widthDp = 380
)
@Composable
private fun TimelineNoteEditErrorPreview() {
    AndroidAutoNoteTheme {
        Surface {
            Column(Modifier.padding(8.dp)) {
                PreviewTimelineNoteEditingError(
                    note = previewNotes[0],
                    timeText = "08:30"
                )
            }
        }
    }
}

// ============================================================
// COMPONENT: Date Header
// ============================================================

@Preview(name = "Date Header", showBackground = true, widthDp = 380)
@Composable
private fun DateHeaderPreview() {
    AndroidAutoNoteTheme {
        Surface { PreviewDateHeader("Hôm nay — 06/08/2026") }
    }
}

// ============================================================
// COMPONENT: Timeline Note (Collapsed vs Expanded)
// ============================================================

@Preview(name = "Note - Collapsed", showBackground = true, widthDp = 380)
@Composable
private fun TimelineNoteCollapsedPreview() {
    AndroidAutoNoteTheme {
        Surface {
            PreviewTimelineNote(previewNotes[0], "08:30", false, {})
        }
    }
}

@Preview(name = "Note - Expanded", showBackground = true, widthDp = 380)
@Composable
private fun TimelineNoteExpandedPreview() {
    AndroidAutoNoteTheme {
        Surface {
            PreviewTimelineNote(previewNotes[1], "10:15", true, {})
        }
    }
}

// ============================================================
// COMPONENT: Search Bar
// ============================================================

@Preview(name = "Search Bar", showBackground = true, widthDp = 380)
@Composable
private fun SearchBarPreview() {
    AndroidAutoNoteTheme {
        Surface { PreviewSearchBar() }
    }
}

// ============================================================
// Reusable preview components (private, standalone)
// ============================================================

@Composable
private fun PreviewSearchBar() {
    OutlinedTextField(
        value = "",
        onValueChange = {},
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text("Tìm kiếm ghi chú...", style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) },
        shape = RoundedCornerShape(28.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            unfocusedBorderColor = Color.Transparent
        ),
        textStyle = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun PreviewDateHeader(dateText: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(dateText, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
    }
}

@Composable
private fun PreviewTimelineNote(
    note: NoteEntity,
    timeText: String,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(horizontal = 16.dp)
    ) {
        // Left: time + dot + line
        Column(Modifier.width(56.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(timeText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Box(Modifier.size(10.dp).clip(CircleShape).background(if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)))
            Box(Modifier.width(2.dp).weight(1f).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))
        }

        Spacer(Modifier.width(12.dp))

        // Right: Card
        Card(
            modifier = Modifier.weight(1f).padding(bottom = 8.dp).clickable(onClick = onClick),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isExpanded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 4.dp else 2.dp)
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.AccessTime, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                    Spacer(Modifier.width(6.dp))
                    Text(note.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = if (isExpanded) Int.MAX_VALUE else 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ExpandMore, null, Modifier.size(20.dp).rotate(if (isExpanded) 180f else 0f), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }

                Spacer(Modifier.height(6.dp))

                if (!isExpanded) {
                    Text(note.content, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
                }

                AnimatedVisibility(visible = isExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                    Column {
                        Text(note.content, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, lineHeight = 22.sp)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Tạo lúc: ${DateUtils.formatDateTime(note.createdAt)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            TextButton(onClick = {}) {
                                Icon(Icons.Default.Edit, "Sửa", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(4.dp))
                                Text("Sửa", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewTimelineNoteEditing(note: NoteEntity, timeText: String) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(horizontal = 16.dp)) {
        Column(Modifier.width(56.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(timeText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Box(Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
            Box(Modifier.width(2.dp).weight(1f).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))
        }
        Spacer(Modifier.width(12.dp))
        Card(
            modifier = Modifier.weight(1f).padding(bottom = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.AccessTime, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary.copy(0.7f))
                    Spacer(Modifier.width(6.dp))
                    Text(note.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ExpandMore, null, Modifier.size(20.dp).rotate(180f), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f))
                }
                Spacer(Modifier.height(6.dp))
                // Edit TextField
                OutlinedTextField(
                    value = note.content,
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant),
                    supportingText = { Text("${note.content.length} ký tự") }
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = {}) {
                        Icon(Icons.Default.Close, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Hủy")
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {}) {
                        Icon(Icons.Default.Check, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                        Text("Lưu", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewTimelineNoteEditingError(note: NoteEntity, timeText: String) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(horizontal = 16.dp)) {
        Column(Modifier.width(56.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(timeText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Box(Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
            Box(Modifier.width(2.dp).weight(1f).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))
        }
        Spacer(Modifier.width(12.dp))
        Card(
            modifier = Modifier.weight(1f).padding(bottom = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.AccessTime, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary.copy(0.7f))
                    Spacer(Modifier.width(6.dp))
                    Text(note.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ExpandMore, null, Modifier.size(20.dp).rotate(180f), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f))
                }
                Spacer(Modifier.height(6.dp))
                // Error state — empty content
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    isError = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant),
                    supportingText = { Text("Nội dung không được để trống", color = MaterialTheme.colorScheme.error) }
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = {}) {
                        Icon(Icons.Default.Close, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Hủy")
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {}, enabled = false) {
                        Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Lưu")
                    }
                }
            }
        }
    }
}
