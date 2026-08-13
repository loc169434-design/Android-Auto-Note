package com.tatl.fastnote.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Singleton wrapper around FirebaseAuth.
 * Exposes auth state as a Flow so UI can react to login/logout.
 */
object AuthManager {

    private val auth: FirebaseAuth get() = Firebase.auth

    /** Currently signed-in Firebase user, or null if not logged in. */
    val currentUser: FirebaseUser? get() = auth.currentUser

    val uid: String? get() = auth.currentUser?.uid

    fun isLoggedIn(): Boolean = auth.currentUser != null

    /** Flow that emits whenever auth state changes (login / logout). */
    val authStateFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { fa ->
            trySend(fa.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    fun signOut() {
        auth.signOut()
    }

    /** True if current user is an anonymous (guest) user. */
    val isAnonymous: Boolean get() = auth.currentUser?.isAnonymous == true

    /** Sign in anonymously — creates a guest UID that works with Firestore. */
    suspend fun signInAnonymously(): Boolean {
        return try {
            auth.signInAnonymously().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Sign in with a Firebase credential (Google or Phone). */
    suspend fun signInWithCredential(credential: com.google.firebase.auth.AuthCredential): FirebaseUser? {
        return try {
            val result = auth.signInWithCredential(credential).await()
            result.user
        } catch (e: Exception) {
            null
        }
    }
}
