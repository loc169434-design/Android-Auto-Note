package com.tatl.fastnote.ui.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tatl.fastnote.R
import com.tatl.fastnote.billing.RatingManager
import kotlinx.coroutines.delay

// ── Palette khớp giao diện app ────────────────────────────────────────────────
private val RdBgGradient = Brush.verticalGradient(
    listOf(Color(0xFF0A1628), Color(0xFF0D1F35), Color(0xFF091220))
)
private val RdCard       = Color(0xFF112240)
private val RdCardBorder = Color(0xFF1E3A5F)
private val RdDivider    = Color(0xFF1E3A5F)
private val RdTextTitle  = Color(0xFFF0F4FF)
private val RdTextBody   = Color(0xFFBDD0EC)
private val RdTextMuted  = Color(0xFF7A9CC0)
private val RdStarGold   = Color(0xFFFFD700)
private val RdBtnBg      = Brush.horizontalGradient(
    listOf(Color(0xFF1E6FD9), Color(0xFF0EA5E9))
)
private val RdBtnBorder  = Color(0xFF38BDF8)
private val RdGhostBorder = Color(0xFF2A4A6A)

/**
 * Dialog đánh giá 1 lần duy nhất — xuất hiện sau 72h kể từ khi nâng cấp Premium.
 * Các ngôi sao xuất hiện lần lượt với hiệu ứng bounce.
 */
@Composable
fun RatingDialog(
    context: Context,
    onDismiss: () -> Unit
) {
    val message = stringResource(R.string.str_rating_message)
    val btnRate = stringResource(R.string.str_rating_btn_rate)
    val btnSkip = stringResource(R.string.str_rating_btn_skip)

    // ── Animation: 5 ngôi sao xuất hiện lần lượt ─────────────────────────────
    val starScales = remember { List(5) { Animatable(0f) } }

    LaunchedEffect(Unit) {
        starScales.forEachIndexed { i, anim ->
            delay(i * 10L)
            anim.animateTo(
                targetValue  = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness    = Spring.StiffnessMediumLow
                )
            )
        }
    }

    fun dismiss() {
        RatingManager.markDialogShown(context)
        onDismiss()
    }

    fun openPlayStore() {
        val pkg = context.packageName
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$pkg")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        dismiss()
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress    = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .background(RdBgGradient, RoundedCornerShape(24.dp))
        ) {
            // Border layer
            Surface(
                modifier = Modifier.matchParentSize(),
                shape    = RoundedCornerShape(24.dp),
                color    = Color.Transparent,
                border   = BorderStroke(1.dp, RdCardBorder)
            ) {}

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {

                // ── 5 ngôi sao bounce lần lượt ────────────────────────────────
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    starScales.forEach { anim ->
                        Text(
                            text     = "★",
                            fontSize = 34.sp,
                            color    = RdStarGold,
                            modifier = Modifier.scale(anim.value)
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Tiêu đề Z-FN ──────────────────────────────────────────────
                Text(
                    text       = "Z-FN",
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color      = RdTextTitle,
                    letterSpacing = 3.sp
                )

                Spacer(Modifier.height(6.dp))

                HorizontalDivider(color = RdDivider, thickness = 1.dp)

                Spacer(Modifier.height(16.dp))

                // ── Nội dung ──────────────────────────────────────────────────
                Text(
                    text       = message,
                    fontSize   = 13.5.sp,
                    lineHeight  = 21.sp,
                    color      = RdTextBody,
                    textAlign  = TextAlign.Center,
                    fontStyle  = FontStyle.Italic
                )

                Spacer(Modifier.height(24.dp))

                // ── Nút Đánh giá (gradient primary) ───────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .background(RdBtnBg, RoundedCornerShape(14.dp))
                ) {
                    Surface(
                        onClick  = { openPlayStore() },
                        modifier = Modifier.matchParentSize(),
                        shape    = RoundedCornerShape(14.dp),
                        color    = Color.Transparent,
                        border   = BorderStroke(1.dp, RdBtnBorder)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text       = "⭐  $btnRate",
                                fontWeight = FontWeight.Bold,
                                fontSize   = 15.sp,
                                color      = Color.White,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // ── Nút Bỏ qua (ghost) ────────────────────────────────────────
                Surface(
                    onClick  = { dismiss() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape    = RoundedCornerShape(14.dp),
                    color    = Color.Transparent,
                    border   = BorderStroke(1.dp, RdGhostBorder)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text  = btnSkip,
                            fontSize = 13.sp,
                            color = RdTextMuted
                        )
                    }
                }
            }
        }
    }
}
