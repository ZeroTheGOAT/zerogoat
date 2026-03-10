package com.zerogoat.zero.voice

/**
 * Voice configuration settings.
 */
data class SpeechConfig(
    val wakeWords: List<String> = listOf("hey zero", "zero", "ok zero"),
    val continuousMode: Boolean = false,
    val ttsPitch: Float = 0.9f,
    val ttsSpeechRate: Float = 1.1f,
    val ttsLanguage: String = "en-US",
    val wakeWordEnabled: Boolean = true
)
