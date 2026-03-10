package com.zerogoat.zero.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import java.util.UUID

/**
 * Voice engine — speech recognition + TTS for JARVIS-like interaction.
 * Supports push-to-talk, continuous listening, and conversational mode.
 *
 * Conversational mode (Google Assistant style):
 * - Listen → Process → Speak response → Auto-listen again
 * - User can interrupt during TTS playback
 * - Voice feedback phrases: "Sure", "Working on it", etc.
 */
class VoiceEngine(private val context: Context) {

    companion object {
        private const val TAG = "VoiceEngine"

        /** Quick voice feedback phrases */
        val ACKNOWLEDGEMENTS = listOf(
            "Sure, I'll do that.",
            "On it.",
            "Working on it.",
            "Got it.",
            "Right away."
        )
        val THINKING = listOf(
            "Let me think about that.",
            "Give me a moment.",
            "Processing."
        )
        val DONE = listOf(
            "Done.",
            "All set.",
            "Finished."
        )
    }

    enum class ListeningState { IDLE, LISTENING, PROCESSING, SPEAKING }

    /** Conversational mode — auto-listen after each TTS response */
    var conversationalMode = false

    private val _state = MutableStateFlow(ListeningState.IDLE)
    val state: StateFlow<ListeningState> = _state

    private val _lastTranscript = MutableStateFlow("")
    val lastTranscript: StateFlow<String> = _lastTranscript

    private val _partialResult = MutableStateFlow("")
    val partialResult: StateFlow<String> = _partialResult

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var onCommandReceived: ((String) -> Unit)? = null
    private var continuousMode = false

    /** Initialize the voice engine */
    fun initialize(onCommand: (String) -> Unit) {
        onCommandReceived = onCommand

        // Initialize TTS
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setPitch(0.9f)  // Slightly deeper for JARVIS feel
                tts?.setSpeechRate(1.1f)  // Slightly faster
                ttsReady = true
                Log.i(TAG, "TTS initialized")
            }
        }

        // Initialize speech recognizer
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(createListener())
            Log.i(TAG, "Speech recognizer initialized")
        } else {
            Log.w(TAG, "Speech recognition not available on this device")
        }
    }

    /** Start listening for voice commands (push-to-talk mode) */
    fun startListening() {
        if (speechRecognizer == null) return
        _state.value = ListeningState.LISTENING
        _partialResult.value = ""

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        speechRecognizer?.startListening(intent)
    }

    /** Stop listening */
    fun stopListening() {
        speechRecognizer?.stopListening()
        _state.value = ListeningState.IDLE
    }

    /** Enable/disable continuous listening mode */
    fun setContinuousMode(enabled: Boolean) {
        continuousMode = enabled
        if (enabled) startListening()
    }

    /** Speak a response using TTS */
    fun speak(text: String, onComplete: (() -> Unit)? = null) {
        if (!ttsReady) return

        _state.value = ListeningState.SPEAKING
        val utteranceId = UUID.randomUUID().toString()

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(uId: String?) {}
            override fun onError(uId: String?) {
                _state.value = ListeningState.IDLE
                onComplete?.invoke()
            }
            override fun onDone(uId: String?) {
                _state.value = ListeningState.IDLE
                onComplete?.invoke()
                // Resume listening in continuous or conversational mode
                if (continuousMode || conversationalMode) startListening()
            }
        })

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    /** Stop speaking — interrupt support for conversational mode */
    fun stopSpeaking() {
        tts?.stop()
        _state.value = ListeningState.IDLE
        // If in conversational mode, interrupting starts listening
        if (conversationalMode) startListening()
    }

    /** Speak a quick acknowledgement phrase */
    fun acknowledge() {
        speak(ACKNOWLEDGEMENTS.random())
    }

    /** Speak a "done" phrase */
    fun speakDone() {
        speak(DONE.random())
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _state.value = ListeningState.LISTENING
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _state.value = ListeningState.PROCESSING
            }

            override fun onError(error: Int) {
                Log.w(TAG, "Speech recognition error: $error")
                _state.value = ListeningState.IDLE
                // Auto-restart in continuous mode (unless it's a fatal error)
                if (continuousMode && error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                    startListening()
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()?.trim() ?: ""
                if (text.isNotEmpty()) {
                    _lastTranscript.value = text
                    _partialResult.value = ""
                    onCommandReceived?.invoke(text)
                }
                _state.value = ListeningState.IDLE
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                _partialResult.value = matches?.firstOrNull() ?: ""
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    /** Release all resources */
    fun release() {
        speechRecognizer?.destroy()
        tts?.shutdown()
        speechRecognizer = null
        tts = null
    }
}
