package com.tatl.fastnote.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tatl.fastnote.util.PinWidgetHelper
import com.tatl.fastnote.util.ThemePreferences
import com.tatl.fastnote.widget.TripleActionWidgetReceiver

/**
 * Bottom sheet prompting the user to add the Triple Action (1×3) widget.
 *
 * Flow when user taps the button:
 *  1. Save pinned state to SharedPreferences
 *  2. Call requestPinAppWidget (system shows placement dialog on launcher)
 *  3. Navigate to home screen (user lands right where they'll place widget)
 *  4. Dismiss this sheet (so when they return the sheet is gone)
 *
 * When [isMandatory] = true (widget was removed / never added):
 *   - sheet cannot be dismissed by swiping — only via the button
 * When [isMandatory] = false (user tapped Widget icon voluntarily):
 *   - sheet can be swiped away
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinWidgetBottomSheet(
    isMandatory: Boolean = false,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { !isMandatory } // block swipe-dismiss when mandatory
    )

    ModalBottomSheet(
        onDismissRequest = {
            if (!isMandatory) onDismiss()
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            // ── Header ──────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (isMandatory)
                                MaterialTheme.colorScheme.errorContainer
                            else
                                MaterialTheme.colorScheme.primaryContainer,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Widgets,
                        contentDescription = null,
                        tint = if (isMandatory)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Column {
                    Text(
                        text = if (isMandatory)
                            "⚠️ Widget bị xóa – Vui lòng thêm lại"
                        else
                            "Thêm Widget 3×1 ra Màn hình chính",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isMandatory)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isMandatory)
                            "Widget đã bị xóa. Bấm bên dưới để cài lại ngay."
                        else
                            "1 widget – 3 tính năng: Micro 🎙, Gemini ✨, File 📂",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Widget preview card ──────────────────────────────
            WidgetPreviewCard()

            Spacer(modifier = Modifier.height(20.dp))

            // ── Single action button ─────────────────────────────
            // Tapping this fires requestPinAppWidget — the system shows
            // its placement dialog on top of the current screen.
            // Do NOT dismiss immediately: let the system dialog appear first.
            // When user returns after placing the widget, ON_RESUME re-checks
            // isWidgetActive() → prompt auto-disappears.
            Button(
                onClick = {
                    ThemePreferences.setWidgetPinned(true)
                    PinWidgetHelper.pinWidget(
                        context,
                        TripleActionWidgetReceiver::class.java,
                        "Bộ 3 tính năng"
                    )
                    // Dismiss the sheet AFTER firing — system dialog appears on top.
                    // When user returns, ON_RESUME will hide the prompt automatically.
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isMandatory)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.Widgets,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isMandatory)
                        "Cài lại Widget ngay →"
                    else
                        "Thêm Widget 3×1 vào màn hình →",
                    fontWeight = FontWeight.Bold
                )
            }

            // Dismiss button (optional mode only)
            if (!isMandatory) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        "Để sau",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Visual preview of the Triple Action widget showing its 3 buttons.
 */
@Composable
private fun WidgetPreviewCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(Color(0xFF1A1A2E), RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PreviewButton(
                icon = Icons.Default.Mic,
                label = "Micro",
                color = Color(0xFF1565C0),
                modifier = Modifier.weight(1f)
            )
            PreviewButton(
                icon = Icons.Default.AutoAwesome,
                label = "Gemini",
                color = Color(0xFF6A1B9A),
                modifier = Modifier.weight(1f)
            )
            PreviewButton(
                icon = Icons.Default.Description,
                label = "File",
                color = Color(0xFF2E7D32),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PreviewButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .background(color, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                label,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
