package com.zerogoat.zero

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.zerogoat.zero.storage.PreferencesManager
import com.zerogoat.zero.ui.chat.ChatScreen
import com.zerogoat.zero.ui.setup.SetupScreen
import com.zerogoat.zero.ui.settings.SettingsScreen
import com.zerogoat.zero.ui.settings.UsageDashboard
import com.zerogoat.zero.ui.theme.ZeroGoatTheme

/**
 * Main entry point for ZeroGoat.
 * Routes between setup, chat, settings, and usage screens.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = PreferencesManager(this)

        setContent {
            ZeroGoatTheme {
                var currentScreen by remember { mutableStateOf(
                    if (prefs.onboardingDone) "chat" else "setup"
                )}

                when (currentScreen) {
                    "setup" -> SetupScreen(
                        onSetupComplete = { currentScreen = "chat" }
                    )
                    "chat" -> ChatScreen(
                        onNavigateToSettings = { currentScreen = "settings" }
                    )
                    "settings" -> SettingsScreen(
                        onBack = { currentScreen = "chat" }
                    )
                    "usage" -> UsageDashboard(
                        onBack = { currentScreen = "settings" }
                    )
                }
            }
        }
    }
}
