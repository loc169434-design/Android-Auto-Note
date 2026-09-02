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
        const val ACTION_SAVE_AND_STOP = "ACTION_SAVE_AND_STOP"

        private const val MAX_RECORDING_TIMEOUT_MS = 600_000L
    }

    inner class LocalBinder : Binder() {
        fun getService(): VoiceRecordingService = this@VoiceRecordingService
    }

    private val binder = LocalBinder()
    private var speechRecognizer: SpeechRecognizer? = null

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

    private val _autoSaveTriggered = MutableStateFlow(false)
    val autoSaveTriggered: StateFlow<Boolean> = _autoSaveTriggered.asStateFlow()

    private val textBuffer = StringBuilder()
    private var isSaved = false

    fun markAsSaved() { isSaved = true }

    private var timerRunnable: Runnable? = null
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
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
            ACTION_SAVE_AND_STOP -> {
                stopListening()
                stopTimer()
                _autoSaveTriggered.value = true
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
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
            .addAction(android.R.drawable.ic_media_pause, "Dung & Luu", saveStopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.recording_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = getString(R.string.recording_channel_desc) }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun triggerAutoSave() {
        stopListening()
        stopTimer()
        _autoSaveTriggered.value = true
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
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(createRecognitionListener())
        }

        val recognizerIntent = createRecognizerIntent()
        speechRecognizer?.startListening(recognizerIntent)
        _isListening.value = true

        Log.d(TAG, "Started listening")
    }

    private fun createSpeechRecognizerInstance(): SpeechRecognizer {
        Log.d(TAG, "Creating high-sensitivity SpeechRecognizer")
        return SpeechRecognizer.createSpeechRecognizer(this)
    }

    private fun createRecognizerIntent(): Intent {
        val langLocale = try {
            com.tatl.fastnote.data.user.LanguageManager.getSpeechLanguageTag()
        } catch (e: Exception) {
            "vi-VN"
        }

        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langLocale)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000L)
            // Android 13+: Google tu them dau cau, viet hoa dau cau, dinh dang so
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                putExtra(
                    RecognizerIntent.EXTRA_ENABLE_FORMATTING,
                    RecognizerIntent.FORMATTING_OPTIMIZE_QUALITY
                )
            }
        }
    }

    /**
     * Format van ban thanh cau co nghia:
     * - Viet hoa chu dau tien
     * - Them dau cham neu chua co dau cau cuoi
     * Dung cho Android < 13 (Android 13+ da co EXTRA_ENABLE_FORMATTING)
     */
    private fun formatSentence(text: String): String {
        if (text.isBlank()) return text
        val trimmed = text.trim()
        // Viet hoa chu dau tien
        val capitalized = trimmed.replaceFirstChar { it.uppercaseChar() }
        // Them dau cham neu chua co dau cau ket thuc
        val endPunctuations = setOf('.', '!', '?', '…')
        return if (capitalized.last() in endPunctuations) capitalized
               else "$capitalized."
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
                val raw = matches?.firstOrNull() ?: ""

                // Android 13+ da co EXTRA_ENABLE_FORMATTING nen khong can xu ly them
                // Android < 13: tu format de tao cau co nghia
                val spokenText = if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ) raw else formatSentence(raw)

                if (spokenText.isNotBlank()) {
                    if (textBuffer.isNotEmpty()) {
                        textBuffer.append(" ")
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
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
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

    fun updateLanguageAndRestart() {
        if (_isListening.value || shouldKeepListening) {
            restartListening()
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

    /**
     * Commit any pending partial result into the main buffer.
     * Call this before saving to ensure mid-speech text is not lost.
     */
    fun flushPartialToBuffer() {
        val partial = _partialText.value.trim()
        if (partial.isNotBlank()) {
            if (textBuffer.isNotEmpty()) textBuffer.append(" ")
            textBuffer.append(partial)
            _recognizedText.value = textBuffer.toString()
            _partialText.value = ""
        }
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
        if (!isSaved) {
            val isPrem = com.tatl.fastnote.billing.PremiumManager.isPremiumCached(applicationContext)
            val isExpired = com.tatl.fastnote.billing.TrialManager.isTrialExpired(applicationContext)
            if (isPrem || !isExpired) {
                val text = getFullText()
                if (text.isNotBlank()) {
                    try {
                        emergencySaveToFiles(text)
                    } catch (e: Exception) {
                        android.util.Log.e(TAG, "Emergency file save failed", e)
                    }
                }
            }
        }
        stopListening()
        stopTimer()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun emergencySaveToFiles(text: String) {
        com.tatl.fastnote.util.FileHelper.appendNote(applicationContext, text)
        android.util.Log.d(TAG, "Emergency save via FileHelper")
    }

    private fun getErrorMessage(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO                    -> "Audio recording error"
        SpeechRecognizer.ERROR_CLIENT                   -> "Client side error"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
        SpeechRecognizer.ERROR_NETWORK                  -> "Network error"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT          -> "Network timeout"
        SpeechRecognizer.ERROR_NO_MATCH                 -> "No match found"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY          -> "Recognizer busy"
        SpeechRecognizer.ERROR_SERVER                   -> "Server error"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT           -> "Speech timeout"
        else                                            -> "Unknown error"
    }
}