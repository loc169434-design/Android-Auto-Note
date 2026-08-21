package com.tatl.fastnote

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tatl.fastnote.auth.AuthManager
import com.tatl.fastnote.auth.OnboardingActivity
import com.tatl.fastnote.billing.PremiumGateDialog
import com.tatl.fastnote.billing.PremiumManager
import com.tatl.fastnote.billing.TrialManager
import com.tatl.fastnote.sync.CloudSyncManager
import com.tatl.fastnote.ui.home.HomeScreen
import com.tatl.fastnote.ui.home.HomeViewModel
import com.tatl.fastnote.ui.recording.RecordingActivity
import com.tatl.fastnote.ui.settings.SettingsScreen
import com.tatl.fastnote.ui.theme.AndroidAutoNoteTheme
import com.tatl.fastnote.util.AIShareHelper
import com.tatl.fastnote.util.SendPcDialog
import com.tatl.fastnote.util.SendPcHelper
import com.tatl.fastnote.util.SendPcPrefs
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.animation.doOnEnd
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Exit animation: icon thu nho + fade out truoc khi vao app
        splashScreen.setOnExitAnimationListener { splashViewProvider ->
            val iconView  = splashViewProvider.iconView
            val rootView  = splashViewProvider.view

            // Scale icon xuong 60%
            val scaleX = android.animation.ObjectAnimator
                .ofFloat(iconView, android.view.View.SCALE_X, 1f, 0.6f)
                .apply { duration = 350 }
            val scaleY = android.animation.ObjectAnimator
                .ofFloat(iconView, android.view.View.SCALE_Y, 1f, 0.6f)
                .apply { duration = 350 }

            // Fade out toan bo splash
            val fade = android.animation.ObjectAnimator
                .ofFloat(rootView, android.view.View.ALPHA, 1f, 0f)
                .apply {
                    duration = 400
                    startDelay = 80
                    doOnEnd { splashViewProvider.remove() }
                }

            android.animation.AnimatorSet().apply {
                playTogether(scaleX, scaleY, fade)
                start()
            }
        }

        // Edge-to-edge: status bar đen, icon trắng; nav bar đen, icon trắng
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        // Đảm bảo nội dung vẽ sau system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // ── Không ép đăng nhập khi khởi động — người dùng vào thẳng app ──────────

        // ── Trial: record first launch + show countdown toast ─────────────────
        TrialManager.initFirstLaunch(applicationContext)
        if (TrialManager.shouldShowCountdown(applicationContext)) {
            Toast.makeText(
                this,
                TrialManager.getCountdownMessage(applicationContext),
                Toast.LENGTH_LONG
            ).show()
        }

        // Từ widget callback khi trial hết hạn
        if (intent?.getBooleanExtra("SHOW_TRIAL_EXPIRED", false) == true) {
            showTrialExpiredToast()
        }

        // ── Cloud sync: chỉ chạy khi isPremium VÀ đã đăng nhập Google thật ─────
        lifecycleScope.launch {
            val isGoogleUser = AuthManager.isLoggedIn() && !AuthManager.isAnonymous
            val isPremiumUser = com.tatl.fastnote.billing.PremiumManager.isPremium()
            if (isGoogleUser && isPremiumUser) {
                com.tatl.fastnote.sync.GoogleDriveSyncManager.sync(applicationContext)
                CloudSyncManager.syncFromCloud(applicationContext)
            }
        }

        val app = application as AutoNoteApplication

        setContent {
            AndroidAutoNoteTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF000000)  // đen tuyệt đối, không ghi đè edge-to-edge
                ) {
                    val navController = rememberNavController()
                    var showPremiumDialog  by remember { mutableStateOf(false) }
                    var isPremiumUser      by remember { mutableStateOf(false) }
                    var showSendPcDialog  by remember { mutableStateOf(false) }

                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        composable("home") {
                            val viewModel = ViewModelProvider(
                                this@MainActivity,
                                HomeViewModel.Factory(app.noteRepository)
                            )[HomeViewModel::class.java]

                            // Bam back 2 lan de thoat app
                            var lastBackMs by remember { mutableStateOf(0L) }
                            BackHandler {
                                val now = System.currentTimeMillis()
                                if (now - lastBackMs < 2000L) {
                                    finish()
                                } else {
                                    lastBackMs = now
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Bấm lại để thoát",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                            HomeScreen(
                                viewModel = viewModel,
                                onRecordClick = {
                                    startActivity(Intent(this@MainActivity, RecordingActivity::class.java))
                                },
                                onSettingsClick = {
                                    navController.navigate("settings")
                                },
                                onAIShareClick = {
                                    lifecycleScope.launch {
                                        val todayText = viewModel.getTodayNotesText()
                                        AIShareHelper.launchAIShare(this@MainActivity, todayText)
                                    }
                                },
                                onPremiumClick = {
                                    showPremiumDialog = true
                                },
                                // Nut Gui PC: moi lan gui la nhap mat khau lai luon
                                onComputerClick = {
                                    showSendPcDialog = true
                                },
                                isPremium = isPremiumUser
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                isPremium = isPremiumUser,
                                onUpgradeClick = { showPremiumDialog = true },
                                onLoginClick = {
                                    startActivity(Intent(this@MainActivity, OnboardingActivity::class.java))
                                },
                                onLogoutClick = {
                                    AuthManager.signOut()
                                    startActivity(Intent(this@MainActivity, OnboardingActivity::class.java))
                                    finish()
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }

                    // ── Premium gate dialog ───────────────────────────────────
                    AnimatedVisibility(
                        visible = showPremiumDialog,
                        enter = slideInVertically(tween(320)) { it / 2 } + fadeIn(tween(280)),
                        exit  = slideOutVertically(tween(240)) { it / 2 } + fadeOut(tween(200))
                    ) {
                        PremiumGateDialog(
                            onDismiss = { showPremiumDialog = false },
                            onPremiumGranted = {
                                isPremiumUser = true
                                showPremiumDialog = false
                                Toast.makeText(
                                    this@MainActivity,
                                    "Cảm ơn bạn. Các đặc quyền Premium đã được mở thành công!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }


                    // ── Send PC dialog ────────────────────────────────────
                    AnimatedVisibility(
                        visible = showSendPcDialog,
                        enter = slideInVertically(tween(320)) { it / 2 } + fadeIn(tween(280)),
                        exit  = slideOutVertically(tween(240)) { it / 2 } + fadeOut(tween(200))
                    ) {
                        SendPcDialog(
                            onDismiss = { showSendPcDialog = false },
                            onConfirm = { pwd ->
                                // Luu mat khau lan dau de khong hoi lai
                                SendPcPrefs.savePassword(this@MainActivity, pwd)
                                showSendPcDialog = false
                                lifecycleScope.launch {
                                    val err = SendPcHelper.zipAndShare(
                                        this@MainActivity, pwd
                                    )
                                    if (err != null) {
                                        Toast.makeText(
                                            this@MainActivity,
                                            err,
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra("SHOW_TRIAL_EXPIRED", false)) {
            showTrialExpiredToast()
        }
    }

    private fun showTrialExpiredToast() {
        Toast.makeText(
            this,
            "Cuốn sổ của bạn đã đồng hành cùng bạn 30 ngày. " +
            "Hãy nâng cấp để tiếp tục lưu giữ những khoảnh khắc tiếp theo nhé!",
            Toast.LENGTH_LONG
        ).show()
    }
}