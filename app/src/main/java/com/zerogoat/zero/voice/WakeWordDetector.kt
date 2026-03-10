package com.zerogoat.zero.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/**
 * Lightweight wake word detector — listens for "Hey Zero" (configurable).
 * Inspired by OpenClaw's WakeWords with parseCommaSeparated pattern.
 * Uses Android's SpeechRecognizer in continuous partial-results mode.
 */
class WakeWordDetector(private val context: Context) {

    companion object {
        private const val TAG = "WakeWord"
        val DEFAULT_WAKE_WORDS = listOf("hey zero", "zero", "ok zero")
    }

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive

    private val _detected = MutableStateFlow(false)
    val detected: StateFlow<Boolean> = _detected

    private var speechRecognizer: SpeechRecognizer? = null
    private var wakeWords = DEFAULT_WAKE_WORDS
    private var onWakeWordDetected: (() -> Unit)? = null

    /** Parse comma-separated wake words (like OpenClaw's WakeWords) */
    fun parseCommaSeparated(input: String): List<String> {
        return input.split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
    }

    /** Update wake words if changed */
    fun updateWakeWords(input: String) {
        val parsed = parseCommaSeparated(input)
        if (parsed.isNotEmpty() && parsed != wakeWords) {
            wakeWords = parsed
            Log.i(TAG, "Wake words updated: $wakeWords")
        }
    }

    /** Start listening for wake words */
    fun start(onDetected: () -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w(TAG, "Speech recognition not available")
            return
        }

        onWakeWordDetected = onDetected
        _isActive.value = true

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer?.setRecognitionListener(createListener())
        startRecognition()
    }

    /** Stop listening */
    fun stop() {
        _isActive.value = false
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    private fun startRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
        }
        speechRecognizer?.startListening(intent)
    }

    private fun checkForWakeWord(text: String): Boolean {
        val lower = text.lowercase().trim()
        return wakeWords.any { word ->
            lower.contains(word) || lower.endsWith(word) || lower.startsWith(word)
        }
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                // Auto-restart on non-fatal errors
                if (_isActive.value && error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                    startRecognition()
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""

                if (text.isNotEmpty() && checkForWakeWord(text)) {
                    Log.i(TAG, "Wake word detected: '$text'")
                    _detected.value = true
                    onWakeWordDetected?.invoke()
                    _detected.value = false
                }

                // Continue listening
                if (_isActive.value) {
                    startRecognition()
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""

                if (text.isNotEmpty() && checkForWakeWord(text)) {
                    Log.i(TAG, "Wake word detected (partial): '$text'")
                    _detected.value = true
                    onWakeWordDetected?.invoke()
                    _detected.value = false
                    speechRecognizer?.stopListening()
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }
}
