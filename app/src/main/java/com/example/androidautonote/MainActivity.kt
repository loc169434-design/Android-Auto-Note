package com.example.androidautonote

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.androidautonote.ui.home.HomeScreen
import com.example.androidautonote.ui.home.HomeViewModel
import com.example.androidautonote.ui.recording.RecordingActivity
import com.example.androidautonote.ui.settings.SettingsScreen
import com.example.androidautonote.ui.theme.AndroidAutoNoteTheme
import com.example.androidautonote.util.AIShareHelper
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as AutoNoteApplication

        setContent {
            AndroidAutoNoteTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        // Home screen — single-file diary timeline
                        composable("home") {
                            val viewModel = ViewModelProvider(
                                this@MainActivity,
                                HomeViewModel.Factory(app.noteRepository)
                            )[HomeViewModel::class.java]

                            HomeScreen(
                                viewModel = viewModel,
                                onRecordClick = {
                                    startActivity(
                                        Intent(
                                            this@MainActivity,
                                            RecordingActivity::class.java
                                        )
                                    )
                                },
                                onSettingsClick = {
                                    navController.navigate("settings")
                                },
                                onAIShareClick = {
                                    // Export today's notes and share with AI
                                    lifecycleScope.launch {
                                        val todayText = viewModel.getTodayNotesText()
                                        AIShareHelper.launchAIShare(
                                            this@MainActivity,
                                            todayText
                                        )
                                    }
                                }
                            )
                        }

                        // Settings screen
                        composable("settings") {
                            SettingsScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}