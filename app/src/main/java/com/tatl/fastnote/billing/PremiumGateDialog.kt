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
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import com.android.billingclient.api.ProductDetails
import com.tatl.fastnote.R
import com.tatl.fastnote.data.user.LanguageManager
import com.tatl.fastnote.ui.theme.InterFontFamily
import com.tatl.fastnote.ui.theme.NotoSansFontFamily
import kotlinx.coroutines.launch
import java.util.Locale

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
    val baseCtx = LocalContext.current
    val activity = baseCtx as? Activity
    val scope    = rememberCoroutineScope()

    // ── Locale-aware context ──
    val currentLanguage by LanguageManager.currentLanguage.collectAsState()
    val localizedCtx = remember(currentLanguage) {
        val config = android.content.res.Configuration(baseCtx.resources.configuration)
        config.setLocale(Locale(currentLanguage.code))
        baseCtx.createConfigurationContext(config)
    }

    var billing     by remember { mutableStateOf<BillingManager?>(null) }
    var product     by remember { mutableStateOf<ProductDetails?>(null) }
    var isLoading   by remember { mutableStateOf(true) }
    var statusText  by remember { mutableStateOf("") }
    var isRestoring by remember { mutableStateOf(false) }

    // Error strings: load trực tiếp từ localizedCtx.resources để đảm bảo đúng ngôn ngữ
    // (không thể dùng stringResource() ở đây vì nằm ngoài CompositionLocalProvider)
    val strProductErr = localizedCtx.resources.getString(R.string.str_premium_product_err)
    val strNoPurchase = localizedCtx.resources.getString(R.string.str_premium_no_purchase)

    // Connect to billing on first compose
    LaunchedEffect(Unit) {
        android.util.Log.d("PremiumDialog", "=== LaunchedEffect start ===")
        val bm = BillingManager(baseCtx)
        billing = bm
        android.util.Log.d("PremiumDialog", "Connecting to BillingClient...")
        val connected = bm.connect()
        android.util.Log.d("PremiumDialog", "BillingClient connected = $connected")
        if (connected) {
            android.util.Log.d("PremiumDialog", "Querying product: ${BillingManager.PRODUCT_ID}")
            val pd = bm.queryProduct()
            product = pd
            android.util.Log.d("PremiumDialog", "Product result = $pd")
            if (pd == null) {
                android.util.Log.w("PremiumDialog", "⚠️ Product is NULL — check Play Console: product ID=${BillingManager.PRODUCT_ID}, app not published to tester?")
            } else {
                android.util.Log.d("PremiumDialog", "✅ Product found: ${pd.name} — price=${pd.oneTimePurchaseOfferDetails?.formattedPrice}")
            }
        } else {
            android.util.Log.e("PremiumDialog", "❌ BillingClient connection FAILED")
        }
        isLoading = false
        android.util.Log.d("PremiumDialog", "=== LaunchedEffect done: product=$product, isLoading=$isLoading ===")
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(LocalContext provides localizedCtx) {
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
                        text = stringResource(R.string.str_premium_title),
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
                                    text = stringResource(R.string.str_premium_feat_send_pc),
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
                                    text = stringResource(R.string.str_premium_feat_drive),
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
                                android.util.Log.d("PremiumDialog", "--- Buy button clicked ---")
                                android.util.Log.d("PremiumDialog", "billing=$billing, product=$product, activity=$activity")
                                val bm = billing
                                if (bm == null) {
                                    android.util.Log.e("PremiumDialog", "❌ billing is NULL")
                                    return@Button
                                }
                                val pd = product
                                if (pd == null) {
                                    android.util.Log.e("PremiumDialog", "❌ product is NULL (Google Play not synced or not uploaded to Internal Testing)")
                                    if (com.tatl.fastnote.BuildConfig.DEBUG) {
                                        // ⚡ Chế độ Debug: Kích hoạt Premium giả lập ngay để test luồng app mà không phải chờ Google Play đồng bộ
                                        scope.launch {
                                            PremiumManager.setPremium("DEBUG_MOCK_TOKEN")
                                            statusText = ""
                                            onPremiumGranted()
                                        }
                                        return@Button
                                    }
                                    statusText = strProductErr
                                    return@Button
                                }
                                if (activity == null) {
                                    android.util.Log.e("PremiumDialog", "❌ activity is NULL (context is not Activity?)")
                                    return@Button
                                }
                                android.util.Log.d("PremiumDialog", "✅ Calling launchBillingFlow with product=${pd.productId}")
                                bm.launchBillingFlow(
                                    activity = activity,
                                    productDetails = pd,
                                    onSuccess = {
                                        android.util.Log.d("PremiumDialog", "✅ Purchase SUCCESS")
                                        statusText = ""; onPremiumGranted()
                                    },
                                    onFailed = { msg ->
                                        android.util.Log.e("PremiumDialog", "❌ Purchase FAILED: $msg")
                                        statusText = msg
                                    }
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
                                text = stringResource(R.string.str_premium_upgrade_btn, priceLabel),
                                color = Color.White,
                                fontSize = 15.sp,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // ── Status / error ────────────────────────────────────────────
                    if (statusText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = statusText,
                            color = Color(0xFFFF5252),
                            fontSize = 13.sp,
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
                                        statusText = strNoPurchase
                                    }
                                }
                                isRestoring = false
                            }
                        }
                    ) {
                        Text(
                            text = if (isRestoring) stringResource(R.string.str_premium_checking) else stringResource(
                                R.string.str_premium_restore_hint
                            ),
                            color = Color(0xFF60A5FA),
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
                            text = stringResource(R.string.str_premium_later),
                            color = Color(0xFFCBD5E1),
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            letterSpacing = 1.5.sp
                        )
                    }
                }
            } // end CompositionLocalProvider
        }
    }
}
