package com.tatl.fastnote.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tatl.fastnote.R
import com.tatl.fastnote.ui.theme.InterFontFamily
import com.tatl.fastnote.ui.theme.NotoSansFontFamily
import com.tatl.fastnote.util.PinWidgetHelper
import com.tatl.fastnote.util.ThemePreferences
import com.tatl.fastnote.widget.TripleActionWidgetReceiver

// ── Bảng màu chuẩn Slate-Blue ────────────────────────────────────────────────
private val BgTop          = Color(0xFF1A2B39)
private val BgMid          = Color(0xFF12202C)
private val BgBottom       = Color(0xFF0C161F)
private val WidgetCardBg   = Color(0xFF142433).copy(alpha = 0.9f)
private val WidgetCardBorder = Color(0xFF38BDF8)
private val TextTitle      = Color(0xFFF8FAFC)
private val TextMuted      = Color(0xFF94A3B8)

/**
 * Màn hình mời tạo Widget — Nền Slate-Blue chuẩn đồng bộ app.
 * Chạm trực tiếp vào ô vuông "XIN MỜI TẠO WIDGET" để tạo widget ngay.
 */
@Composable
fun PinWidgetBottomSheet(
    isMandatory: Boolean = false,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val widgetTitle = stringResource(R.string.widget_triple_action_name)

    fun pinAndDismiss() {
        // KHÔNG set hasPinned=true ở đây — chỉ set sau khi widget
        // thực sự được xác nhận đặt lên màn hình (trong WidgetPlacedReceiver)
        PinWidgetHelper.pinWidget(
            context,
            TripleActionWidgetReceiver::class.java,
            widgetTitle
        )
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BgTop, BgMid, BgBottom)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top spacer ────────────────────────────────────────────────────
            Spacer(Modifier.weight(1f))

            // ── Ô vuông tạo widget (Chạm trực tiếp vào ô để tạo) ─────────────
            Surface(
                onClick = { pinAndDismiss() },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                shape = RoundedCornerShape(20.dp),
                color = WidgetCardBg,
                border = BorderStroke(1.5.dp, WidgetCardBorder.copy(alpha = 0.7f)),
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Icon Widget nổi bật
                    Icon(
                        imageVector = Icons.Default.Widgets,
                        contentDescription = stringResource(R.string.str_widget_icon_desc),
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = stringResource(R.string.str_create_widget),
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        letterSpacing = 1.sp,
                        color = TextTitle,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.str_tap_to_create_widget),
                        fontFamily = NotoSansFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }

            // ── Bottom spacer ─────────────────────────────────────────────────
            Spacer(Modifier.weight(1f))

            // Nút để sau — chỉ hiện khi không bắt buộc
            if (!isMandatory) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.padding(bottom = 32.dp)
                ) {
                    Text(
                        text = stringResource(R.string.btn_later),
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        letterSpacing = 1.5.sp,
                        color = TextMuted
                    )
                }
            } else {
                Spacer(Modifier.height(48.dp))
            }
        }
    }
}
