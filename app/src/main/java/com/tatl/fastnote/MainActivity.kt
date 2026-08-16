package com.tatl.fastnote

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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

        // ── Auth gate: redirect to Onboarding if not logged in ─────────────────
        if (!AuthManager.isLoggedIn()) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        // ── Trial: record first launch + show countdown toast ─────────────────
        TrialManager.initFirstLaunch(applicationContext)
        if (TrialManager.shouldShowCountdown(applicationContext)) {
            Toast.makeText(
                this,
                TrialManager.getCountdownMessage(applicationContext),
                Toast.LENGTH_LONG
            ).show()
        }

        // ── Cloud sync: Google Drive (appDataFolder) & Firebase (background) ──
        lifecycleScope.launch {
            com.tatl.fastnote.sync.GoogleDriveSyncManager.sync(applicationContext)
            CloudSyncManager.syncFromCloud(applicationContext)
        }

        val app = application as AutoNoteApplication

        setContent {
            AndroidAutoNoteTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF000000)  // đen tuyệt đối, không ghi đè edge-to-edge
                ) {
                    val navController = rememberNavController()
                    var showPremiumDialog by remember { mutableStateOf(false) }
                    var isPremiumUser    by remember { mutableStateOf(false) }

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

                    // Premium gate dialog
                    if (showPremiumDialog) {
                        PremiumGateDialog(
                            onDismiss = { showPremiumDialog = false },
                            onPremiumGranted = {
                                isPremiumUser = true
                                showPremiumDialog = false
                                Toast.makeText(
                                    this@MainActivity,
                                    "🎉 Cảm ơn! Đặc quyền Premium đã được kích hoạt!",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                    }
                }
            }
        }
    }
}