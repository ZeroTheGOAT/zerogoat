package com.zerogoat.zero.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerogoat.zero.llm.ModelRegistry
import com.zerogoat.zero.llm.ThinkingLevel
import com.zerogoat.zero.storage.PreferencesManager
import com.zerogoat.zero.storage.SecureKeyStore
import com.zerogoat.zero.ui.theme.ZeroColors

/**
 * Full settings screen — provider, model, voice, channels, agent config.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val keyStore = remember { SecureKeyStore(context) }
    val prefs = remember { PreferencesManager(context) }

    var selectedProvider by remember { mutableStateOf(keyStore.activeProvider) }
    var selectedModel by remember { mutableStateOf(keyStore.activeModel) }
    var showModelPicker by remember { mutableStateOf(false) }

    // API Key states
    var openRouterKey by remember { mutableStateOf(keyStore.openRouterApiKey ?: "") }
    var geminiKey by remember { mutableStateOf(keyStore.geminiApiKey ?: "") }
    var openaiKey by remember { mutableStateOf(keyStore.openaiApiKey ?: "") }
    var anthropicKey by remember { mutableStateOf(keyStore.anthropicApiKey ?: "") }
    var groqKey by remember { mutableStateOf(keyStore.groqApiKey ?: "") }
    var telegramToken by remember { mutableStateOf(keyStore.telegramBotToken ?: "") }
    var waContact by remember { mutableStateOf(prefs.whatsappControlContact ?: "") }

    // Preferences
    var wakeWordEnabled by remember { mutableStateOf(prefs.wakeWordEnabled) }
    var continuousVoice by remember { mutableStateOf(prefs.continuousVoice) }
    var confirmPayments by remember { mutableStateOf(prefs.confirmPayments) }
    var maxSteps by remember { mutableStateOf(prefs.maxSteps.toString()) }
    var thinkingLevel by remember { mutableStateOf("balanced") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        // Save all
                        keyStore.activeProvider = selectedProvider
                        keyStore.activeModel = selectedModel
                        keyStore.openRouterApiKey = openRouterKey.ifBlank { null }
                        keyStore.geminiApiKey = geminiKey.ifBlank { null }
                        keyStore.openaiApiKey = openaiKey.ifBlank { null }
                        keyStore.anthropicApiKey = anthropicKey.ifBlank { null }
                        keyStore.groqApiKey = groqKey.ifBlank { null }
                        keyStore.telegramBotToken = telegramToken.ifBlank { null }
                        prefs.whatsappControlContact = waContact.ifBlank { null }
                        prefs.wakeWordEnabled = wakeWordEnabled
                        prefs.continuousVoice = continuousVoice
                        prefs.confirmPayments = confirmPayments
                        prefs.maxSteps = maxSteps.toIntOrNull() ?: 30
                        onBack()
                    }) {
                        Text("Save", color = ZeroColors.AccentCyan, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ZeroColors.BgPrimary)
            )
        },
        containerColor = ZeroColors.BgPrimary
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ===== LLM Provider =====
            item { SectionHeader("🤖 AI Provider") }
            item {
                val providers = listOf(
                    "openrouter" to "🌐 OpenRouter (100+ models)",
                    "gemini" to "🔵 Google Gemini",
                    "openai" to "🟢 OpenAI",
                    "anthropic" to "🟠 Anthropic",
                    "groq" to "⚡ Groq (Fast)",
                    "ollama" to "🏠 Ollama (Local)"
                )
                Column {
                    providers.forEach { (id, label) ->
                        Surface(
                            onClick = { selectedProvider = id },
                            color = if (selectedProvider == id) ZeroColors.AccentCyan.copy(alpha = 0.15f)
                            else ZeroColors.Surface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = selectedProvider == id,
                                    onClick = { selectedProvider = id },
                                    colors = RadioButtonDefaults.colors(selectedColor = ZeroColors.AccentCyan)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(label, color = ZeroColors.TextPrimary, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            // ===== Model Picker =====
            item { SectionHeader("🧠 Model") }
            item {
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
            }

            // ===== Thinking Level =====
            item { SectionHeader("💭 Thinking Level") }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThinkingLevel.entries.forEach { level ->
                        Surface(
                            onClick = { thinkingLevel = level.name.lowercase() },
                            color = if (thinkingLevel == level.name.lowercase()) ZeroColors.AccentCyan.copy(alpha = 0.2f) else ZeroColors.Surface,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(level.label, fontSize = 13.sp, color = ZeroColors.TextPrimary)
                            }
                        }
                    }
                }
            }

            // ===== API Keys =====
            item { SectionHeader("🔑 API Keys") }
            item { ApiKeyField("OpenRouter", openRouterKey, { openRouterKey = it }) }
            item { ApiKeyField("Gemini", geminiKey, { geminiKey = it }) }
            item { ApiKeyField("OpenAI", openaiKey, { openaiKey = it }) }
            item { ApiKeyField("Anthropic", anthropicKey, { anthropicKey = it }) }
            item { ApiKeyField("Groq", groqKey, { groqKey = it }) }

            // ===== Voice =====
            item { SectionHeader("🎤 Voice") }
            item {
                SettingsToggle("Wake word (Hey Zero)", wakeWordEnabled) { wakeWordEnabled = it }
            }
            item {
                SettingsToggle("Continuous listening", continuousVoice) { continuousVoice = it }
            }

            // ===== Channels =====
            item { SectionHeader("📡 Channels") }
            item { ApiKeyField("Telegram Bot Token", telegramToken, { telegramToken = it }) }
            item { ApiKeyField("WhatsApp Control Contact", waContact, { waContact = it }, isPassword = false) }

            // ===== Agent =====
            item { SectionHeader("⚙️ Agent") }
            item {
                SettingsToggle("Confirm before payments", confirmPayments) { confirmPayments = it }
            }
            item {
                OutlinedTextField(
                    value = maxSteps,
                    onValueChange = { maxSteps = it.filter { c -> c.isDigit() } },
                    label = { Text("Max steps per task") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ZeroColors.AccentCyan,
                        unfocusedBorderColor = ZeroColors.BgTertiary,
                        cursorColor = ZeroColors.AccentCyan,
                        focusedTextColor = ZeroColors.TextPrimary,
                        unfocusedTextColor = ZeroColors.TextPrimary,
                        focusedLabelColor = ZeroColors.AccentCyan
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // ===== About =====
            item { SectionHeader("ℹ️ About") }
            item {
                Surface(color = ZeroColors.Surface, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("ZeroGoat v1.0.0", fontWeight = FontWeight.SemiBold, color = ZeroColors.TextPrimary)
                        Text("Zero — Your JARVIS for Android", color = ZeroColors.TextSecondary, fontSize = 13.sp)
                        Text("${ModelRegistry.allModels.size} models available", color = ZeroColors.AccentCyan, fontSize = 12.sp)
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    // Model Picker Dialog
    if (showModelPicker) {
        ModelPickerDialog(
            currentModel = selectedModel,
            onSelect = { selectedModel = it; showModelPicker = false },
            onDismiss = { showModelPicker = false }
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = ZeroColors.AccentCyan,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
    )
}

@Composable
fun ApiKeyField(label: String, value: String, onValueChange: (String) -> Unit, isPassword: Boolean = true) {
    var showKey by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = if (isPassword && !showKey) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = if (isPassword) {
            { IconButton(onClick = { showKey = !showKey }) {
                Icon(if (showKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = null, tint = ZeroColors.TextMuted)
            } }
        } else null,
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
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
    )
}

@Composable
fun SettingsToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(color = ZeroColors.Surface, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = ZeroColors.TextPrimary, modifier = Modifier.weight(1f))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedTrackColor = ZeroColors.AccentCyan)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelPickerDialog(currentModel: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ModelRegistry.Category?>(null) }

    val filteredModels = remember(searchQuery, selectedCategory) {
        ModelRegistry.allModels.filter { model ->
            val matchesSearch = searchQuery.isEmpty() ||
                model.name.contains(searchQuery, ignoreCase = true) ||
                model.provider.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == null || model.category == selectedCategory
            matchesSearch && matchesCategory
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Model (${ModelRegistry.allModels.size}+)", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                // Search
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search models…") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ZeroColors.AccentCyan,
                        cursorColor = ZeroColors.AccentCyan,
                        focusedTextColor = ZeroColors.TextPrimary,
                        unfocusedTextColor = ZeroColors.TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(Modifier.height(8.dp))

                // Category chips
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("All", fontSize = 11.sp) }
                    )
                    listOf("🆓" to ModelRegistry.Category.FREE, "⚡" to ModelRegistry.Category.FAST, "🧠" to ModelRegistry.Category.SMART, "💭" to ModelRegistry.Category.REASONING).forEach { (emoji, cat) ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                            label = { Text(emoji, fontSize = 11.sp) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))

                // Model list
                LazyColumn(Modifier.heightIn(max = 300.dp)) {
                    items(filteredModels) { model ->
                        Surface(
                            onClick = { onSelect(model.id) },
                            color = if (model.id == currentModel) ZeroColors.AccentCyan.copy(alpha = 0.15f) else ZeroColors.Surface,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        ) {
                            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(model.name, color = ZeroColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text(
                                        "${model.provider} · ${if (model.isFree) "Free" else "\$${model.inputCostPer1M}/M"}${if (model.supportsVision) " 👁️" else ""}",
                                        color = ZeroColors.TextSecondary, fontSize = 11.sp
                                    )
                                }
                                if (model.id == currentModel) {
                                    Icon(Icons.Filled.Check, contentDescription = null, tint = ZeroColors.AccentCyan, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = ZeroColors.AccentCyan) } },
        containerColor = ZeroColors.BgSecondary
    )
}
