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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.res.painterResource
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
private val WidgetCardBorder = Color(0xFF2E5470)  // slate-blue trầm, hợp nền tối
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

    // Bắt buộc tạo widget: phím Back thoát hẳn app, không cho bỏ qua dialog
    // Lần sau mở lại sẽ load lại từ đầu, không chiếm tài nguyên nền
    if (isMandatory) {
        BackHandler(enabled = true) {
            (context as? android.app.Activity)?.finishAffinity()
        }
    }

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
                    .widthIn(max = 340.dp)
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
                    // Preview widget thực tế: pill container + 3 icon như widget ngoài màn hình
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .background(
                                color = Color(0xFF0D1B27).copy(alpha = 0.85f),
                                shape = RoundedCornerShape(36.dp)
                            )
                            .then(
                                Modifier.background(
                                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                        colors = listOf(Color(0x1538BDF8), Color(0x0A38BDF8), Color(0x1538BDF8))
                                    ),
                                    shape = RoundedCornerShape(36.dp)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Viền glow nhẹ giả lập bằng border
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                                .background(Color.Transparent, RoundedCornerShape(36.dp))
                                .then(
                                    Modifier.background(
                                        Color.Transparent,
                                        RoundedCornerShape(36.dp)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Mic pill
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color(0x1AFFFFFF), RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_mic),
                                        contentDescription = "Mic",
                                        tint = Color(0xFFBAC4CE),
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                // Note pill — nổi bật hơn (icon trung tâm)
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color(0x1AFFFFFF), RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_note),
                                        contentDescription = "Note",
                                        tint = Color(0xFFBAC4CE),
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                // AI pill
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color(0x1AFFFFFF), RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_ai),
                                        contentDescription = "AI",
                                        tint = Color(0xFFBAC4CE),
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }
                    }

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

// ── Preview ───────────────────────────────────────────────────────────────────

@androidx.compose.ui.tooling.preview.Preview(
    name = "Widget Screen — Mandatory",
    showBackground = true,
    backgroundColor = 0xFF0C161F,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun PreviewPinWidgetMandatory() {
    PinWidgetBottomSheet(isMandatory = true, onDismiss = {})
}

@androidx.compose.ui.tooling.preview.Preview(
    name = "Widget Screen — Optional (nút Để Sau)",
    showBackground = true,
    backgroundColor = 0xFF0C161F,
    widthDp = 390,
    heightDp = 844
)
@Composable
private fun PreviewPinWidgetOptional() {
    PinWidgetBottomSheet(isMandatory = false, onDismiss = {})
}
