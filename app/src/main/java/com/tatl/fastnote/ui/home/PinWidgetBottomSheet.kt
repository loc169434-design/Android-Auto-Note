package com.tatl.fastnote.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tatl.fastnote.ui.theme.AppBgBlack
import com.tatl.fastnote.ui.theme.AppTextMuted
import com.tatl.fastnote.ui.theme.InterFontFamily
import com.tatl.fastnote.util.PinWidgetHelper
import com.tatl.fastnote.util.ThemePreferences
import com.tatl.fastnote.widget.TripleActionWidgetReceiver

/**
 * Full-screen OLED black widget invite screen.
 *
 * Design:
 *   - Nền đen tuyệt đối
 *   - Hình vuông viền mỏng ở giữa, chữ "XIN MỜI TẠO WIDGET"
 *   - Nút "TIẾP TỤC" spacing đều ở dưới
 *
 * Thay thế hoàn toàn ModalBottomSheet cũ.
 */
@Composable
fun PinWidgetBottomSheet(
    isMandatory: Boolean = false,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBgBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top spacer — đẩy hộp vuông lên giữa ──────────────────────────
            Spacer(Modifier.weight(1f))

            // ── Hình vuông viền mỏng ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)                      // vuông tỉ lệ 1:1
                    .border(
                        width = 0.7.dp,
                        color = Color(0xFF3A3A3A)         // viền xám rất nhẹ
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "XIN MỜI TẠO WIDGET",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    letterSpacing = 1.5.sp,
                    color = Color(0xFFCCCCCC),
                    textAlign = TextAlign.Center
                )
            }

            // ── Bottom spacer — khoảng cách đều từ hộp xuống nút ─────────────
            Spacer(Modifier.weight(1f))

            // ── Nút TIẾP TỤC ─────────────────────────────────────────────────
            TextButton(
                onClick = {
                    ThemePreferences.setWidgetPinned(true)
                    PinWidgetHelper.pinWidget(
                        context,
                        TripleActionWidgetReceiver::class.java,
                        "Bộ 3 tính năng"
                    )
                    onDismiss()
                }
            ) {
                Text(
                    text = "TIẾP TỤC",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    letterSpacing = 3.sp,
                    color = AppTextMuted
                )
            }

            // Nút bỏ qua — chỉ hiện khi không bắt buộc
            if (!isMandatory) {
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "để sau",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = Color(0xFF444444)
                    )
                }
            }

            Spacer(Modifier.height(48.dp))
        }
    }
}
