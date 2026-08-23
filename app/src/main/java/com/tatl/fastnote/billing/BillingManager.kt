package com.tatl.fastnote.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Manages Google Play Billing for the one-time "premium_lifetime" purchase.
 *
 * Product ID: "premium_lifetime"  (must match Play Console)
 * Price: 200,000₫ / lifetime
 */
class BillingManager(context: Context) {

    companion object {
        const val PRODUCT_ID = "premium_lifetime"
        private const val TAG = "BillingManager"
    }

    private var onPurchaseSuccess: (() -> Unit)? = null
    private var onPurchaseFailed: ((String) -> Unit)? = null

    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { handlePurchase(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User cancelled billing flow")
            }
            else -> {
                onPurchaseFailed?.invoke("Thanh toán thất bại (${result.responseCode})")
            }
        }
    }

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    // ── Connect ───────────────────────────────────────────────────────────────

    suspend fun connect(): Boolean = suspendCancellableCoroutine { cont ->
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                cont.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
            }
            override fun onBillingServiceDisconnected() {
                if (cont.isActive) cont.resume(false)
            }
        })
    }

    // ── Query product ─────────────────────────────────────────────────────────

    suspend fun queryProduct(): ProductDetails? = withContext(Dispatchers.IO) {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRODUCT_ID)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            ))
            .build()

        suspendCancellableCoroutine { cont ->
            billingClient.queryProductDetailsAsync(params) { result, details ->
                cont.resume(
                    if (result.responseCode == BillingClient.BillingResponseCode.OK)
                        details.firstOrNull()
                    else null
                )
            }
        }
    }

    // ── Launch flow ───────────────────────────────────────────────────────────

    fun launchBillingFlow(
        activity: Activity,
        productDetails: ProductDetails,
        onSuccess: () -> Unit,
        onFailed: (String) -> Unit
    ) {
        onPurchaseSuccess = onSuccess
        onPurchaseFailed  = onFailed

        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .build()
            ))
            .build()

        Log.d(TAG, "launchBillingFlow: billingClient.isReady=${billingClient.isReady}")
        val result = billingClient.launchBillingFlow(activity, params)
        Log.d(TAG, "launchBillingFlow result: responseCode=${result.responseCode} debugMsg=${result.debugMessage}")
        if (result.responseCode != com.android.billingclient.api.BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "❌ launchBillingFlow FAILED: ${result.responseCode} — ${result.debugMessage}")
            onFailed("Lỗi mở thanh toán: ${result.debugMessage} (${result.responseCode})")
        }
    }

    // ── Handle purchase ───────────────────────────────────────────────────────

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return

        val ackParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(ackParams) { result ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Purchase acknowledged")
                // TODO: in production, validate server-side via Firebase Cloud Functions
                MainScope().launch {
                    PremiumManager.setPremium(purchase.purchaseToken)
                    onPurchaseSuccess?.invoke()
                }
            }
        }
    }

    // ── Restore purchases ─────────────────────────────────────────────────────

    suspend fun queryExistingPurchases(): Boolean = withContext(Dispatchers.IO) {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        suspendCancellableCoroutine { cont ->
            billingClient.queryPurchasesAsync(params) { result, purchases ->
                cont.resume(
                    result.responseCode == BillingClient.BillingResponseCode.OK &&
                    purchases.any {
                        it.products.contains(PRODUCT_ID) &&
                        it.purchaseState == Purchase.PurchaseState.PURCHASED
                    }
                )
            }
        }
    }

    fun disconnect() = billingClient.endConnection()
}
