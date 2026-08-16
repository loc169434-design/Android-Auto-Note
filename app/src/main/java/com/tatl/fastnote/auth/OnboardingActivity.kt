package com.tatl.fastnote.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontStyle
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
import com.tatl.fastnote.ui.theme.AppAccentGreen
import com.tatl.fastnote.ui.theme.AppBgBlack
import com.tatl.fastnote.ui.theme.AppBorder
import com.tatl.fastnote.ui.theme.AppTextMuted
import com.tatl.fastnote.ui.theme.AppTextPrimary
import com.tatl.fastnote.ui.theme.AppTextSecondary
import com.tatl.fastnote.ui.theme.InterFontFamily
import com.tatl.fastnote.ui.theme.NotoSansFontFamily
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * OLED Pure Black onboarding screen — designed to match screenshot mockup.
 * Login options:
 *  1. Google Sign-In
 *  2. Phone OTP (Firebase Phone Auth)
 *  3. Anonymous / Guest mode
 */
class OnboardingActivity : ComponentActivity() {

    companion object {
        private const val TAG = "OnboardingActivity"
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

    // ── Google Sign-In ────────────────────────────────────────────────────────

    private fun launchGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .requestScopes(com.tatl.fastnote.sync.GoogleDriveSyncManager.DRIVE_APPDATA_SCOPE)
            .build()
        val client = GoogleSignIn.getClient(this, gso)
        client.signOut().addOnCompleteListener {
            Log.d(TAG, "Launching Google Sign-In intent with Drive appdata scope...")
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

// ══════════════════════════════════════════════════════════════════════════════
//  Composable UI — uses shared AppColors + AppFonts
// ══════════════════════════════════════════════════════════════════════════════

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
            .background(AppBgBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // ── Intro Text ───────────────────────────────────────────────────
            Spacer(Modifier.weight(1f))

            Text(
                text = "Chào bạn,",
                fontFamily = NotoSansFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 18.sp,
                color = AppTextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Đây là một cuốn sổ phẳng 'biết điều' – nơi\ndòng suy nghĩ của bạn được số hóa tức thời\nmà không vấp phải bất kỳ rào cản nào.",
                fontFamily = NotoSansFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                lineHeight = 24.sp,
                color = AppTextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = "Chạm để nói. Thoát là lưu.",
                fontFamily = NotoSansFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                color = AppTextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Việc của bạn là trải nghiệm sự tiện ích.",
                fontFamily = NotoSansFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                color = AppTextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = "Hãy để chúng tôi làm người thư ký mẫn cán,\nâm thầm lưu vết hành trình cuộc sống của\nbạn từ những cái chạm bản năng nhất.",
                fontFamily = NotoSansFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                lineHeight = 24.sp,
                color = AppTextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.weight(1f))

            // ── Auth buttons ─────────────────────────────────────────────────
            if (!showPhoneMode) {
                // Google button
                OutlinedButton(
                    onClick = onGoogleSignIn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AppBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = AppTextSecondary
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Google "G" icon using text
                        Text(
                            text = "G",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = AppTextSecondary,
                            modifier = Modifier.width(32.dp),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Tiếp tục với Google",
                            fontFamily = NotoSansFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 15.sp,
                            color = AppTextSecondary
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Phone button
                OutlinedButton(
                    onClick = { showPhoneMode = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AppBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = AppTextSecondary
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp).then(Modifier.width(32.dp)),
                            tint = AppTextSecondary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Tiếp tục với Số điện thoại",
                            fontFamily = NotoSansFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 15.sp,
                            color = AppTextSecondary
                        )
                    }
                }

            } else {
                // ── Phone OTP mode ───────────────────────────────────────────
                if (!otpSent) {
                    OutlinedTextField(
                        value = phone, onValueChange = { phone = it },
                        label = { Text("Số điện thoại (+84...)", color = AppTextMuted,
                            fontFamily = NotoSansFontFamily, fontSize = 13.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AppTextPrimary,
                            unfocusedTextColor = AppTextPrimary,
                            focusedBorderColor = AppTextSecondary,
                            unfocusedBorderColor = AppBorder,
                            cursorColor = AppTextPrimary
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = NotoSansFontFamily,
                            fontSize = 15.sp,
                            color = AppTextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { if (phone.isNotBlank()) { onSendOtp(phone); otpSent = true } },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AppBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = AppTextSecondary
                        )
                    ) {
                        Text("GỬI MÃ OTP", fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium, fontSize = 14.sp,
                            letterSpacing = 1.sp, color = AppTextSecondary)
                    }
                } else {
                    OutlinedTextField(
                        value = otp, onValueChange = { otp = it },
                        label = { Text("Nhập mã OTP", color = AppTextMuted,
                            fontFamily = NotoSansFontFamily, fontSize = 13.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AppTextPrimary,
                            unfocusedTextColor = AppTextPrimary,
                            focusedBorderColor = AppTextSecondary,
                            unfocusedBorderColor = AppBorder,
                            cursorColor = AppTextPrimary
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = NotoSansFontFamily,
                            fontSize = 15.sp,
                            color = AppTextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { if (otp.isNotBlank()) onVerifyOtp(otp) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AppBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = AppTextSecondary
                        )
                    ) {
                        Text("XÁC NHẬN", fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium, fontSize = 14.sp,
                            letterSpacing = 1.sp, color = AppTextSecondary)
                    }
                }
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { showPhoneMode = false; otpSent = false; phone = ""; otp = "" }) {
                    Text("← Quay lại", fontFamily = NotoSansFontFamily,
                        color = AppTextMuted, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(40.dp))

            // ── Guest / skip section ─────────────────────────────────────────
            Text(
                text = "BẮT ĐẦU TRẢI NGHIỆM",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                color = AppTextMuted,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onGuestSignIn,
                modifier = Modifier.height(44.dp),
                shape = RoundedCornerShape(50.dp),
                border = BorderStroke(1.dp, AppBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = AppTextMuted
                )
            ) {
                Text(
                    text = "BẮT ĐẦU TRẢI NGHIỆM",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    letterSpacing = 1.5.sp,
                    color = AppTextMuted
                )
            }

            Spacer(Modifier.height(36.dp))
        }
    }
}
