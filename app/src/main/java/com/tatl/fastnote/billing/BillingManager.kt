package com.tatl.fastnote.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages Google Play Billing for one-time (non-consumable) premium purchase.
 *
 * For PoC, purchase state is verified locally via BillingClient.
 * Production should add server-side verification via Google Play Developer API.
 */
class BillingManager(private val context: Context) {

    companion object {
        private const val TAG = "BillingManager"
        // TODO: Replace with actual product ID from Google Play Console
        const val PRODUCT_ID_PREMIUM = "premium_unlock"
    }

    private var billingClient: BillingClient? = null
    private var productDetails: ProductDetails? = null

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _billingState = MutableStateFlow(BillingState.DISCONNECTED)
    val billingState: StateFlow<BillingState> = _billingState.asStateFlow()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    handlePurchase(purchase)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User cancelled purchase")
            }
            else -> {
                Log.e(TAG, "Purchase error: ${billingResult.debugMessage}")
            }
        }
    }

    fun initialize() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(
                com.android.billingclient.api.PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()

        connectToPlayBilling()
    }

    private fun connectToPlayBilling() {
        _billingState.value = BillingState.CONNECTING

        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _billingState.value = BillingState.CONNECTED
                    Log.d(TAG, "Billing connected")
                    queryProductDetails()
                    queryExistingPurchases()
                } else {
                    _billingState.value = BillingState.ERROR
                    Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                _billingState.value = BillingState.DISCONNECTED
                Log.w(TAG, "Billing disconnected")
            }
        })
    }

    private fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID_PREMIUM)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetails = productDetailsList.firstOrNull()
                Log.d(TAG, "Product details loaded: ${productDetails?.name}")
            } else {
                Log.e(TAG, "Failed to query product details: ${billingResult.debugMessage}")
            }
        }
    }

    /**
     * Check if user has already purchased premium (restore purchases).
     */
    fun queryExistingPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient?.queryPurchasesAsync(params) { billingResult, purchasesList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasPremium = purchasesList.any { purchase ->
                    purchase.products.contains(PRODUCT_ID_PREMIUM) &&
                            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                _isPremium.value = hasPremium

                // Acknowledge any unacknowledged purchases
                purchasesList.forEach { purchase ->
                    if (!purchase.isAcknowledged &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                    ) {
                        acknowledgePurchase(purchase)
                    }
                }

                Log.d(TAG, "Existing purchases checked. Premium: $hasPremium")
            }
        }
    }

    /**
     * Launch the purchase flow for premium.
     */
    fun launchPurchaseFlow(activity: Activity): Boolean {
        val details = productDetails
        if (details == null) {
            Log.e(TAG, "Product details not available yet")
            return false
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val billingResult = billingClient?.launchBillingFlow(activity, billingFlowParams)
        return billingResult?.responseCode == BillingClient.BillingResponseCode.OK
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (purchase.products.contains(PRODUCT_ID_PREMIUM)) {
                _isPremium.value = true
            }

            // Must acknowledge within 3 days or Google will refund
            if (!purchase.isAcknowledged) {
                acknowledgePurchase(purchase)
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient?.acknowledgePurchase(params) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Purchase acknowledged")
            } else {
                Log.e(TAG, "Acknowledge failed: ${billingResult.debugMessage}")
            }
        }
    }

    fun destroy() {
        billingClient?.endConnection()
        billingClient = null
    }

    /**
     * Get the formatted price string for display.
     */
    fun getPriceString(): String? {
        return productDetails
            ?.oneTimePurchaseOfferDetails
            ?.formattedPrice
    }

    enum class BillingState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        ERROR
    }
}
