package com.tatl.fastnote.billing

import android.util.Log
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.tatl.fastnote.auth.AuthManager
import kotlinx.coroutines.tasks.await

/**
 * Manages Premium status via Firebase Firestore.
 *
 * Firestore path: users/{uid}/premium/status
 *   isPremium: Boolean
 *   purchaseToken: String?
 *   purchasedAt: Timestamp?
 *
 * Source of truth is Firestore (server-side), not local SharedPreferences,
 * to prevent Lucky Patcher / APK tampering.
 */
object PremiumManager {

    private const val TAG = "PremiumManager"

    // ── Read premium status ───────────────────────────────────────────────────

    /**
     * Fetch premium status from Firestore.
     * Returns false if user is not logged in or Firestore fails.
     */
    suspend fun isPremium(): Boolean {
        val uid = AuthManager.uid ?: return false
        return try {
            val doc = Firebase.firestore
                .collection("users").document(uid)
                .collection("premium").document("status")
                .get().await()
            doc.getBoolean("isPremium") == true
        } catch (e: Exception) {
            Log.w(TAG, "isPremium check failed: ${e.message}")
            false
        }
    }

    /**
     * Full gate check: user is premium OR trial hasn't expired.
     * This is the main check to use before showing locked features.
     */
    suspend fun hasAccess(context: android.content.Context): Boolean {
        if (!TrialManager.isTrialExpired(context)) return true
        return isPremium()
    }

    // ── Write premium status (called after billing validation) ─────────────────

    /**
     * Mark user as premium in Firestore.
     * In production this should be done server-side via Cloud Functions.
     * This client-side write is a fallback for development/testing.
     */
    suspend fun setPremium(purchaseToken: String? = null) {
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
