package com.tatl.fastnote.data.user

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseUser
import com.tatl.fastnote.auth.AuthManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

object UserManager {
    private const val PREFS_NAME = "auto_note_user_prefs"
    private const val KEY_GUEST_ID = "guest_id"

    private lateinit var prefs: SharedPreferences

    private val _userProfile = MutableStateFlow(
        UserProfile(
            userId = "GUEST-000000",
            userName = "Khách",
            email = "",
            isLoggedIn = false,
            accountType = AccountType.GUEST
        )
    )
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val guestId = getOrCreateGuestId()
        
        val googleAccount = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)
        if (googleAccount != null) {
            updateProfileFromGoogle(googleAccount)
        } else {
            updateProfile(AuthManager.currentUser, guestId)
        }

        CoroutineScope(Dispatchers.Main).launch {
            AuthManager.authStateFlow.collect { firebaseUser ->
                if (firebaseUser != null && !firebaseUser.isAnonymous) {
                    updateProfile(firebaseUser, getOrCreateGuestId())
                } else {
                    val gAcc = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)
                    if (gAcc != null) {
                        updateProfileFromGoogle(gAcc)
                    } else {
                        updateProfile(firebaseUser, getOrCreateGuestId())
                    }
                }
            }
        }
    }

    fun updateProfileFromGoogle(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount) {
        _userProfile.value = UserProfile(
            userId = account.id ?: account.email ?: "GOOGLE-USER",
            userName = account.displayName ?: account.email?.substringBefore("@") ?: "Google User",
            email = account.email ?: "",
            avatarUrl = account.photoUrl?.toString(),
            isLoggedIn = true,
            accountType = AccountType.GOOGLE
        )
    }

    private fun getOrCreateGuestId(): String {
        var id = prefs.getString(KEY_GUEST_ID, null)
        if (id.isNullOrEmpty()) {
            val randomSegment = UUID.randomUUID().toString().take(6).uppercase()
            id = "GUEST-$randomSegment"
            prefs.edit().putString(KEY_GUEST_ID, id).apply()
        }
        return id
    }

    private fun updateProfile(firebaseUser: FirebaseUser?, guestId: String) {
        if (firebaseUser != null && !firebaseUser.isAnonymous) {
            _userProfile.value = UserProfile(
                userId = firebaseUser.uid,
                userName = firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@") ?: "Google User",
                email = firebaseUser.email ?: "",
                avatarUrl = firebaseUser.photoUrl?.toString(),
                isLoggedIn = true,
                accountType = AccountType.GOOGLE
            )
        } else {
            _userProfile.value = UserProfile(
                userId = guestId,
                userName = "Guest User",
                email = "",
                avatarUrl = null,
                isLoggedIn = false,
                accountType = AccountType.GUEST
            )
        }
    }
}
