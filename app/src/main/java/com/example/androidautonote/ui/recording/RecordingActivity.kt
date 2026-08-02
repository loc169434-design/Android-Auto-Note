package com.example.androidautonote.ui.recording

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.androidautonote.AutoNoteApplication
import com.example.androidautonote.service.VoiceRecordingService
import com.example.androidautonote.ui.theme.AndroidAutoNoteTheme
import com.example.androidautonote.widget.WidgetUpdater
import kotlinx.coroutines.launch

/**
 * Transparent Activity that displays a floating recording dialog.
 * Launched from Widget via PendingIntent.
 *
 * - Starts as a transparent overlay with background dimming
 * - Binds to VoiceRecordingService to control recording
 * - Dismisses like a dialog when user taps outside or presses back
 */
class RecordingActivity : ComponentActivity() {

    private var voiceService: VoiceRecordingService? = null
    private var isBound by mutableStateOf(false)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            voiceService = (binder as VoiceRecordingService.LocalBinder).getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            voiceService = null
            isBound = false
        }
    }

    // Permission launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        if (audioGranted) {
            startRecordingService()
        } else {
            Toast.makeText(
                this,
                "Cần quyền ghi âm để sử dụng tính năng này",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check and request permissions first
        if (hasRequiredPermissions()) {
            startRecordingService()
        } else {
            requestPermissions()
        }

        setContent {
            AndroidAutoNoteTheme {
                RecordingScreen(
                    service = voiceService,
                    isBound = isBound,
                    onSave = { text -> saveNote(text) },
                    onCancel = { cancelRecording() }
                )
            }
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        val audioPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        return audioPermission && notificationPermission
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun startRecordingService() {
        // Start the Foreground Service
        val serviceIntent = Intent(this, VoiceRecordingService::class.java).apply {
            action = VoiceRecordingService.ACTION_START
        }
        ContextCompat.startForegroundService(this, serviceIntent)

        // Bind to the service to observe state
        bindService(
            Intent(this, VoiceRecordingService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    private fun saveNote(text: String) {
        if (text.isBlank()) {
            Toast.makeText(this, "Chưa có nội dung để lưu", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            // Auto-generate title from first 10 words
            val title = text.split(" ")
                .take(10)
                .joinToString(" ")
                .let { if (it.length > 50) it.take(50) + "..." else it }

            val app = application as AutoNoteApplication
            app.noteRepository.insertNote(title = title, content = text)

            // Update all widgets to reflect new note
            WidgetUpdater.updateAllWidgets(this@RecordingActivity)

            stopRecordingService()
            Toast.makeText(this@RecordingActivity, "Đã lưu ghi chú!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun cancelRecording() {
        voiceService?.clearText()
        stopRecordingService()
        finish()
    }

    private fun stopRecordingService() {
        // Stop the Foreground Service
        val stopIntent = Intent(this, VoiceRecordingService::class.java).apply {
            action = VoiceRecordingService.ACTION_STOP
        }
        startService(stopIntent)

        // Unbind
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }

    override fun onDestroy() {
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
        super.onDestroy()
    }
}
