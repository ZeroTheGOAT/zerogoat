package com.zerogoat.zero.ui.setup

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerogoat.zero.accessibility.ZeroAccessibilityService
import com.zerogoat.zero.llm.ModelRegistry
import com.zerogoat.zero.storage.PreferencesManager
import com.zerogoat.zero.storage.SecureKeyStore
import com.zerogoat.zero.ui.settings.ModelPickerDialog
import com.zerogoat.zero.ui.theme.ZeroColors

/**
 * First-time setup screen — welcomes user, collects API key,
 * and guides through permission setup.
 */
@Composable
fun SetupScreen(onSetupComplete: () -> Unit) {
    val context = LocalContext.current
    val keyStore = remember { SecureKeyStore(context) }
    val prefs = remember { PreferencesManager(context) }

    var currentStep by remember { mutableIntStateOf(0) }
    var selectedProvider by remember { mutableStateOf("openrouter") }
    var selectedModel by remember { mutableStateOf("google/gemini-2.0-flash-exp:free") }
    var showModelPicker by remember { mutableStateOf(false) }
    var apiKey by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }

    val accessibilityEnabled by ZeroAccessibilityService.isRunning.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ZeroColors.BgPrimary)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (currentStep) {
            // Step 0: Welcome
            0 -> {
                Spacer(modifier = Modifier.height(48.dp))
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(
                            listOf(ZeroColors.AccentCyan, ZeroColors.AccentTeal)
                        )),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Z", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Meet Zero",
                    fontSize = 32.sp, fontWeight = FontWeight.Bold,
                    color = ZeroColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Your AI assistant that controls your phone.\nLike JARVIS, but on Android.",
                    fontSize = 16.sp, color = ZeroColors.TextSecondary,
                    textAlign = TextAlign.Center, lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(48.dp))
                Button(
                    onClick = { currentStep = 1 },
                    colors = ButtonDefaults.buttonColors(containerColor = ZeroColors.AccentCyan),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Get Started", fontSize = 16.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            // Step 1: API Key
            1 -> {
                Text("Add Your API Key", fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    color = ZeroColors.TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Zero needs an AI provider to think. Choose one:",
                    color = ZeroColors.TextSecondary, textAlign = TextAlign.Center)

                Spacer(modifier = Modifier.height(24.dp))

                // Provider selection
                val providers = listOf(
                    "openrouter" to "🌐 OpenRouter (100+ models)",
                    "gemini" to "🔵 Google Gemini",
                    "openai" to "🟢 OpenAI",
                    "anthropic" to "🟠 Anthropic",
                    "groq" to "⚡ Groq (Fast)",
                    "ollama" to "🏠 Ollama (Local)"
                )
                providers.forEach { (id, label) ->
                    Surface(
                        onClick = { selectedProvider = id },
                        color = if (selectedProvider == id) ZeroColors.AccentCyan.copy(alpha = 0.15f)
                        else ZeroColors.Surface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedProvider == id,
                                onClick = { selectedProvider = id },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = ZeroColors.AccentCyan
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label, color = ZeroColors.TextPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Model Picker (only if openrouter, groq etc. We can just show it always or based on logic)
                val modelInfo = ModelRegistry.findById(selectedModel)
                Surface(
                    onClick = { showModelPicker = true },
                    color = ZeroColors.Surface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(modelInfo?.name ?: selectedModel, color = ZeroColors.TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${modelInfo?.provider ?: "?"} · ${modelInfo?.let { "\$${it.inputCostPer1M}/M in" } ?: ""}",
                                color = ZeroColors.TextSecondary, fontSize = 12.sp
                            )
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = ZeroColors.TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("API Key") },
                    placeholder = { Text("Paste your API key here") },
                    visualTransformation = if (showKey) VisualTransformation.None
                        else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                if (showKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (showKey) "Hide" else "Show"
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ZeroColors.AccentCyan,
                        unfocusedBorderColor = ZeroColors.BgTertiary,
                        cursorColor = ZeroColors.AccentCyan,
                        focusedTextColor = ZeroColors.TextPrimary,
                        unfocusedTextColor = ZeroColors.TextPrimary,
                        focusedLabelColor = ZeroColors.AccentCyan
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        when (selectedProvider) {
                            "openrouter" -> keyStore.openRouterApiKey = apiKey
                            "gemini" -> keyStore.geminiApiKey = apiKey
                            "openai" -> keyStore.openaiApiKey = apiKey
                            "anthropic" -> keyStore.anthropicApiKey = apiKey
                            "groq" -> keyStore.groqApiKey = apiKey
                        }
                        keyStore.activeProvider = selectedProvider
                        keyStore.activeModel = selectedModel
                        currentStep = 2
                    },
                    enabled = apiKey.length >= 10,
                    colors = ButtonDefaults.buttonColors(containerColor = ZeroColors.AccentCyan),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Save & Continue", fontSize = 16.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            // Step 2: Accessibility Permission
            2 -> {
                Icon(
                    Icons.Filled.Accessibility,
                    contentDescription = null,
                    tint = ZeroColors.AccentCyan,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Enable Accessibility", fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    color = ZeroColors.TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Zero needs permission to see and interact with your screen.",
                    color = ZeroColors.TextSecondary, textAlign = TextAlign.Center)

                Spacer(modifier = Modifier.height(24.dp))

                Surface(
                    color = ZeroColors.Surface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Steps:", fontWeight = FontWeight.SemiBold, color = ZeroColors.TextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        listOf(
                            "1. Tap the button below to open Settings",
                            "2. Find 'ZeroGoat' in the list",
                            "3. Toggle it ON",
                            "4. Tap 'Allow' on the confirmation dialog",
                            "5. Come back to this screen"
                        ).forEach { step ->
                            Text(step, color = ZeroColors.TextSecondary,
                                modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (accessibilityEnabled) {
                    Surface(
                        color = ZeroColors.Success.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = ZeroColors.Success)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Accessibility Service is enabled! ✓", color = ZeroColors.Success)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!accessibilityEnabled) {
                    Button(
                        onClick = {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ZeroColors.AccentBlue),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Open Accessibility Settings", fontSize = 16.sp, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(
                    onClick = { currentStep = 3 },
                    enabled = accessibilityEnabled,
                    colors = ButtonDefaults.buttonColors(containerColor = ZeroColors.AccentCyan),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Continue", fontSize = 16.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            // Step 3: Done
            3 -> {
                Icon(
                    Icons.Filled.RocketLaunch,
                    contentDescription = null,
                    tint = ZeroColors.AccentCyan,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text("Zero is Ready!", fontSize = 28.sp, fontWeight = FontWeight.Bold,
                    color = ZeroColors.TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Try saying: \"Hey Zero, open Chrome\"",
                    color = ZeroColors.TextSecondary, textAlign = TextAlign.Center, fontSize = 16.sp)

                Spacer(modifier = Modifier.height(48.dp))
                Button(
                    onClick = {
                        prefs.onboardingDone = true
                        onSetupComplete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ZeroColors.AccentCyan),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Let's Go! 🚀", fontSize = 16.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Step indicator
        Spacer(modifier = Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.Center) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (index == currentStep) 24.dp else 8.dp, 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == currentStep) ZeroColors.AccentCyan
                            else if (index < currentStep) ZeroColors.AccentTeal
                            else ZeroColors.BgTertiary
                        )
                )
            }
        }
    }

    if (showModelPicker) {
        ModelPickerDialog(
            currentModel = selectedModel,
            onSelect = { selectedModel = it; showModelPicker = false },
            onDismiss = { showModelPicker = false }
        )
    }
}
