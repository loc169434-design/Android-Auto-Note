package com.tatl.fastnote.billing

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.launch

private val Black     = Color(0xFF000000)
private val White     = Color(0xFFFFFFFF)
private val TextGray  = Color(0xFFAAAAAA)
private val BoxBorder = Color(0xFF444444)

/**
 * Premium gate dialog — shown when trial expires or user taps Crown button.
 *
 * Shows:
 *  - Benefits box listing upgrade perks
 *  - Buy button → Google Play Billing flow
 *  - Restore link → queryExistingPurchases()
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Black)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // ── Benefits box ──────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = BoxBorder,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text = "QUYỀN LỢI KHI BẠN ĐỒNG Ý\nNÂNG CẤP:",
                            color = White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "1. Gửi tệp tin nén bảo mật mã hóa AES-256 sang máy tính cá nhân (Gửi PC);",
                            color = White,
                            fontSize = 14.sp,
                            lineHeight = 21.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "2. Tự động sao lưu và đồng bộ an toàn lên đám mây cá nhân Google Drive.",
                            color = White,
                            fontSize = 14.sp,
                            lineHeight = 21.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // ── Buy button ────────────────────────────────────────────
                val priceLabel = product?.oneTimePurchaseOfferDetails?.formattedPrice ?: "200k"

                if (isLoading) {
                    CircularProgressIndicator(color = White)
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
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = White),
                        shape  = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text       = "Nâng Cấp Ngay - $priceLabel/Trọn Đời",
                            color      = Black,
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // ── Status / error ────────────────────────────────────────
                if (statusText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text      = statusText,
                        color     = Color.Red,
                        fontSize  = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ── Restore link ──────────────────────────────────────────
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
                        text     = if (isRestoring) "Đang kiểm tra..." else "Bạn đã mua trước đó? Chạm để khôi phục.",
                        color    = TextGray,
                        fontSize = 12.sp
                    )
                }

                // ── Dismiss ───────────────────────────────────────────────
                TextButton(onClick = onDismiss) {
                    Text(
                        text     = "Để sau",
                        color    = TextGray.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
