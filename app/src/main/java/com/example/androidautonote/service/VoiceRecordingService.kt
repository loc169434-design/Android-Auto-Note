package com.example.androidautonote.service

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
import com.example.androidautonote.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Foreground Service that uses SpeechRecognizer to convert voice to text in real-time.
 *
 * Key design decisions for Android 14/15 compliance:
 * - Uses foregroundServiceType="microphone" (declared in manifest)
 * - Started from a visible Activity (RecordingActivity) to avoid background start restrictions
 * - Auto-restarts SpeechRecognizer on silence/errors to prevent premature cutoff
 * - Accumulates partial results across restart cycles in a text buffer
 */
class VoiceRecordingService : Service() {

    companion object {
        private const val TAG = "VoiceRecordingService"
        private const val CHANNEL_ID = "voice_recording_channel"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
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

    // Accumulated text buffer (persists across SpeechRecognizer restarts)
    private val textBuffer = StringBuilder()

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
            ACTION_PAUSE -> {
                pauseListening()
            }
            ACTION_RESUME -> {
                resumeListening()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ requires specifying foreground service type at runtime
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
        val stopIntent = Intent(this, VoiceRecordingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.recording_notification_title))
            .setContentText(getString(R.string.recording_notification_text))
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_media_pause,
                getString(R.string.btn_stop_recording),
                stopPendingIntent
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

    // --- Speech Recognition ---

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.e(TAG, "Speech recognition not available on this device")
            return
        }

        shouldKeepListening = true
        _isPaused.value = false

        // Destroy previous instance before creating new one
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(createRecognitionListener())
        }

        val recognizerIntent = createRecognizerIntent()
        speechRecognizer?.startListening(recognizerIntent)
        _isListening.value = true

        Log.d(TAG, "Started listening")
    }

    private fun createRecognizerIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN") // Vietnamese
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // Extended silence timeout to reduce premature cutoffs
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                5000L
            )
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                5000L
            )
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,
                30000L
            )
        }
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Ready for speech")
                _isListening.value = true
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Beginning of speech")
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Could be used for audio level visualization
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d(TAG, "End of speech detected")
            }

            override fun onError(error: Int) {
                val errorMessage = getErrorMessage(error)
                Log.w(TAG, "Recognition error: $errorMessage (code: $error)")

                // Auto-restart on recoverable errors (silence, no match, etc.)
                if (shouldKeepListening && !_isPaused.value) {
                    when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                        SpeechRecognizer.ERROR_CLIENT -> {
                            // Small delay before restart to avoid rapid cycling
                            handler.postDelayed({ restartListening() }, 300)
                        }
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                            handler.postDelayed({ restartListening() }, 1000)
                        }
                        else -> {
                            // For fatal errors (network, server, etc.), try restart with delay
                            handler.postDelayed({ restartListening() }, 2000)
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

                // Auto-restart to keep listening (the key trick for continuous recording!)
                if (shouldKeepListening && !_isPaused.value) {
                    handler.postDelayed({ restartListening() }, 200)
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

    /**
     * Restarts the SpeechRecognizer for continuous listening.
     * This is the core mechanism to prevent the "auto-stop on silence" problem.
     */
    private fun restartListening() {
        if (!shouldKeepListening || _isPaused.value) return

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
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

    /**
     * Returns all accumulated text and clears the buffer.
     */
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
     * Clears the text buffer and resets all state.
     */
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
        stopListening()
        stopTimer()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
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
