package com.tatl.fastnote

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
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
import com.tatl.fastnote.util.PinWidgetHelper
import com.tatl.fastnote.util.ThemePreferences
import com.tatl.fastnote.widget.TripleActionWidgetReceiver
import kotlinx.coroutines.launch
import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.tatl.fastnote.data.user.LanguageManager
import java.util.Locale

import android.content.Context
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.getLocalizedContext(newBase))
    }

    companion object {
        private const val TAG = "MainActivity"
    }

    private var pendingActionAfterPremium: (() -> Unit)? = null

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            Log.d(TAG, "Google sign-in OK: ${account.email}")
            com.tatl.fastnote.data.user.UserManager.updateProfileFromGoogle(account)
            lifecycleScope.launch {
                val ok = com.tatl.fastnote.sync.GoogleDriveSyncManager.sync(applicationContext)
                if (ok) {
                }
                kotlinx.coroutines.delay(600L)
                // Chỉ khi đã đồng bộ Google Drive hoàn tất thì mới mở popup chờ (Send PC)
                pendingActionAfterPremium?.invoke()
                pendingActionAfterPremium = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google sign-in failed", e)
            val code = if (e is ApiException) " (Mã: ${e.statusCode})" else if (e.cause is ApiException) " (Mã: ${(e.cause as ApiException).statusCode})" else ""
            pendingActionAfterPremium = null
        }
    }

    fun launchGoogleSignInForDrive() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(com.tatl.fastnote.sync.GoogleDriveSyncManager.DRIVE_APPDATA_SCOPE)
            .build()
        val client = GoogleSignIn.getClient(this, gso)
        client.signOut().addOnCompleteListener {
            googleSignInLauncher.launch(client.signInIntent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val fromWidgetNote = intent.getBooleanExtra("FROM_WIDGET_NOTE", false) ||
                             intent.action == "com.tatl.fastnote.ACTION_VIEW_NOTES"
        val showTrialExpired = intent.getBooleanExtra("SHOW_TRIAL_EXPIRED", false)
        if (showTrialExpired) {
            // Chỉ hiện toast nếu chưa mua Premium
            val isPrem = com.tatl.fastnote.billing.PremiumManager.isPremiumCached(this)
            if (!isPrem) showTrialExpiredToast()
        }
        if (!fromWidgetNote && !showTrialExpired) {
            val isWidgetActive = PinWidgetHelper.isWidgetActive(this, TripleActionWidgetReceiver::class.java)
            if (!isWidgetActive && ThemePreferences.hasPinnedWidget.value) {
                ThemePreferences.setWidgetPinned(false)
            }
            if (isWidgetActive && ThemePreferences.hasPinnedWidget.value) {
                val recordIntent = Intent(this, RecordingActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                startActivity(recordIntent)
                finishAffinity()
                finishAndRemoveTask()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // ── Kiểm tra điều hướng: Mở từ App Icon (Launcher) vs Mở từ Nút Sổ trên Widget ──
        val fromWidgetNote = intent?.getBooleanExtra("FROM_WIDGET_NOTE", false) == true ||
                             intent?.action == "com.tatl.fastnote.ACTION_VIEW_NOTES"
        val showTrialExpired = intent?.getBooleanExtra("SHOW_TRIAL_EXPIRED", false) == true

        // Nếu mở từ App Icon: Nếu đã có widget đang active -> Biến hình App Icon thành Mic nói
        // Nếu chưa có widget -> Vào Home để hiện màn hình mời tạo Widget
        if (!fromWidgetNote && !showTrialExpired) {
            val isWidgetActive = PinWidgetHelper.isWidgetActive(this, TripleActionWidgetReceiver::class.java)
            if (!isWidgetActive && ThemePreferences.hasPinnedWidget.value) {
                ThemePreferences.setWidgetPinned(false)
            }
            if (isWidgetActive && ThemePreferences.hasPinnedWidget.value) {
                val recordIntent = Intent(this, RecordingActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                startActivity(recordIntent)
                finishAffinity()
                finishAndRemoveTask()
                return
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

        }

        // Từ widget callback khi trial hết hạn
        if (intent?.getBooleanExtra("SHOW_TRIAL_EXPIRED", false) == true) {
            // Chỉ hiện toast nếu chưa mua Premium
            val isPrem = com.tatl.fastnote.billing.PremiumManager.isPremiumCached(this)
            if (!isPrem) showTrialExpiredToast()
        }

        // ── Cloud sync: chỉ chạy khi isPremium VÀ đã đăng nhập Google thật ─────
        lifecycleScope.launch {
            val isGoogleUser = AuthManager.isLoggedIn() && !AuthManager.isAnonymous
            val isPremiumUser = com.tatl.fastnote.billing.PremiumManager.isPremium(applicationContext)
            if (isGoogleUser && isPremiumUser) {
                com.tatl.fastnote.sync.GoogleDriveSyncManager.sync(applicationContext)
                CloudSyncManager.syncFromCloud(applicationContext)
            }
        }

        val app = application as AutoNoteApplication

        setContent {
            AndroidAutoNoteTheme {
                // ── Locale-aware context: cap nhat ngay lap tuc khi doi ngon ngu ──
                val currentLanguage by LanguageManager.currentLanguage.collectAsState()
                val baseCtx = LocalContext.current
                val localizedContext = remember(currentLanguage) {
                    val config = Configuration(baseCtx.resources.configuration)
                    val locale = Locale.forLanguageTag(currentLanguage.code)
                    config.setLocale(locale)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        config.setLocales(android.os.LocaleList(locale))
                    }
                    baseCtx.createConfigurationContext(config)
                }
                CompositionLocalProvider(
                    LocalContext provides localizedContext,
                    androidx.compose.ui.platform.LocalConfiguration provides localizedContext.resources.configuration
                ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF000000)  // den tuyet doi, khong ghi de edge-to-edge
                ) {
                    val navController = rememberNavController()
                    var showPremiumDialog  by remember {
                        mutableStateOf(
                            intent?.getBooleanExtra("SHOW_PREMIUM_DIALOG", false) == true ||
                            intent?.getBooleanExtra("SHOW_TRIAL_EXPIRED", false) == true
                        )
                    }
                    var isPremiumUser      by remember { mutableStateOf(false) }
                    var showSendPcDialog  by remember { mutableStateOf(false) }

                    // ── Tự động đóng dialog khi app xuống background / chuyển tab ────────
                    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
                        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                                showPremiumDialog = false
                                showSendPcDialog = false
                                pendingActionAfterPremium = null
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                        }
                    }

                    LaunchedEffect(Unit) {
                        isPremiumUser = com.tatl.fastnote.billing.PremiumManager.isPremium(this@MainActivity)
                    }

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
                                        // Share AI luôn cho phép dù hết hạn trial
                                        val todayText = viewModel.getTodayNotesText()
                                        AIShareHelper.launchAIShare(this@MainActivity, todayText)
                                    }
                                },
                                onPremiumClick = {
                                    pendingActionAfterPremium = null
                                    showPremiumDialog = true
                                },
                                // Nut Gui PC: Bat buoc phai la Premium (khong bo qua bang trial de test thanh toan)
                                onComputerClick = {
                                    lifecycleScope.launch {
                                        val isPrem = isPremiumUser || com.tatl.fastnote.billing.PremiumManager.isPremium(this@MainActivity)
                                        if (isPrem) {
                                            val googleAccount = GoogleSignIn.getLastSignedInAccount(this@MainActivity)
                                            if (googleAccount == null) {
                                                pendingActionAfterPremium = { showSendPcDialog = true }
                                                launchGoogleSignInForDrive()
                                            } else {
                                                showSendPcDialog = true
                                            }
                                        } else {
                                            pendingActionAfterPremium = { showSendPcDialog = true }
                                            showPremiumDialog = true
                                        }
                                    }
                                },
                                isPremium = isPremiumUser
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                isPremium = isPremiumUser,
                                onUpgradeClick = {
                                    pendingActionAfterPremium = null
                                    showPremiumDialog = true
                                },
                                onRestoreClick = {
                                    pendingActionAfterPremium = null
                                    showPremiumDialog = true
                                },
                                onLoginClick = {
                                    launchGoogleSignInForDrive()
                                },
                                onSyncClick = {
                                    val googleAccount = GoogleSignIn.getLastSignedInAccount(this@MainActivity)
                                    if (googleAccount == null) {
                                        launchGoogleSignInForDrive()
                                    } else {
                                        lifecycleScope.launch {
                                            val ok = com.tatl.fastnote.sync.GoogleDriveSyncManager.sync(applicationContext)
                                            if (ok) {
                                            }
                                        }
                                    }
                                },
                                onLogoutClick = {
                                    AuthManager.signOut()
                                    GoogleSignIn.getClient(
                                        this@MainActivity,
                                        GoogleSignInOptions.DEFAULT_SIGN_IN
                                    ).signOut()
                                    com.tatl.fastnote.data.user.UserManager.init(this@MainActivity)
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
                            activity = this@MainActivity,
                            onDismiss = {
                                showPremiumDialog = false
                                pendingActionAfterPremium = null
                            },
                            onPremiumGranted = {
                                isPremiumUser = true
                                showPremiumDialog = false
                                lifecycleScope.launch {
                                    com.tatl.fastnote.billing.PremiumManager.setPremium(context = this@MainActivity)
                                    val googleAccount = GoogleSignIn.getLastSignedInAccount(this@MainActivity)
                                    if (googleAccount == null) {
                                        launchGoogleSignInForDrive()
                                    } else {
                                        com.tatl.fastnote.sync.GoogleDriveSyncManager.sync(applicationContext)
                                        com.tatl.fastnote.sync.CloudSyncManager.syncFromCloud(applicationContext)
                                        pendingActionAfterPremium?.invoke()
                                        pendingActionAfterPremium = null
                                    }
                                }
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
                                    }
                                }
                            }
                        )
                    }
                } // end Surface
                } // end CompositionLocalProvider
            }
        }
    }

    private fun showTrialExpiredToast() {
        // Dùng localizedContext để toast hiển thị đúng ngôn ngữ app đang chọn
        val localizedCtx = com.tatl.fastnote.data.user.LanguageManager.getLocalizedContext(this)
    }
}