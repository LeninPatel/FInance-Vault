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
 * Battery-friendly on-demand Voice Transaction Controller with:
 * 1. Strict Word-Only 4-Second Auto-Close Timer (ignores ambient background noise, RMS volume
 *    fluctuations, and `onBeginningOfSpeech` acoustic triggers — only resets when genuinely
 *    new spoken words are recognized).
 * 2. 15-Second Maximum Session Safety Cap so the microphone never stays open indefinitely
 *    even in noisy environments.
 * 3. Instant All-State Mute/Cancel when tapping the microphone button while listening,
 *    processing, or speaking.
 */
class VoiceTransactionController(
    private val context: Context,
    private val onUtteranceCaptured: (String) -> Unit,
    private val onCancelActiveCommand: () -> Unit = {}
) {
    companion object {
        const val SILENCE_AUTO_CLOSE_DELAY_MS = 4000L
        const val MAX_SESSION_DURATION_MS = 15000L
        private const val WATCHDOG_TICK_MS = 200L
        private const val MIN_VALID_CONFIDENCE = 0.25f

        private val NOISE_FILLER_TOKENS = setOf(
            "uh", "um", "umm", "uhh", "ah", "ahh", "eh", "oh", "hmm", "hm", "mm", "er", "huh"
        )

        /**
         * Extracts meaningful spoken English words/numbers and strips out punctuation,
         * ambient noise filler sounds ("uh", "um", "hmm"), and single-letter non-digit artifacts.
         */
        fun normalizeMeaningfulWords(raw: String): String {
            if (raw.isBlank()) return ""
            return raw
                .lowercase(Locale.US)
                .split(Regex("""[^a-z0-9₹]+"""))
                .map { it.trim() }
                .filter { token ->
                    if (token.isEmpty() || token in NOISE_FILLER_TOKENS) {
                        false
                    } else if (token.length == 1) {
                        // Keep single digits (e.g. option "1", "2", "3") but ignore single-letter noise artifacts
                        token[0].isDigit()
                    } else {
                        true
                    }
                }
                .joinToString(" ")
                .trim()
        }
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

    // 4-Second Word-Only Grace Window & 15-Second Max Session State
    private var isSessionOpen = false
    private var sessionStartRealtimeMs = 0L
    private var lastWordRecognizedRealtimeMs = 0L
    private var lastRecognizedNormalizedWords = ""
    private val accumulatedSegments = mutableListOf<String>()
    private var currentLivePartial = ""
    private var isRestartingRecognizerInternal = false

    private val silenceWatchdogRunnable = object : Runnable {
        override fun run() {
            if (!isSessionOpen || isDestroyed) return
            val now = SystemClock.elapsedRealtime()
            val elapsedSinceLastWord = now - lastWordRecognizedRealtimeMs
            val totalSessionElapsed = now - sessionStartRealtimeMs

            val remainingSilenceMs = (SILENCE_AUTO_CLOSE_DELAY_MS - elapsedSinceLastWord).coerceAtLeast(0L)
            val remainingCapMs = (MAX_SESSION_DURATION_MS - totalSessionElapsed).coerceAtLeast(0L)
            val effectiveRemainingMs = minOf(remainingSilenceMs, remainingCapMs)
            val remainingSecs = ceil(effectiveRemainingMs / 1000.0).toInt().coerceIn(0, 4)

            if (_state.value.remainingSilenceSeconds != remainingSecs) {
                _state.value = _state.value.copy(remainingSilenceSeconds = remainingSecs)
            }

            if (elapsedSinceLastWord >= SILENCE_AUTO_CLOSE_DELAY_MS || totalSessionElapsed >= MAX_SESSION_DURATION_MS) {
                // 4 seconds without any new recognized words (or 15s max session cap reached) -> auto-close microphone
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
     * Starts a 4-second word-only grace-window voice recognition session (with 15s max session cap)
     * when the user presses the Microphone button.
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
            lastWordRecognizedRealtimeMs = now
            lastRecognizedNormalizedWords = ""
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
     * Checks whether the newly recognized transcript contains genuinely new spoken words
     * compared to what was already recognized in this session.
     * Only resets the 4-second timer when this returns true.
     */
    private fun checkAndRecordNewSpokenWords(candidateTranscript: String): Boolean {
        val normalized = normalizeMeaningfulWords(candidateTranscript)
        if (normalized.isBlank()) return false
        if (normalized == lastRecognizedNormalizedWords) return false

        lastRecognizedNormalizedWords = normalized
        lastWordRecognizedRealtimeMs = SystemClock.elapsedRealtime()
        return true
    }

    /**
     * Called automatically when 4 seconds pass without any new recognized words
     * (or when the 15-second max session cap is reached).
     * Immediately closes the microphone and processes the captured command if valid words were heard.
     */
    private fun finalizeAndAutoCloseSession() {
        mainHandler.removeCallbacks(silenceWatchdogRunnable)
        isSessionOpen = false
        releaseSpeechRecognizer()

        val fullTranscript = buildCombinedTranscript()
        val meaningfulWords = normalizeMeaningfulWords(fullTranscript)
        accumulatedSegments.clear()
        currentLivePartial = ""
        lastRecognizedNormalizedWords = ""

        if (meaningfulWords.isNotBlank()) {
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
            lastRecognizedNormalizedWords = ""
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

    private fun isStillWithinSessionWindow(): Boolean {
        val now = SystemClock.elapsedRealtime()
        val elapsedSinceLastWord = now - lastWordRecognizedRealtimeMs
        val totalSessionElapsed = now - sessionStartRealtimeMs
        return elapsedSinceLastWord < (SILENCE_AUTO_CLOSE_DELAY_MS - 350L) &&
            totalSessionElapsed < (MAX_SESSION_DURATION_MS - 350L)
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                if (!isSessionOpen) return
                _state.value = _state.value.copy(isActivelyListening = true, lastError = null)
            }

            override fun onBeginningOfSpeech() {
                // Intentionally do NOT reset the 4-second timer here:
                // Android triggers onBeginningOfSpeech on any ambient background noise (fan, AC, traffic).
                // The timer is strictly reset only when actual spoken words are recognized.
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Intentionally do NOT reset the 4-second timer on acoustic volume changes:
                // Background noise fluctuates RMS levels continuously without spoken words.
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {}

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

                // Only continue listening if we are still within the 4-second no-new-words window and 15s max session cap
                if (isStillWithinSessionWindow()) {
                    mainHandler.postDelayed({
                        if (isSessionOpen && !isDestroyed && isStillWithinSessionWindow()) {
                            startUnderlyingRecognizerInternal()
                        } else if (isSessionOpen && !isDestroyed) {
                            finalizeAndAutoCloseSession()
                        }
                    }, 150L)
                } else {
                    finalizeAndAutoCloseSession()
                }
            }

            override fun onResults(results: Bundle?) {
                if (!isSessionOpen) return
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val confidenceScores = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                val topConfidence = confidenceScores?.firstOrNull() ?: 1.0f
                val rawTop = matches?.firstOrNull()?.trim().orEmpty()
                val meaningfulSegment = normalizeMeaningfulWords(rawTop)

                val isConfidentEnough = topConfidence <= 0f || topConfidence >= MIN_VALID_CONFIDENCE

                if (meaningfulSegment.isNotBlank() && isConfidentEnough) {
                    currentLivePartial = ""
                    if (accumulatedSegments.isEmpty() ||
                        normalizeMeaningfulWords(accumulatedSegments.last()) != meaningfulSegment
                    ) {
                        accumulatedSegments.add(rawTop)
                    }
                    val combined = buildCombinedTranscript()
                    val addedNewWords = checkAndRecordNewSpokenWords(combined)
                    _state.value = _state.value.copy(
                        isActivelyListening = true,
                        remainingSilenceSeconds = if (addedNewWords) 4 else _state.value.remainingSilenceSeconds,
                        partialTranscript = combined
                    )
                }

                if (isStillWithinSessionWindow()) {
                    mainHandler.postDelayed({
                        if (isSessionOpen && !isDestroyed && isStillWithinSessionWindow()) {
                            startUnderlyingRecognizerInternal()
                        } else if (isSessionOpen && !isDestroyed) {
                            finalizeAndAutoCloseSession()
                        }
                    }, 150L)
                } else {
                    finalizeAndAutoCloseSession()
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                if (!isSessionOpen) return
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()

                val meaningfulPartial = normalizeMeaningfulWords(partial)
                if (meaningfulPartial.isNotBlank()) {
                    currentLivePartial = partial
                    val combined = buildCombinedTranscript()
                    // Only reset the 4-second countdown if genuinely new words were recognized
                    val addedNewWords = checkAndRecordNewSpokenWords(combined)
                    _state.value = _state.value.copy(
                        isActivelyListening = true,
                        remainingSilenceSeconds = if (addedNewWords) 4 else _state.value.remainingSilenceSeconds,
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
