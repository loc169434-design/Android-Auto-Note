package com.tatl.fastnote.billing

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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

private val BgDark    = Color(0xFF0D1F0F)
private val TextMain  = Color(0xFFECF5EE)
private val TextMuted = Color(0xFF7FAB8A)
private val Gold      = Color(0xFFFFD54F)
private val Green     = Color(0xFF4CAF50)

/**
 * Premium gate dialog — shown when trial expires or user taps Crown button.
 *
 * Shows:
 *  - Price and pitch
 *  - Buy button → Google Play Billing flow
 *  - Restore link → queryExistingPurchases()
 */
@Composable
fun PremiumGateDialog(
    onDismiss:       () -> Unit,
    onPremiumGranted: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope    = rememberCoroutineScope()

    var billing      by remember { mutableStateOf<BillingManager?>(null) }
    var product      by remember { mutableStateOf<ProductDetails?>(null) }
    var isLoading    by remember { mutableStateOf(true) }
    var statusText   by remember { mutableStateOf("") }
    var isRestoring  by remember { mutableStateOf(false) }

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
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(BgDark, shape = RoundedCornerShape(20.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Crown
            Text("👑", fontSize = 44.sp)
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Nâng cấp Premium",
                color = Gold,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Mở khóa toàn bộ tính năng mãi mãi.\nKhông thuê bao. Một lần dùng trọn đời.",
                color = TextMuted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(20.dp))

            // Features list
            listOf(
                "🎙 Micro ghi chú không giới hạn",
                "🧠 Cầu nối AI đính kèm file",
                "☁️ Đồng bộ đám mây tự động",
                "🔒 Không quảng cáo, không theo dõi"
            ).forEach { feature ->
                Text(
                    text = feature,
                    color = TextMain,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Price
            val priceText = product?.oneTimePurchaseOfferDetails?.formattedPrice ?: "200.000₫"
            Text(
                text = priceText,
                color = Gold,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(text = "thanh toán một lần • dùng trọn đời", color = TextMuted, fontSize = 12.sp)

            Spacer(modifier = Modifier.height(20.dp))

            if (isLoading) {
                CircularProgressIndicator(color = Green)
            } else {
                // Buy button
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
                            activity = activity,
                            productDetails = pd,
                            onSuccess = {
                                statusText = ""
                                onPremiumGranted()
                            },
                            onFailed = { msg -> statusText = msg }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Green),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("MUA NGAY — $priceText", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            if (statusText.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(statusText, color = Color.Red, fontSize = 12.sp, textAlign = TextAlign.Center)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Restore link
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
                    text = if (isRestoring) "Đang kiểm tra..." else "Bạn đã mua trước đó? Chạm để khôi phục.",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }

            // Close
            TextButton(onClick = onDismiss) {
                Text("Để sau", color = TextMuted.copy(alpha = 0.5f), fontSize = 11.sp)
            }
        }
    }
}
