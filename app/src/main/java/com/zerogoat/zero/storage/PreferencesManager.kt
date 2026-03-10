package com.zerogoat.zero.storage

import android.content.Context
import android.content.SharedPreferences

/**
 * App preferences — non-sensitive settings stored in regular SharedPreferences.
 */
class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("zero_preferences", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_WAKE_WORD_ENABLED = "wake_word_enabled"
        private const val KEY_WAKE_WORDS = "wake_words"
        private const val KEY_CONTINUOUS_VOICE = "continuous_voice"
        private const val KEY_MAX_STEPS = "max_steps"
        private const val KEY_CONFIRM_PAYMENTS = "confirm_payments"
        private const val KEY_DARK_THEME = "dark_theme"
        private const val KEY_WA_CONTROL_CONTACT = "wa_control_contact"
        private const val KEY_FLOATING_BUBBLE = "floating_bubble"
    }

    var onboardingDone: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_DONE, value).apply()

    var wakeWordEnabled: Boolean
        get() = prefs.getBoolean(KEY_WAKE_WORD_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_WAKE_WORD_ENABLED, value).apply()

    var wakeWords: String
        get() = prefs.getString(KEY_WAKE_WORDS, "hey zero, zero, ok zero") ?: "hey zero"
        set(value) = prefs.edit().putString(KEY_WAKE_WORDS, value).apply()

    var continuousVoice: Boolean
        get() = prefs.getBoolean(KEY_CONTINUOUS_VOICE, false)
        set(value) = prefs.edit().putBoolean(KEY_CONTINUOUS_VOICE, value).apply()

    var maxSteps: Int
        get() = prefs.getInt(KEY_MAX_STEPS, 30)
        set(value) = prefs.edit().putInt(KEY_MAX_STEPS, value).apply()

    var confirmPayments: Boolean
        get() = prefs.getBoolean(KEY_CONFIRM_PAYMENTS, true)
        set(value) = prefs.edit().putBoolean(KEY_CONFIRM_PAYMENTS, value).apply()

    var darkTheme: Boolean
        get() = prefs.getBoolean(KEY_DARK_THEME, true)
        set(value) = prefs.edit().putBoolean(KEY_DARK_THEME, value).apply()

    var whatsappControlContact: String?
        get() = prefs.getString(KEY_WA_CONTROL_CONTACT, null)
        set(value) = prefs.edit().putString(KEY_WA_CONTROL_CONTACT, value).apply()

    var floatingBubbleEnabled: Boolean
        get() = prefs.getBoolean(KEY_FLOATING_BUBBLE, true)
        set(value) = prefs.edit().putBoolean(KEY_FLOATING_BUBBLE, value).apply()
}
