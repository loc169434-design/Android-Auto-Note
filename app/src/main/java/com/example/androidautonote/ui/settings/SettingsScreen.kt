package com.example.androidautonote.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.androidautonote.util.AppTheme
import com.example.androidautonote.util.RecognitionLang
import com.example.androidautonote.util.ThemePreferences

private fun getThemePreviewColor(theme: AppTheme): Color {
    return when (theme) {
        AppTheme.OCEAN_BLUE -> Color(0xFF1565C0)
        AppTheme.FOREST_GREEN -> Color(0xFF2E7D32)
        AppTheme.SUNSET_ORANGE -> Color(0xFFE65100)
        AppTheme.LAVENDER -> Color(0xFF7B1FA2)
        AppTheme.ROSE_PINK -> Color(0xFFC2185B)
        AppTheme.MIDNIGHT -> Color(0xFF283593)
        AppTheme.COFFEE -> Color(0xFF5D4037)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    isPremium: Boolean = false,
    onUpgradeClick: () -> Unit = {},
    onRestoreClick: () -> Unit = {},
    onBack: () -> Unit
) {
    val currentTheme by ThemePreferences.currentTheme.collectAsState()
    val autoStopSeconds by ThemePreferences.autoStopSeconds.collectAsState()
    val recognitionLang by ThemePreferences.recognitionLanguage.collectAsState()
    val vibrateOnRecord by ThemePreferences.vibrateOnRecord.collectAsState()
    val cloudSync by ThemePreferences.cloudSync.collectAsState()

    var showAutoStopDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Auto Note", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // === Section: Cài đặt ===
            SectionHeader("Cài đặt")

            Spacer(modifier = Modifier.height(12.dp))

            // === Chủ đề (Theme) ===
            Text(
                text = "Chủ đề",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AppTheme.entries.forEach { theme ->
                    ThemeColorCircle(
                        theme = theme,
                        isSelected = theme == currentTheme,
                        onClick = { ThemePreferences.setTheme(theme) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))

            // === Tài khoản ===
            SectionHeader("Tài khoản")
            Spacer(modifier = Modifier.height(8.dp))

            // Account row (UI only)
            SettingsRow(
                icon = Icons.Default.AccountCircle,
                iconTint = MaterialTheme.colorScheme.primary,
                title = if (isPremium) "Auto Note Pro" else "Auto Note Free",
                subtitle = "user@example.com",
                showArrow = true,
                onClick = onUpgradeClick
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Cloud sync toggle (UI only)
            SettingsToggleRow(
                icon = Icons.Default.Cloud,
                iconTint = MaterialTheme.colorScheme.primary,
                title = "Đồng bộ hóa đám mây",
                checked = cloudSync,
                onCheckedChange = { ThemePreferences.setCloudSync(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))

            // === Cài đặt ghi âm ===
            SectionHeader("Cài đặt ghi âm")
            Spacer(modifier = Modifier.height(8.dp))

            // Auto-stop
            SettingsRow(
                icon = Icons.Default.Timer,
                iconTint = MaterialTheme.colorScheme.primary,
                title = "Tự động dừng ghi âm",
                subtitle = "Dừng sau $autoStopSeconds giây im lặng",
                showArrow = true,
                onClick = { showAutoStopDialog = true }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Recognition language
            SettingsRow(
                icon = Icons.Default.Language,
                iconTint = MaterialTheme.colorScheme.primary,
                title = "Ngôn ngữ nhận dạng",
                subtitle = recognitionLang.displayName,
                showArrow = true,
                onClick = { showLanguageDialog = true }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Vibrate toggle
            SettingsToggleRow(
                icon = Icons.Default.Vibration,
                iconTint = MaterialTheme.colorScheme.primary,
                title = "Rung khi bắt đầu/dừng",
                checked = vibrateOnRecord,
                onCheckedChange = { ThemePreferences.setVibrateOnRecord(it) }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // App version
            Text(
                text = "Auto Note v1.1.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }

    // === Auto-stop duration picker dialog ===
    if (showAutoStopDialog) {
        val options = listOf(15, 30, 45, 60, 90, 120)
        AlertDialog(
            onDismissRequest = { showAutoStopDialog = false },
            title = { Text("Thời gian tự động dừng") },
            text = {
                Column {
                    options.forEach { sec ->
                        val label = if (sec >= 60) "${sec / 60} phút" else "$sec giây"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    ThemePreferences.setAutoStopSeconds(sec)
                                    showAutoStopDialog = false
                                }
                                .padding(vertical = 14.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (sec == autoStopSeconds) FontWeight.Bold else FontWeight.Normal,
                                color = if (sec == autoStopSeconds) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (sec == autoStopSeconds) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAutoStopDialog = false }) {
                    Text("Đóng")
                }
            }
        )
    }

    // === Language picker dialog ===
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Ngôn ngữ nhận dạng") },
            text = {
                Column {
                    RecognitionLang.entries.forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    ThemePreferences.setRecognitionLanguage(lang)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 14.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = lang.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (lang == recognitionLang) FontWeight.Bold else FontWeight.Normal,
                                color = if (lang == recognitionLang) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (lang == recognitionLang) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("Đóng")
                }
            }
        )
    }
}

// ============================================================
// Section Header
// ============================================================

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

// ============================================================
// Theme Color Circle (compact, no label — matching image)
// ============================================================

@Composable
private fun ThemeColorCircle(
    theme: AppTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(getThemePreviewColor(theme))
            .then(
                if (isSelected) {
                    Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                } else {
                    Modifier.border(1.dp, Color.Gray.copy(alpha = 0.2f), CircleShape)
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Đã chọn",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// ============================================================
// Settings Row — clickable with icon, title, subtitle, arrow
// ============================================================

@Composable
private fun SettingsRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String? = null,
    showArrow: Boolean = false,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon circle
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    iconTint.copy(alpha = 0.1f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Text column
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Arrow
        if (showArrow) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ============================================================
// Settings Toggle Row — with switch
// ============================================================

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconTint.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
        }

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
    }
}
