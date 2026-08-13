package com.tatl.fastnote.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.tatl.fastnote.MainActivity
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * OLED Pure Black onboarding screen.
 * Login options:
 *  1. Google Sign-In (legacy GoogleSignInClient — works on all devices/dev builds)
 *  2. Phone OTP (Firebase Phone Auth)
 *  3. Anonymous / Guest mode
 */
class OnboardingActivity : ComponentActivity() {

    companion object {
        private const val TAG = "OnboardingActivity"
        // Web Client ID from Firebase Console → Authentication → Sign-in method → Google
        private const val WEB_CLIENT_ID =
            "364981991189-vf79qsdjd922hvcf3lk1a93ms8m2kjaq.apps.googleusercontent.com"
    }

    private var verificationId: String? = null

    // ── Google Sign-In result launcher ────────────────────────────────────────
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken  = account.idToken
            if (idToken == null) {
                showToast("Google idToken is null — check OAuth config")
                return@registerForActivityResult
            }
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            lifecycleScope.launch {
                val user = AuthManager.signInWithCredential(credential)
                if (user != null) {
                    Log.d(TAG, "Firebase sign-in OK uid=${user.uid}")
                    goToMain()
                } else {
                    showToast("Đăng nhập Firebase thất bại")
                }
            }
        } catch (e: ApiException) {
            Log.e(TAG, "Google sign-in ApiException: ${e.statusCode}", e)
            showToast("Lỗi Google: ${e.statusCode} — ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Google sign-in failed", e)
            showToast("Lỗi: ${e.javaClass.simpleName}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (AuthManager.isLoggedIn()) { goToMain(); return }

        setContent {
            OnboardingScreen(
                onGoogleSignIn = { launchGoogleSignIn() },
                onSendOtp      = { phone -> sendPhoneOtp(phone) },
                onVerifyOtp    = { otp   -> verifyOtp(otp) },
                onGuestSignIn  = { launchGuestSignIn() }
            )
        }
    }

    // ── Google Sign-In (legacy — works without Play Console publish) ──────────

    private fun launchGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(this, gso)
        // Sign out first to force account picker (not auto-select)
        client.signOut().addOnCompleteListener {
            Log.d(TAG, "Launching Google Sign-In intent...")
            googleSignInLauncher.launch(client.signInIntent)
        }
    }

    // ── Anonymous / Guest Sign-In ─────────────────────────────────────────────

    private fun launchGuestSignIn() {
        Log.d(TAG, "Attempting anonymous sign-in...")
        lifecycleScope.launch {
            try {
                val ok = AuthManager.signInAnonymously()
                Log.d(TAG, "Anonymous result: $ok")
                if (ok) {
                    goToMain()
                } else {
                    showToast("Chế độ khách thất bại — bật Anonymous Auth trong Firebase Console")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Anonymous exception", e)
                showToast("Lỗi khách: ${e.javaClass.simpleName} — ${e.message?.take(60)}")
            }
        }
    }

    // ── Phone OTP ─────────────────────────────────────────────────────────────

    private fun sendPhoneOtp(phoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder(Firebase.auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    lifecycleScope.launch {
                        val user = AuthManager.signInWithCredential(credential)
                        if (user != null) goToMain()
                    }
                }
                override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                    Log.e(TAG, "OTP verification failed", e)
                    showToast("Gửi OTP thất bại: ${e.message}")
                }
                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                    verificationId = id
                    showToast("OTP đã gửi")
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun verifyOtp(otp: String) {
        val vid = verificationId ?: run { showToast("Gửi OTP trước"); return }
        val credential = PhoneAuthProvider.getCredential(vid, otp)
        lifecycleScope.launch {
            val user = AuthManager.signInWithCredential(credential)
            if (user != null) goToMain() else showToast("Mã OTP không đúng")
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showToast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}

// ── Composable UI ─────────────────────────────────────────────────────────────

private val OledBlack   = Color(0xFF000000)
private val TextWhite   = Color(0xFFFFFFFF)
private val TextGray    = Color(0xFF888888)
private val AccentGreen = Color(0xFF4CAF50)

@Composable
private fun OnboardingScreen(
    onGoogleSignIn: () -> Unit,
    onSendOtp:      (String) -> Unit,
    onVerifyOtp:    (String) -> Unit,
    onGuestSignIn:  () -> Unit
) {
    var showPhoneMode by remember { mutableStateOf(false) }
    var phone   by remember { mutableStateOf("") }
    var otp     by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── Intro ───────────────────────────────────────────────────────────
            Text("Chào bạn,", color = TextWhite, fontSize = 28.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Đây là một cuốn sổ phẳng \"biết điều\" — nơi dòng suy nghĩ của bạn được số hóa tức thời mà không vấp phải bất kỳ rào cản nào.",
                color = TextGray, fontSize = 15.sp, textAlign = TextAlign.Center, lineHeight = 22.sp
            )
            Spacer(Modifier.height(8.dp))
            Text("Chạm để nói. Thoát là lưu.", color = AccentGreen, fontSize = 14.sp,
                fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)

            Spacer(Modifier.height(60.dp))

            if (!showPhoneMode) {
                // ── Google button ─────────────────────────────────────────────
                Button(
                    onClick = onGoogleSignIn,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A))
                ) {
                    Text("🔵  BẮT ĐẦU VỚI GMAIL", color = TextWhite, fontSize = 15.sp)
                }
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = { showPhoneMode = true }) {
                    Text("Dùng số điện thoại", color = TextGray, fontSize = 14.sp)
                }
            } else {
                // ── Phone OTP ─────────────────────────────────────────────────
                if (!otpSent) {
                    OutlinedTextField(
                        value = phone, onValueChange = { phone = it },
                        label = { Text("Số điện thoại (+84...)", color = TextGray) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite, unfocusedTextColor = TextWhite,
                            focusedBorderColor = AccentGreen, unfocusedBorderColor = TextGray,
                            cursorColor = AccentGreen
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { if (phone.isNotBlank()) { onSendOtp(phone); otpSent = true } },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                    ) { Text("GỬI MÃ OTP", color = TextWhite) }
                } else {
                    OutlinedTextField(
                        value = otp, onValueChange = { otp = it },
                        label = { Text("Nhập mã OTP", color = TextGray) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite, unfocusedTextColor = TextWhite,
                            focusedBorderColor = AccentGreen, unfocusedBorderColor = TextGray,
                            cursorColor = AccentGreen
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { if (otp.isNotBlank()) onVerifyOtp(otp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                    ) { Text("XÁC NHẬN", color = TextWhite) }
                }
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { showPhoneMode = false; otpSent = false; phone = ""; otp = "" }) {
                    Text("← Quay lại", color = TextGray, fontSize = 13.sp)
                }
            }

            // ── Guest mode ────────────────────────────────────────────────────
            Spacer(Modifier.height(40.dp))
            TextButton(onClick = onGuestSignIn) {
                Text("Bỏ qua, vào chế độ khách →", color = TextGray.copy(alpha = 0.6f), fontSize = 12.sp)
            }
            Text(
                "(Dữ liệu không được đồng bộ — chỉ lưu trên thiết bị)",
                color = TextGray.copy(alpha = 0.35f), fontSize = 10.sp, textAlign = TextAlign.Center
            )
        }
    }
}
