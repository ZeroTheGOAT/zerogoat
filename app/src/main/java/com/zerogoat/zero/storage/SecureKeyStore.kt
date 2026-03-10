package com.zerogoat.zero.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure key storage using Android's EncryptedSharedPreferences.
 * Inspired by OpenClaw's SecurePrefs — API keys are encrypted at rest.
 */
class SecureKeyStore(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "zero_secure_keys",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_GEMINI = "api_key_gemini"
        private const val KEY_OPENAI = "api_key_openai"
        private const val KEY_ANTHROPIC = "api_key_anthropic"
        private const val KEY_OPENROUTER = "api_key_openrouter"
        private const val KEY_GROQ = "api_key_groq"
        private const val KEY_OLLAMA_URL = "ollama_base_url"
        private const val KEY_TELEGRAM_BOT = "telegram_bot_token"
        private const val KEY_ACTIVE_PROVIDER = "active_provider"
        private const val KEY_ACTIVE_MODEL = "active_model"
    }

    // API Keys
    var geminiApiKey: String?
        get() = prefs.getString(KEY_GEMINI, null)
        set(value) = prefs.edit().putString(KEY_GEMINI, value).apply()

    var openaiApiKey: String?
        get() = prefs.getString(KEY_OPENAI, null)
        set(value) = prefs.edit().putString(KEY_OPENAI, value).apply()

    var anthropicApiKey: String?
        get() = prefs.getString(KEY_ANTHROPIC, null)
        set(value) = prefs.edit().putString(KEY_ANTHROPIC, value).apply()

    var openRouterApiKey: String?
        get() = prefs.getString(KEY_OPENROUTER, null)
        set(value) = prefs.edit().putString(KEY_OPENROUTER, value).apply()

    var groqApiKey: String?
        get() = prefs.getString(KEY_GROQ, null)
        set(value) = prefs.edit().putString(KEY_GROQ, value).apply()

    var ollamaBaseUrl: String?
        get() = prefs.getString(KEY_OLLAMA_URL, "http://localhost:11434")
        set(value) = prefs.edit().putString(KEY_OLLAMA_URL, value).apply()

    var telegramBotToken: String?
        get() = prefs.getString(KEY_TELEGRAM_BOT, null)
        set(value) = prefs.edit().putString(KEY_TELEGRAM_BOT, value).apply()

    // Active provider & model
    var activeProvider: String
        get() = prefs.getString(KEY_ACTIVE_PROVIDER, "openrouter") ?: "openrouter"
        set(value) = prefs.edit().putString(KEY_ACTIVE_PROVIDER, value).apply()

    var activeModel: String
        get() = prefs.getString(KEY_ACTIVE_MODEL, "google/gemini-2.5-flash") ?: "google/gemini-2.5-flash"
        set(value) = prefs.edit().putString(KEY_ACTIVE_MODEL, value).apply()

    /** Check if at least one API key is configured */
    fun hasAnyApiKey(): Boolean {
        return !geminiApiKey.isNullOrEmpty() ||
               !openaiApiKey.isNullOrEmpty() ||
               !anthropicApiKey.isNullOrEmpty() ||
               !openRouterApiKey.isNullOrEmpty() ||
               !groqApiKey.isNullOrEmpty()
    }

    /** Get the API key for the active provider */
    fun getActiveApiKey(): String? {
        return when (activeProvider) {
            "gemini" -> geminiApiKey
            "openai" -> openaiApiKey
            "anthropic" -> anthropicApiKey
            "openrouter" -> openRouterApiKey
            "groq" -> groqApiKey
            "ollama" -> ollamaBaseUrl
            else -> openRouterApiKey ?: geminiApiKey
        }
    }

    /** Clear all stored keys */
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
