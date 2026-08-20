package com.tatl.fastnote.ui.recording

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.tatl.fastnote.AutoNoteApplication
import com.tatl.fastnote.service.VoiceRecordingService
import com.tatl.fastnote.ui.theme.AndroidAutoNoteTheme
import com.tatl.fastnote.util.FileHelper
import com.tatl.fastnote.widget.WidgetUpdater
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Transparent Activity that displays a floating recording dialog.
 *
 * Save policy:
 *  - Any stop other than "Hủy" → auto-save to Room DB + 2 txt files
 *  - "Hủy" (Cancel) button → discard (no save)
 *  - Phone off / system kill → VoiceRecordingService.onDestroy() saves to files
 *
 * Two txt files are written on each save:
 *  1. note_{timestamp}.txt    — raw single-session file
 *  2. fileguidi.txt           — cumulative backup (append, never erased)
 */
class RecordingActivity : ComponentActivity() {

    private var voiceService: VoiceRecordingService? = null
    private var isBound by mutableStateOf(false)
    private var hasSaved = false    // Guard: prevent double-save
    private var showSavedToast by mutableStateOf(false)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val service = (binder as VoiceRecordingService.LocalBinder).getService()
            voiceService = service
            isBound = true

            // Observe auto-save signal from service (notification stop, timer, etc.)
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
                    showSavedToast = showSavedToast,
                    onCancel = { cancelRecording() },
                    onSaveAndExit = { autoSaveNote() }
                )
            }
        }

        // Back button -> auto save (khong discard)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                autoSaveNote()
            }
        })
    }

    /**
     * When the Activity is stopped (user locks screen via power button,
     * switches apps, etc.) → auto-save so no data is lost.
     */
    override fun onStop() {
        super.onStop()
        if (!hasSaved) {
            autoSaveNote()
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        val audioOk = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val notifOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        return audioOk && notifOk
    }

    private fun requestPermissions() {
        val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(perms.toTypedArray())
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

    // ── Save logic ───────────────────────────────────────────────────────────

    /**
     * Auto-save to:
     *  1. Room database (for in-app display)
     *  2. note_{timestamp}.txt — raw single-session file
     *  3. fileguidi.txt        — cumulative backup file (appended, never erased)
     */
    private fun autoSaveNote() {
        if (hasSaved) return
        hasSaved = true

        val text = voiceService?.getFullText() ?: ""

        // Mark service so emergency save in onDestroy is skipped
        voiceService?.markAsSaved()

        if (text.isBlank()) {
            stopRecordingService()
            Toast.makeText(this, "Không có nội dung ghi âm", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Save to files using FileHelper (AndroidAutoNote folder)
        FileHelper.appendNote(this, text)
        val dir = FileHelper.getNotesDir(this)
        Log.d("RecordingActivity", "Files saved in: ${dir.absolutePath}")

        lifecycleScope.launch {
            val title = text.split(" ")
                .take(10)
                .joinToString(" ")
                .let { if (it.length > 50) it.take(50) + "..." else it }

            val app = application as AutoNoteApplication
            app.noteRepository.insertNote(title = title, content = text)

            WidgetUpdater.updateAllWidgets(this@RecordingActivity)

            // Sync to Google Drive appDataFolder in background
            com.tatl.fastnote.sync.GoogleDriveSyncManager.sync(applicationContext)

            stopRecordingService()

            // Hien thi custom toast roi finish sau 1.4s
            showSavedToast = true
            kotlinx.coroutines.delay(1400)
            finish()
        }
    }



    private fun cancelRecording() {
        hasSaved = true     // prevent onStop from auto-saving
        voiceService?.clearText()
        voiceService?.markAsSaved()
        stopRecordingService()
        finish()
    }

    // ── Service lifecycle ────────────────────────────────────────────────────

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
