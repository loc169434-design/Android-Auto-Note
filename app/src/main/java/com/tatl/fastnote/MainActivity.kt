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
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                    var showComputerGate  by remember { mutableStateOf(false) }
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
                                // Nút Gửi PC: chặn nếu chưa premium → mở dialog nâng cấp
                                onComputerClick = {
                                    if (isPremiumUser) {
                                        showSendPcDialog = true
                                    } else {
                                        showComputerGate = true
                                    }
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
                    if (showPremiumDialog) {
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

                    // ── Computer gate dialog (chim mồi) ──────────────────────
                    if (showComputerGate) {
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = { showComputerGate = false },
                            containerColor = androidx.compose.ui.graphics.Color(0xFF111111),
                            title = {
                                androidx.compose.material3.Text(
                                    "Tính năng Premium",
                                    color = androidx.compose.ui.graphics.Color.White,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                            },
                            text = {
                                androidx.compose.material3.Text(
                                    "Hãy nâng cấp lên Premium để sử dụng tính năng xuất file bảo mật này.",
                                    color = androidx.compose.ui.graphics.Color(0xFFAAAAAA),
                                    fontSize = 14.sp
                                )
                            },
                            confirmButton = {
                                androidx.compose.material3.Button(
                                    onClick = {
                                        showComputerGate = false
                                        showPremiumDialog = true
                                    },
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = androidx.compose.ui.graphics.Color.White
                                    ),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                                ) {
                                    androidx.compose.material3.Text(
                                        "Nâng Cấp Ngay",
                                        color = androidx.compose.ui.graphics.Color.Black,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                }
                            },
                            dismissButton = {
                                androidx.compose.material3.TextButton(onClick = { showComputerGate = false }) {
                                    androidx.compose.material3.Text(
                                        "Để sau",
                                        color = androidx.compose.ui.graphics.Color(0xFF666666)
                                    )
                                }
                            }
                        )
                    }

                    // ── Send PC dialog ───────────────────────────────
                    if (showSendPcDialog) {
                        SendPcDialog(
                            onDismiss = { showSendPcDialog = false },
                            onConfirm = { pwd ->
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