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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.androidautonote.ui.detail.DetailScreen
import com.example.androidautonote.ui.detail.DetailViewModel
import com.example.androidautonote.ui.home.HomeScreen
import com.example.androidautonote.ui.home.HomeViewModel
import com.example.androidautonote.ui.recording.RecordingActivity
import com.example.androidautonote.ui.settings.SettingsScreen
import com.example.androidautonote.ui.theme.AndroidAutoNoteTheme

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
                        // Home screen — note list
                        composable("home") {
                            val viewModel = ViewModelProvider(
                                this@MainActivity,
                                HomeViewModel.Factory(app.noteRepository)
                            )[HomeViewModel::class.java]

                            HomeScreen(
                                viewModel = viewModel,
                                onNoteClick = { noteId ->
                                    navController.navigate("detail/$noteId")
                                },
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
                                }
                            )
                        }

                        // Detail screen — view/edit note
                        composable(
                            route = "detail/{noteId}",
                            arguments = listOf(
                                navArgument("noteId") { type = NavType.LongType }
                            )
                        ) { backStackEntry ->
                            val noteId = backStackEntry.arguments?.getLong("noteId") ?: 0L
                            val viewModel = ViewModelProvider(
                                this@MainActivity,
                                DetailViewModel.Factory(app.noteRepository, noteId)
                            )[DetailViewModel::class.java]

                            DetailScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
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