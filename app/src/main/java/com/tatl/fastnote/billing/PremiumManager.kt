package com.tatl.fastnote.billing

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.tatl.fastnote.auth.AuthManager
import kotlinx.coroutines.tasks.await

/**
 * Manages Premium status via Firebase Firestore & local SharedPreferences cache.
 *
 * Firestore path: users/{uid}/premium/status
 *   isPremium: Boolean
 *   purchaseToken: String?
 *   purchasedAt: Timestamp?
 */
object PremiumManager {

    private const val TAG = "PremiumManager"
    private const val PREFS_NAME = "premium_prefs"
    private const val KEY_IS_PREMIUM = "is_premium_cached"

    // ── Read premium status ───────────────────────────────────────────────────

    /**
     * Fetch premium status from local cache or Firestore.
     */
    suspend fun isPremium(context: Context? = null): Boolean {
        if (context != null) {
            val localCached = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_IS_PREMIUM, false)
            if (localCached) return true
        }
        val uid = AuthManager.uid ?: return false
        return try {
            val doc = Firebase.firestore
                .collection("users").document(uid)
                .collection("premium").document("status")
                .get().await()
            val isPrem = doc.getBoolean("isPremium") == true
            if (isPrem && context != null) {
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit().putBoolean(KEY_IS_PREMIUM, true).apply()
            }
            isPrem
        } catch (e: Exception) {
            Log.w(TAG, "isPremium check failed: ${e.message}")
            false
        }
    }

    /**
     * Full gate check: user is premium OR trial hasn't expired.
     * This is the main check to use before showing locked features.
     */
    suspend fun hasAccess(context: Context): Boolean {
        if (!TrialManager.isTrialExpired(context)) return true
        return isPremium(context)
    }

    // ── Write premium status (called after billing validation) ─────────────────

    /**
     * Mark user as premium in local cache and Firestore.
     */
    suspend fun setPremium(purchaseToken: String? = null, context: Context? = null) {
        if (context != null) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_IS_PREMIUM, true).apply()
        }
        val uid = AuthManager.uid ?: return
        try {
            val data = mutableMapOf<String, Any>(
                "isPremium" to true,
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            if (purchaseToken != null) data["purchaseToken"] = purchaseToken

            Firebase.firestore
                .collection("users").document(uid)
                .collection("premium").document("status")
                .set(data, SetOptions.merge())
                .await()
            Log.d(TAG, "Premium activated for $uid")
        } catch (e: Exception) {
            Log.e(TAG, "setPremium failed: ${e.message}")
        }
    }
}
