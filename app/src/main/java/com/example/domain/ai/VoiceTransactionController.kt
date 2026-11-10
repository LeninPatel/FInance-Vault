package com.example.domain.ai

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Immutable
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import kotlin.math.ceil

@Immutable
data class VoiceAssistantUiState(
    val isActivelyListening: Boolean = false,
    val isProcessingCommand: Boolean = false,
    val isSpeakingReply: Boolean = false,
    val remainingSilenceSeconds: Int = 4,
    val partialTranscript: String = "",
    val lastSpokenTranscript: String = "",
    val lastGeminiReply: String = "",
    val lastError: String? = null
) {
    val isBusyOrActive: Boolean
        get() = isActivelyListening || isProcessingCommand || isSpeakingReply
}

/**
 * Battery-friendly on-demand Voice Transaction Controller with a 4-second (4,000 ms)
 * pre-speech and post-speech grace timer and instant all-state Mute/Cancel support.
 *
 * - Waits 4 seconds before speech begins and 4 seconds after speech pauses before auto-closing.
 * - Tapping the microphone/mute button while listening, processing, or speaking immediately
 *   stops the microphone, cancels command processing, and mutes any spoken reply.
 */
class VoiceTransactionController(
    private val context: Context,
    private val onUtteranceCaptured: (String) -> Unit,
    private val onCancelActiveCommand: () -> Unit = {}
) {
    companion object {
        const val SILENCE_AUTO_CLOSE_DELAY_MS = 4000L
        private const val WATCHDOG_TICK_MS = 200L
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    private val _state = MutableStateFlow(VoiceAssistantUiState())
    val state: StateFlow<VoiceAssistantUiState> = _state.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false
    private var isTtsInitializing = false
    private var pendingSpeechText: String? = null
    private var pendingFollowUpListen = false
    private var isDestroyed = false

    // 4-Second Pre-Speech & Post-Speech Grace Window State
    private var isSessionOpen = false
    private var sessionStartRealtimeMs = 0L
    private var lastSpeechActivityRealtimeMs = 0L
    private var hasDetectedSpeechInSession = false
    private val accumulatedSegments = mutableListOf<String>()
    private var currentLivePartial = ""
    private var isRestartingRecognizerInternal = false

    private val silenceWatchdogRunnable = object : Runnable {
        override fun run() {
            if (!isSessionOpen || isDestroyed) return
            val now = SystemClock.elapsedRealtime()
            val elapsedSinceLastActivity = now - lastSpeechActivityRealtimeMs
            val remainingMs = (SILENCE_AUTO_CLOSE_DELAY_MS - elapsedSinceLastActivity).coerceAtLeast(0L)
            val remainingSecs = ceil(remainingMs / 1000.0).toInt().coerceIn(0, 4)

            if (_state.value.remainingSilenceSeconds != remainingSecs) {
                _state.value = _state.value.copy(remainingSilenceSeconds = remainingSecs)
            }

            if (elapsedSinceLastActivity >= SILENCE_AUTO_CLOSE_DELAY_MS) {
                // 4 seconds of silence elapsed -> auto-close microphone and commit any spoken transcript
                finalizeAndAutoCloseSession()
            } else {
                mainHandler.postDelayed(this, WATCHDOG_TICK_MS)
            }
        }
    }

    private fun ensureTextToSpeechInitialized() {
        if (isTtsReady || isTtsInitializing || isDestroyed) return
        isTtsInitializing = true
        runCatching {
            textToSpeech = TextToSpeech(context.applicationContext) { status ->
                isTtsInitializing = false
                if (status == TextToSpeech.SUCCESS && !isDestroyed) {
                    val tts = textToSpeech ?: return@TextToSpeech
                    val res = tts.setLanguage(Locale("en", "IN"))
                    if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts.language = Locale.US
                    }
                    isTtsReady = true
                    tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _state.value = _state.value.copy(isSpeakingReply = true)
                        }

                        override fun onDone(utteranceId: String?) {
                            _state.value = _state.value.copy(isSpeakingReply = false)
                            if (pendingFollowUpListen && !isDestroyed) {
                                pendingFollowUpListen = false
                                mainHandler.postDelayed({ startListeningSession() }, 250L)
                            }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            _state.value = _state.value.copy(isSpeakingReply = false)
                            if (pendingFollowUpListen && !isDestroyed) {
                                pendingFollowUpListen = false
                                mainHandler.postDelayed({ startListeningSession() }, 250L)
                            }
                        }
                    })
                    val queued = pendingSpeechText
                    pendingSpeechText = null
                    if (!queued.isNullOrBlank()) {
                        speakOutLoud(queued, pendingFollowUpListen)
                    }
                } else {
                    _state.value = _state.value.copy(isSpeakingReply = false)
                    if (pendingFollowUpListen && !isDestroyed) {
                        pendingFollowUpListen = false
                        mainHandler.postDelayed({ startListeningSession() }, 350L)
                    }
                }
            }
        }.onFailure {
            isTtsInitializing = false
            if (pendingFollowUpListen && !isDestroyed) {
                pendingFollowUpListen = false
                mainHandler.postDelayed({ startListeningSession() }, 350L)
            }
        }
    }

    fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Toggles the Voice Command session.
     * If the controller is currently listening, processing a command, or speaking a reply,
     * tapping the button immediately stops listening, cancels processing, and mutes speech.
     */
    fun toggleListeningSession() {
        val current = _state.value
        if (isSessionOpen || current.isActivelyListening || current.isProcessingCommand || current.isSpeakingReply) {
            stopAndMuteAll()
        } else {
            startListeningSession()
        }
    }

    /**
     * Starts a 4-second grace-window voice recognition session when the user presses the Microphone button.
     */
    fun startListeningSession() {
        mainHandler.post {
            if (isDestroyed) return@post
            if (!hasAudioPermission()) {
                _state.value = _state.value.copy(
                    isActivelyListening = false,
                    lastError = "Microphone permission is needed to use Voice Command."
                )
                return@post
            }

            // Cancel any previous speech or processing before starting fresh
            mainHandler.removeCallbacks(silenceWatchdogRunnable)
            pendingFollowUpListen = false
            pendingSpeechText = null
            if (_state.value.isSpeakingReply) {
                runCatching { textToSpeech?.stop() }
            }

            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                _state.value = _state.value.copy(
                    isActivelyListening = false,
                    isProcessingCommand = false,
                    isSpeakingReply = false,
                    lastError = "Speech recognition service is unavailable on this device."
                )
                return@post
            }

            val now = SystemClock.elapsedRealtime()
            isSessionOpen = true
            sessionStartRealtimeMs = now
            lastSpeechActivityRealtimeMs = now
            hasDetectedSpeechInSession = false
            accumulatedSegments.clear()
            currentLivePartial = ""

            _state.value = _state.value.copy(
                isActivelyListening = true,
                isProcessingCommand = false,
                isSpeakingReply = false,
                remainingSilenceSeconds = 4,
                partialTranscript = "",
                lastError = null
            )

            startUnderlyingRecognizerInternal()
            mainHandler.removeCallbacks(silenceWatchdogRunnable)
            mainHandler.postDelayed(silenceWatchdogRunnable, WATCHDOG_TICK_MS)
        }
    }

    private fun startUnderlyingRecognizerInternal() {
        if (!isSessionOpen || isDestroyed) return
        runCatching {
            isRestartingRecognizerInternal = true
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext).apply {
                    setRecognitionListener(createRecognitionListener())
                }
            } else {
                runCatching { speechRecognizer?.cancel() }
            }
            val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                // Configure 4-second (4,000 ms) pre-speech and post-speech silence thresholds
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, SILENCE_AUTO_CLOSE_DELAY_MS)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, SILENCE_AUTO_CLOSE_DELAY_MS)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, SILENCE_AUTO_CLOSE_DELAY_MS)
            }
            speechRecognizer?.startListening(recognizerIntent)
            isRestartingRecognizerInternal = false
        }.onFailure { e ->
            isRestartingRecognizerInternal = false
            isSessionOpen = false
            mainHandler.removeCallbacks(silenceWatchdogRunnable)
            releaseSpeechRecognizer()
            _state.value = _state.value.copy(
                isActivelyListening = false,
                lastError = e.localizedMessage
            )
        }
    }

    private fun buildCombinedTranscript(): String {
        val parts = mutableListOf<String>()
        accumulatedSegments.forEach { seg ->
            if (seg.isNotBlank()) parts.add(seg.trim())
        }
        if (currentLivePartial.isNotBlank()) {
            val live = currentLivePartial.trim()
            if (parts.isEmpty() || !parts.last().equals(live, ignoreCase = true)) {
                parts.add(live)
            }
        }
        return parts.joinToString(" ").trim()
    }

    /**
     * Called automatically when the 4-second silence timer expires.
     * Closes the microphone and processes the captured command if speech was heard.
     */
    private fun finalizeAndAutoCloseSession() {
        mainHandler.removeCallbacks(silenceWatchdogRunnable)
        isSessionOpen = false
        releaseSpeechRecognizer()

        val fullTranscript = buildCombinedTranscript()
        accumulatedSegments.clear()
        currentLivePartial = ""

        if (fullTranscript.isNotBlank()) {
            markProcessingStarted(fullTranscript)
            onUtteranceCaptured(fullTranscript)
        } else {
            _state.value = _state.value.copy(
                isActivelyListening = false,
                isProcessingCommand = false,
                remainingSilenceSeconds = 4,
                partialTranscript = "",
                lastError = "No speech detected after 4 seconds. Microphone closed to save battery."
            )
        }
    }

    /**
     * Immediately stops listening, cancels any in-flight command processing, and mutes any spoken reply.
     */
    fun stopAndMuteAll() {
        mainHandler.post {
            mainHandler.removeCallbacksAndMessages(null)
            isSessionOpen = false
            pendingFollowUpListen = false
            pendingSpeechText = null
            accumulatedSegments.clear()
            currentLivePartial = ""
            releaseSpeechRecognizer()
            runCatching { textToSpeech?.stop() }
            _state.value = _state.value.copy(
                isActivelyListening = false,
                isProcessingCommand = false,
                isSpeakingReply = false,
                remainingSilenceSeconds = 4,
                partialTranscript = "",
                lastError = null
            )
            onCancelActiveCommand()
        }
    }

    /**
     * Alias for stopping the microphone session and muting active output.
     */
    fun stopListeningSession() {
        stopAndMuteAll()
    }

    private fun releaseSpeechRecognizer() {
        runCatching {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        }
        speechRecognizer = null
    }

    fun markProcessingStarted(transcript: String) {
        _state.value = _state.value.copy(
            isActivelyListening = false,
            isProcessingCommand = true,
            remainingSilenceSeconds = 4,
            partialTranscript = "",
            lastSpokenTranscript = transcript,
            lastError = null
        )
    }

    fun onCommandProcessedAndSpeak(
        replyText: String,
        errorMessage: String? = null,
        listenForFollowUpAnswer: Boolean = false
    ) {
        mainHandler.post {
            _state.value = _state.value.copy(
                isActivelyListening = false,
                isProcessingCommand = false,
                lastGeminiReply = replyText,
                lastError = errorMessage
            )
            speakOutLoud(replyText, listenForFollowUpAnswer)
        }
    }

    fun speakOutLoud(text: String, listenAfterFinished: Boolean = false) {
        if (text.isBlank() || isDestroyed) return
        mainHandler.post {
            releaseSpeechRecognizer()
            pendingFollowUpListen = listenAfterFinished
            val tts = textToSpeech
            if (tts != null && isTtsReady) {
                _state.value = _state.value.copy(isActivelyListening = false, isSpeakingReply = true)
                val params = Bundle()
                val res = tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, "vault_voice_reply_${System.currentTimeMillis()}")
                if (res == TextToSpeech.ERROR) {
                    _state.value = _state.value.copy(isSpeakingReply = false)
                    if (pendingFollowUpListen) {
                        pendingFollowUpListen = false
                        mainHandler.postDelayed({ startListeningSession() }, 350L)
                    }
                }
            } else {
                pendingSpeechText = text
                ensureTextToSpeechInitialized()
            }
        }
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                if (!isSessionOpen) return
                _state.value = _state.value.copy(isActivelyListening = true, lastError = null)
            }

            override fun onBeginningOfSpeech() {
                if (!isSessionOpen) return
                hasDetectedSpeechInSession = true
                lastSpeechActivityRealtimeMs = SystemClock.elapsedRealtime()
                _state.value = _state.value.copy(
                    isActivelyListening = true,
                    remainingSilenceSeconds = 4
                )
            }

            override fun onRmsChanged(rmsdB: Float) {
                if (!isSessionOpen) return
                // If user is actively speaking loudly enough, refresh the 4-second post-speech timer
                if (hasDetectedSpeechInSession && rmsdB > 3.5f) {
                    lastSpeechActivityRealtimeMs = SystemClock.elapsedRealtime()
                }
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                // Do not immediately close onEndOfSpeech; let onResults + 4-second grace timer handle auto-close
            }

            override fun onError(error: Int) {
                if (!isSessionOpen || isRestartingRecognizerInternal) return

                if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                    mainHandler.removeCallbacks(silenceWatchdogRunnable)
                    isSessionOpen = false
                    releaseSpeechRecognizer()
                    _state.value = _state.value.copy(
                        isActivelyListening = false,
                        partialTranscript = "",
                        lastError = "Microphone permission is required for Voice Command."
                    )
                    return
                }

                val now = SystemClock.elapsedRealtime()
                val elapsedSinceLastActivity = now - lastSpeechActivityRealtimeMs

                // If the OS recognizer timed out early (< 4 seconds of silence), keep listening until the 4-second window expires!
                if (elapsedSinceLastActivity < SILENCE_AUTO_CLOSE_DELAY_MS - 300L) {
                    mainHandler.postDelayed({
                        if (isSessionOpen && !isDestroyed) {
                            startUnderlyingRecognizerInternal()
                        }
                    }, 120L)
                } else {
                    finalizeAndAutoCloseSession()
                }
            }

            override fun onResults(results: Bundle?) {
                if (!isSessionOpen) return
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val topTranscript = matches?.firstOrNull()?.trim().orEmpty()
                if (topTranscript.isNotBlank()) {
                    hasDetectedSpeechInSession = true
                    lastSpeechActivityRealtimeMs = SystemClock.elapsedRealtime()
                    accumulatedSegments.add(topTranscript)
                    currentLivePartial = ""
                    val combined = buildCombinedTranscript()
                    _state.value = _state.value.copy(
                        isActivelyListening = true,
                        remainingSilenceSeconds = 4,
                        partialTranscript = combined
                    )
                    // Keep microphone alive during the 4-second post-speech grace window in case the user adds more words
                    mainHandler.postDelayed({
                        if (isSessionOpen && !isDestroyed) {
                            startUnderlyingRecognizerInternal()
                        }
                    }, 120L)
                } else {
                    val now = SystemClock.elapsedRealtime()
                    if (now - lastSpeechActivityRealtimeMs < SILENCE_AUTO_CLOSE_DELAY_MS - 300L) {
                        mainHandler.postDelayed({
                            if (isSessionOpen && !isDestroyed) {
                                startUnderlyingRecognizerInternal()
                            }
                        }, 120L)
                    } else {
                        finalizeAndAutoCloseSession()
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                if (!isSessionOpen) return
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()
                if (partial.isNotBlank()) {
                    hasDetectedSpeechInSession = true
                    lastSpeechActivityRealtimeMs = SystemClock.elapsedRealtime()
                    currentLivePartial = partial
                    val combined = buildCombinedTranscript()
                    _state.value = _state.value.copy(
                        isActivelyListening = true,
                        remainingSilenceSeconds = 4,
                        partialTranscript = combined
                    )
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    fun destroy() {
        isDestroyed = true
        isSessionOpen = false
        mainHandler.removeCallbacksAndMessages(null)
        releaseSpeechRecognizer()
        runCatching {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
        }
    }
}
