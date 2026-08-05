package com.example.androidautonote.ui.preview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.androidautonote.data.db.NoteEntity
import com.example.androidautonote.ui.recording.AudioWaveform
import com.example.androidautonote.ui.recording.GlowingMicButton
import com.example.androidautonote.ui.recording.RecordingScreen
import com.example.androidautonote.ui.settings.SettingsScreen
import com.example.androidautonote.ui.theme.AndroidAutoNoteTheme

// ============================================================
// Fake data for previews
// ============================================================

private val sampleNotes = listOf(
    NoteEntity(
        id = 1,
        title = "Hoàn thành báo cáo dự án",
        content = "Hôm nay tôi cần phải hoàn thành báo cáo dự án trước 5 giờ chiều và gửi email cho khách hàng. Nhớ đính kèm file Excel thống kê doanh thu tháng 7.",
        createdAt = System.currentTimeMillis() - 3600_000, // 1h ago
        updatedAt = System.currentTimeMillis() - 3600_000
    ),
    NoteEntity(
        id = 2,
        title = "Meeting với team backend",
        content = "Thảo luận về API mới cho tính năng notification. Cần review lại endpoint /api/v2/notifications và thêm pagination. Deadline: thứ 6 tuần này.",
        createdAt = System.currentTimeMillis() - 7200_000, // 2h ago
        updatedAt = System.currentTimeMillis() - 7200_000
    ),
    NoteEntity(
        id = 3,
        title = "Ý tưởng thiết kế app",
        content = "Thêm dark mode, custom theme cho user. Cân nhắc thêm widget hiển thị ghi chú mới nhất trên home screen. Tham khảo design của Google Keep và Notion.",
        createdAt = System.currentTimeMillis() - 86400_000, // yesterday
        updatedAt = System.currentTimeMillis() - 86400_000
    ),
    NoteEntity(
        id = 4,
        title = "Mua sắm cuối tuần",
        content = "Sữa, trứng, bánh mì, rau xà lách, thịt gà, nước mắm. Nhớ mua thêm bột giặt và nước rửa chén.",
        createdAt = System.currentTimeMillis() - 86400_000 - 3600_000,
        updatedAt = System.currentTimeMillis() - 86400_000 - 3600_000
    )
)

// ============================================================
// 1. RECORDING SCREEN PREVIEWS
// ============================================================

@Preview(
    name = "Recording - Active",
    showBackground = true,
    showSystemUi = true,
    device = "id:pixel_5"
)
@Composable
private fun RecordingScreenActivePreview() {
    AndroidAutoNoteTheme {
        RecordingScreen(
            service = null,
            isBound = true,
            onCancel = {},
            onPause = {},
            onResume = {},
            onManualStop = {}
        )
    }
}

@Preview(
    name = "Recording - Dark",
    showBackground = true,
    showSystemUi = true,
    device = "id:pixel_5",
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun RecordingScreenDarkPreview() {
    AndroidAutoNoteTheme(darkTheme = true) {
        RecordingScreen(
            service = null,
            isBound = true,
            onCancel = {},
            onPause = {},
            onResume = {},
            onManualStop = {}
        )
    }
}

// ============================================================
// 2. MIC BUTTON & WAVEFORM
// ============================================================

@Preview(name = "Mic - Active (Glow)", showBackground = true)
@Composable
private fun GlowingMicActivePreview() {
    AndroidAutoNoteTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                GlowingMicButton(isActive = true)
            }
        }
    }
}

@Preview(name = "Mic - Inactive", showBackground = true)
@Composable
private fun GlowingMicInactivePreview() {
    AndroidAutoNoteTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                GlowingMicButton(isActive = false)
            }
        }
    }
}

@Preview(name = "Waveform - Active", showBackground = true, widthDp = 380, heightDp = 120)
@Composable
private fun WaveformActivePreview() {
    AndroidAutoNoteTheme {
        Surface { AudioWaveform(isActive = true, modifier = Modifier.fillMaxSize()) }
    }
}

@Preview(name = "Waveform - Inactive", showBackground = true, widthDp = 380, heightDp = 120)
@Composable
private fun WaveformInactivePreview() {
    AndroidAutoNoteTheme {
        Surface { AudioWaveform(isActive = false, modifier = Modifier.fillMaxSize()) }
    }
}

// ============================================================
// 3. SETTINGS SCREEN PREVIEWS
// ============================================================

@Preview(
    name = "Settings - Free",
    showBackground = true,
    showSystemUi = true,
    device = "id:pixel_5"
)
@Composable
private fun SettingsFreePreview() {
    AndroidAutoNoteTheme {
        SettingsScreen(
            isPremium = false,
            onUpgradeClick = {},
            onRestoreClick = {},
            onBack = {}
        )
    }
}

@Preview(
    name = "Settings - Premium",
    showBackground = true,
    showSystemUi = true,
    device = "id:pixel_5"
)
@Composable
private fun SettingsPremiumPreview() {
    AndroidAutoNoteTheme {
        SettingsScreen(
            isPremium = true,
            onUpgradeClick = {},
            onRestoreClick = {},
            onBack = {}
        )
    }
}

@Preview(
    name = "Settings - Dark",
    showBackground = true,
    showSystemUi = true,
    device = "id:pixel_5",
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun SettingsDarkPreview() {
    AndroidAutoNoteTheme(darkTheme = true) {
        SettingsScreen(
            isPremium = false,
            onUpgradeClick = {},
            onRestoreClick = {},
            onBack = {}
        )
    }
}
