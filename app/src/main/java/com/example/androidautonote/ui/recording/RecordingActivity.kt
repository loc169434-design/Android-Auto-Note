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
import android.view.WindowManager
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Transparent Activity that displays a floating recording dialog.
 *
 * Key behavior changes:
 * - Mic auto-stops after 15s of no speech
 * - Auto-saves note when mic stops (no manual Save needed)
 * - No background running — stops service completely after save
 */
class RecordingActivity : ComponentActivity() {

    private var voiceService: VoiceRecordingService? = null
    private var isBound by mutableStateOf(false)
    private var hasSaved = false // Prevent double-save

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val service = (binder as VoiceRecordingService.LocalBinder).getService()
            voiceService = service
            isBound = true

            // Observe auto-save signal from service
            lifecycleScope.launch {
                service.autoSaveTriggered.collectLatest { triggered ->
                    if (triggered && !hasSaved) {
                        autoSaveNote()
                    }
                }
            }
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
        // Keep screen awake while recording
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

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
                    onCancel = { cancelRecording() },
                    onPause = {
                        val pauseIntent = Intent(this@RecordingActivity, VoiceRecordingService::class.java).apply {
                            action = VoiceRecordingService.ACTION_PAUSE
                        }
                        startService(pauseIntent)
                    },
                    onResume = {
                        val resumeIntent = Intent(this@RecordingActivity, VoiceRecordingService::class.java).apply {
                            action = VoiceRecordingService.ACTION_RESUME
                        }
                        startService(resumeIntent)
                    },
                    onManualStop = {
                        // User manually stops — auto-save
                        autoSaveNote()
                    }
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
        val serviceIntent = Intent(this, VoiceRecordingService::class.java).apply {
            action = VoiceRecordingService.ACTION_START
        }
        ContextCompat.startForegroundService(this, serviceIntent)

        bindService(
            Intent(this, VoiceRecordingService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    /**
     * Auto-save: called when mic auto-stops (idle timeout) or user manually stops.
     * No manual Save button needed.
     */
    private fun autoSaveNote() {
        if (hasSaved) return
        hasSaved = true

        val text = voiceService?.getFullText() ?: ""

        if (text.isBlank()) {
            // Nothing recorded — just close
            stopRecordingService()
            Toast.makeText(this, "Không có nội dung ghi âm", Toast.LENGTH_SHORT).show()
            finish()
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

            WidgetUpdater.updateAllWidgets(this@RecordingActivity)

            stopRecordingService()
            Toast.makeText(this@RecordingActivity, "✅ Đã tự động lưu ghi chú!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun cancelRecording() {
        voiceService?.clearText()
        stopRecordingService()
        finish()
    }

    private fun stopRecordingService() {
        val stopIntent = Intent(this, VoiceRecordingService::class.java).apply {
            action = VoiceRecordingService.ACTION_STOP
        }
        startService(stopIntent)

        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }

    override fun onDestroy() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
        super.onDestroy()
    }
}
