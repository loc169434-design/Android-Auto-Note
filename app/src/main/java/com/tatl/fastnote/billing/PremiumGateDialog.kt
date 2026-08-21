package com.tatl.fastnote.billing

import android.app.Activity
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.android.billingclient.api.ProductDetails
import com.tatl.fastnote.R
import com.tatl.fastnote.ui.theme.InterFontFamily
import com.tatl.fastnote.ui.theme.NotoSansFontFamily
import kotlinx.coroutines.launch

// ── Bảng màu chuẩn Slate-Blue ────────────────────────────────────────────────
private val DialogBgTop      = Color(0xFF1C2D3D)
private val DialogBgBottom   = Color(0xFF101B26)
private val DialogBorder     = Color(0xFF2E465E)
private val CardBg           = Color(0xFF0B151E)
private val CardBorder       = Color(0xFF253B4F)
private val TextTitle        = Color(0xFFF8FAFC)
private val TextBody         = Color(0xFFE2E8F0)
private val TextMuted        = Color(0xFF94A3B8)
private val AccentGold       = Color(0xFFFFB800)

/**
 * Premium gate dialog — giao diện Slate-Blue sang trọng, chữ to rõ ràng.
 */
@Composable
fun PremiumGateDialog(
    onDismiss:        () -> Unit,
    onPremiumGranted: () -> Unit
) {
    val context  = LocalContext.current
    val activity = context as? Activity
    val scope    = rememberCoroutineScope()

    var billing     by remember { mutableStateOf<BillingManager?>(null) }
    var product     by remember { mutableStateOf<ProductDetails?>(null) }
    var isLoading   by remember { mutableStateOf(true) }
    var statusText  by remember { mutableStateOf("") }
    var isRestoring by remember { mutableStateOf(false) }

    // Connect to billing on first compose
    LaunchedEffect(Unit) {
        val bm = BillingManager(context)
        billing = bm
        val connected = bm.connect()
        if (connected) {
            product = bm.queryProduct()
        }
        isLoading = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f),
            shape = RoundedCornerShape(20.dp),
            color = Color.Transparent,
            border = BorderStroke(1.dp, DialogBorder),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(DialogBgTop, DialogBgBottom)
                        )
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // ── Huy hiệu vương miện Premium ──────────────────────────────
                Surface(
                    shape = CircleShape,
                    color = AccentGold.copy(alpha = 0.15f),
                    border = BorderStroke(1.5.dp, AccentGold.copy(alpha = 0.8f)),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_premium),
                            contentDescription = "Premium",
                            tint = AccentGold,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── Tiêu đề ───────────────────────────────────────────────────
                Text(
                    text = "QUYỀN LỢI BẢN PREMIUM",
                    color = TextTitle,
                    fontSize = 18.sp,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ── Khung danh sách quyền lợi ────────────────────────────────
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = CardBg,
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Quyền lợi 1: Gửi PC
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(top = 2.dp)
                            )
                            Text(
                                text = "Gửi tệp tin nén bảo mật mã hóa AES-256 sang máy tính cá nhân (Gửi PC).",
                                color = TextBody,
                                fontSize = 15.sp,
                                fontFamily = NotoSansFontFamily,
                                lineHeight = 22.sp
                            )
                        }

                        // Quyền lợi 2: Google Drive
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(top = 2.dp)
                            )
                            Text(
                                text = "Tự động sao lưu và đồng bộ an toàn dữ liệu lên đám mây cá nhân Google Drive.",
                                color = TextBody,
                                fontSize = 15.sp,
                                fontFamily = NotoSansFontFamily,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Buy button ────────────────────────────────────────────────
                val priceLabel = product?.oneTimePurchaseOfferDetails?.formattedPrice ?: "200k"

                if (isLoading) {
                    CircularProgressIndicator(
                        color = AccentGold,
                        modifier = Modifier.size(36.dp)
                    )
                } else {
                    Button(
                        onClick = {
                            val bm = billing ?: return@Button
                            val pd = product
                            if (pd == null) {
                                statusText = "Không tải được sản phẩm. Thử lại sau."
                                return@Button
                            }
                            if (activity == null) return@Button
                            bm.launchBillingFlow(
                                activity        = activity,
                                productDetails  = pd,
                                onSuccess       = { statusText = ""; onPremiumGranted() },
                                onFailed        = { msg -> statusText = msg }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2563EB)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Text(
                            text       = "Nâng Cấp Ngay - $priceLabel / Trọn Đời",
                            color      = Color.White,
                            fontSize   = 15.sp,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // ── Status / error ────────────────────────────────────────────
                if (statusText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text      = statusText,
                        color     = Color(0xFFFF5252),
                        fontSize  = 13.sp,
                        fontFamily = NotoSansFontFamily,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ── Restore link ──────────────────────────────────────────────
                TextButton(
                    onClick = {
                        scope.launch {
                            isRestoring = true
                            val bm = billing
                            if (bm != null) {
                                val hasPurchase = bm.queryExistingPurchases()
                                if (hasPurchase) {
                                    PremiumManager.setPremium()
                                    onPremiumGranted()
                                } else {
                                    statusText = "Không tìm thấy giao dịch trước đó."
                                }
                            }
                            isRestoring = false
                        }
                    }
                ) {
                    Text(
                        text     = if (isRestoring) "Đang kiểm tra..." else "Đã mua trước đó? Chạm để khôi phục",
                        color    = Color(0xFF60A5FA),
                        fontFamily = InterFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // ── Nút ĐỂ SAU (To, rõ ràng, dễ nhìn) ────────────────────────
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF334B63)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFCBD5E1)
                    )
                ) {
                    Text(
                        text     = "ĐỂ SAU",
                        color    = Color(0xFFCBD5E1),
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        letterSpacing = 1.5.sp
                    )
                }
            }
        }
    }
}
