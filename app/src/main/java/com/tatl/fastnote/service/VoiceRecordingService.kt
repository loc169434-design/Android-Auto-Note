package com.tatl.fastnote.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import com.tatl.fastnote.R
import com.tatl.fastnote.util.ThemePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Foreground Service that uses SpeechRecognizer to convert voice to text in real-time.
 *
 * Key features:
 * - Auto-restarts SpeechRecognizer on silence/errors
 * - Auto-stops after IDLE_TIMEOUT_SECONDS of no new speech
 * - Emits autoSaveTriggered when auto-stopping so Activity can save
 * - Does NOT run in background after auto-stop
 */
class VoiceRecordingService : Service() {

    companion object {
        private const val TAG = "VoiceRecordingService"
        private const val CHANNEL_ID = "voice_recording_channel"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START        = "ACTION_START"
        const val ACTION_STOP         = "ACTION_STOP"
        // Notification button: triggers autoSaveTriggered → Activity saves then stops
        const val ACTION_SAVE_AND_STOP = "ACTION_SAVE_AND_STOP"

        // Maximum continuous recording cap (10 minutes)
        private const val MAX_RECORDING_TIMEOUT_MS = 600_000L
    }

    // Binder for Activity to observe state
    inner class LocalBinder : Binder() {
        fun getService(): VoiceRecordingService = this@VoiceRecordingService
    }

    private val binder = LocalBinder()
    private var speechRecognizer: SpeechRecognizer? = null

    // --- Observable State ---
    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _recordingSeconds = MutableStateFlow(0)
    val recordingSeconds: StateFlow<Int> = _recordingSeconds.asStateFlow()

    /**
     * Emits true when mic auto-stops due to idle timeout.
     * The Activity observes this to trigger auto-save.
     */
    private val _autoSaveTriggered = MutableStateFlow(false)
    val autoSaveTriggered: StateFlow<Boolean> = _autoSaveTriggered.asStateFlow()

    // Accumulated text buffer (persists across SpeechRecognizer restarts)
    private val textBuffer = StringBuilder()

    // True after Activity saves — prevents double-save in onDestroy()
    private var isSaved = false

    /** Called by RecordingActivity after it successfully saves the note. */
    fun markAsSaved() { isSaved = true }

    // Timer for recording duration
    private var timerRunnable: Runnable? = null
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    // Flag to auto-restart listening after speech ends
    private var shouldKeepListening = false

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForegroundWithNotification()
                startListening()
                startTimer()
            }
            ACTION_STOP -> {
                stopListening()
                stopTimer()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            // Notification "Dừng & Lưu" → signal Activity to save, then stop
            ACTION_SAVE_AND_STOP -> {
                stopListening()
                stopTimer()
                _autoSaveTriggered.value = true
                // Activity observes this, saves, then calls ACTION_STOP
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        // Notification action: "Dừng & Lưu" → triggers auto-save via Activity
        val saveStopIntent = Intent(this, VoiceRecordingService::class.java).apply {
            action = ACTION_SAVE_AND_STOP
        }
        val saveStopPendingIntent = PendingIntent.getService(
            this, 0, saveStopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.recording_notification_title))
            .setContentText(getString(R.string.recording_notification_text))
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Dừng & Lưu",
                saveStopPendingIntent
            )
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.recording_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.recording_channel_desc)
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    /**
     * Auto-stop and signal the Activity to save.
     */
    private fun triggerAutoSave() {
        stopListening()
        stopTimer()
        _autoSaveTriggered.value = true
        // Don't stopSelf() here — let the Activity handle save then stop service
    }

    // --- Speech Recognition ---

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.e(TAG, "Speech recognition not available on this device")
            return
        }

        shouldKeepListening = true
        _isPaused.value = false
        _autoSaveTriggered.value = false

        speechRecognizer?.destroy()
        speechRecognizer = createSpeechRecognizerInstance().apply {
            setRecognitionListener(createRecognitionListener())
        }

        val recognizerIntent = createRecognizerIntent()
        speechRecognizer?.startListening(recognizerIntent)
        _isListening.value = true

        Log.d(TAG, "Started listening")
    }

    private fun createSpeechRecognizerInstance(): SpeechRecognizer {
        // Standard SpeechRecognizer uses high-precision cloud/hybrid model (instant & ultra-sensitive)
        Log.d(TAG, "Creating high-sensitivity SpeechRecognizer")
        return SpeechRecognizer.createSpeechRecognizer(this)
    }

    private fun createRecognizerIntent(): Intent {
        val langLocale = try {
            ThemePreferences.recognitionLanguage.value.locale
        } catch (e: Exception) {
            "vi-VN"
        }

        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langLocale)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // Instant sensitivity: process sentence after 1.5s of silence
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                1500L
            )
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                1500L
            )
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,
                1000L
            )
        }
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Ready for speech — listening active")
                _isListening.value = true
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Beginning of speech detected")
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d(TAG, "End of speech detected")
            }

            override fun onError(error: Int) {
                val errorMessage = getErrorMessage(error)
                Log.w(TAG, "Recognition error: $errorMessage (code: $error)")

                if (shouldKeepListening && !_isPaused.value) {
                    // Ultra-fast restart on silence timeout or no match to keep mic active without gap
                    when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                            handler.postDelayed({ restartListening() }, 50)
                        }
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                            handler.postDelayed({ restartListening() }, 300)
                        }
                        else -> {
                            handler.postDelayed({ restartListening() }, 500)
                        }
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spokenText = matches?.firstOrNull() ?: ""

                if (spokenText.isNotBlank()) {
                    if (textBuffer.isNotEmpty()) {
                        textBuffer.append(". ")
                    }
                    textBuffer.append(spokenText)
                    _recognizedText.value = textBuffer.toString()
                }
                _partialText.value = ""

                Log.d(TAG, "Final result: $spokenText")

                if (shouldKeepListening && !_isPaused.value) {
                    handler.postDelayed({ restartListening() }, 50)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches =
                    partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partial = matches?.firstOrNull() ?: ""
                _partialText.value = partial
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    private fun restartListening() {
        if (!shouldKeepListening || _isPaused.value) return

        try {
            speechRecognizer?.destroy()
            speechRecognizer = createSpeechRecognizerInstance().apply {
                setRecognitionListener(createRecognitionListener())
            }
            speechRecognizer?.startListening(createRecognizerIntent())
            _isListening.value = true
            Log.d(TAG, "Restarted listening")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restart listening", e)
        }
    }

    private fun stopListening() {
        shouldKeepListening = false
        _isListening.value = false
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping speech recognizer", e)
        }
    }

    private fun pauseListening() {
        _isPaused.value = true
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing speech recognizer", e)
        }
        _isListening.value = false
        stopTimer()
    }

    private fun resumeListening() {
        _isPaused.value = false
        startListening()
        startTimer()
    }

    fun getFullText(): String {
        val currentPartial = _partialText.value
        val fullText = if (currentPartial.isNotBlank()) {
            if (textBuffer.isNotEmpty()) {
                "${textBuffer}. $currentPartial"
            } else {
                currentPartial
            }
        } else {
            textBuffer.toString()
        }
        return fullText.trim()
    }

    fun clearText() {
        textBuffer.clear()
        _recognizedText.value = ""
        _partialText.value = ""
    }

    // --- Timer ---

    private fun startTimer() {
        timerRunnable = object : Runnable {
            override fun run() {
                if (!_isPaused.value) {
                    _recordingSeconds.value += 1
                    // 10 minutes max recording limit (600s)
                    if (_recordingSeconds.value >= 600) {
                        Log.d(TAG, "Reached 10-minute max recording limit — auto-saving")
                        triggerAutoSave()
                        return
                    }
                }
                handler.postDelayed(this, 1000)
            }
        }
        handler.postDelayed(timerRunnable!!, 1000)
    }

    private fun stopTimer() {
        timerRunnable?.let { handler.removeCallbacks(it) }
        timerRunnable = null
    }

    override fun onDestroy() {
        // Emergency save: handles phone-off / system-kill scenarios.
        // Only runs if Activity hasn't saved yet (isSaved = false).
        if (!isSaved) {
            val text = getFullText()
            if (text.isNotBlank()) {
                try {
                    emergencySaveToFiles(text)
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Emergency file save failed", e)
                }
            }
        }
        stopListening()
        stopTimer()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    /**
     * Synchronous file save — safe to call from onDestroy().
     * Writes to internal filesDir (no external storage permission needed,
     * always accessible even when external storage is unavailable).
     */
    private fun emergencySaveToFiles(text: String) {
        com.tatl.fastnote.util.FileHelper.appendNote(applicationContext, text)
        android.util.Log.d(TAG, "Emergency save via FileHelper")
    }

    private fun getErrorMessage(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
        SpeechRecognizer.ERROR_CLIENT -> "Client side error"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
        SpeechRecognizer.ERROR_NETWORK -> "Network error"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
        SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
        SpeechRecognizer.ERROR_SERVER -> "Server error"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
        else -> "Unknown error"
    }
}
