package com.zerogoat.zero.ui.chat

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.util.Consumer
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.activity.ComponentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zerogoat.zero.agent.AgentState
import com.zerogoat.zero.ui.theme.ZeroColors
import com.zerogoat.zero.voice.VoiceEngine
import kotlinx.coroutines.launch

/**
 * Main chat screen — JARVIS-like dark interface with agent status.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel(),
    onNavigateToSettings: () -> Unit = {}
) {
    val messages by viewModel.messages.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val agentState by viewModel.agentState.collectAsState()
    val voiceState by viewModel.voiceEngine.state.collectAsState()
    val partialResult by viewModel.voiceEngine.partialResult.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    val context = LocalContext.current
    DisposableEffect(context) {
        val activity = context as? ComponentActivity
        val listener = Consumer<Intent> { intent ->
            if (intent.getBooleanExtra("start_voice", false)) {
                intent.removeExtra("start_voice")
                viewModel.voiceEngine.startListening()
            }
        }
        activity?.addOnNewIntentListener(listener)
        
        // Also check current intent
        if (activity?.intent?.getBooleanExtra("start_voice", false) == true) {
            activity.intent.removeExtra("start_voice")
            viewModel.voiceEngine.startListening()
        }
        
        onDispose {
            activity?.removeOnNewIntentListener(listener)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Pulsing status indicator
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(
                                    when (agentState) {
                                        is AgentState.Idle -> ZeroColors.AccentCyan
                                        is AgentState.Acting -> ZeroColors.Warning
                                        is AgentState.Planning -> ZeroColors.AccentBlue
                                        is AgentState.Completed -> ZeroColors.Success
                                        is AgentState.Failed -> ZeroColors.Error
                                        else -> ZeroColors.TextMuted
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Zero",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = ZeroColors.TextPrimary
                            )
                            Text(
                                text = when (agentState) {
                                    is AgentState.Idle -> "Ready"
                                    is AgentState.Planning -> "Planning..."
                                    is AgentState.Acting -> "Step ${(agentState as AgentState.Acting).step}/${(agentState as AgentState.Acting).maxSteps}"
                                    is AgentState.Completed -> "Done ✓"
                                    is AgentState.Failed -> "Failed ✗"
                                    is AgentState.WaitingConfirmation -> "Waiting for confirmation"
                                    is AgentState.Paused -> "Paused"
                                },
                                fontSize = 11.sp,
                                color = ZeroColors.TextSecondary
                            )
                        }
                    }
                },
                actions = {
                    if (isProcessing) {
                        IconButton(onClick = { viewModel.cancelTask() }) {
                            Icon(
                                Icons.Filled.Stop,
                                contentDescription = "Stop",
                                tint = ZeroColors.Error
                            )
                        }
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = ZeroColors.TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ZeroColors.BgPrimary
                )
            )
        },
        containerColor = ZeroColors.BgPrimary
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Chat messages
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(messages, key = { it.timestamp }) { message ->
                    MessageBubble(message = message)
                }

                // Confirmation buttons
                if (agentState is AgentState.WaitingConfirmation) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { viewModel.confirmAction() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ZeroColors.Success
                                ),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Icon(Icons.Filled.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Confirm")
                            }
                            OutlinedButton(
                                onClick = { viewModel.denyAction() },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = ZeroColors.Error
                                )
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cancel")
                            }
                        }
                    }
                }
            }

            // Voice partial result
            AnimatedVisibility(
                visible = voiceState == VoiceEngine.ListeningState.LISTENING && partialResult.isNotEmpty()
            ) {
                Text(
                    text = "🎤 $partialResult",
                    color = ZeroColors.AccentCyan,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Input bar
            Surface(
                color = ZeroColors.BgSecondary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Voice button
                    IconButton(
                        onClick = {
                            if (voiceState == VoiceEngine.ListeningState.LISTENING) {
                                viewModel.voiceEngine.stopListening()
                            } else {
                                viewModel.voiceEngine.startListening()
                            }
                        }
                    ) {
                        Icon(
                            if (voiceState == VoiceEngine.ListeningState.LISTENING)
                                Icons.Filled.MicOff else Icons.Filled.Mic,
                            contentDescription = "Voice",
                            tint = if (voiceState == VoiceEngine.ListeningState.LISTENING)
                                ZeroColors.Error else ZeroColors.AccentCyan
                        )
                    }

                    // Text input
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text("Tell Zero what to do…", color = ZeroColors.TextMuted)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ZeroColors.AccentCyan,
                            unfocusedBorderColor = ZeroColors.BgTertiary,
                            cursorColor = ZeroColors.AccentCyan,
                            focusedTextColor = ZeroColors.TextPrimary,
                            unfocusedTextColor = ZeroColors.TextPrimary
                        ),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputText.isNotBlank()) {
                                    viewModel.sendMessage(inputText)
                                    inputText = ""
                                }
                            }
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Send button
                    FilledIconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                            }
                        },
                        enabled = inputText.isNotBlank() && !isProcessing,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = ZeroColors.AccentCyan,
                            contentColor = ZeroColors.BgPrimary
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
}

/**
 * Individual message bubble.
 */
@Composable
fun MessageBubble(message: ChatViewModel.ChatMessage) {
    val isUser = message.isUser

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            // Zero avatar
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(ZeroColors.AccentCyan, ZeroColors.AccentTeal)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("Z", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            color = if (isUser) ZeroColors.UserBubble else ZeroColors.AgentBubble,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            modifier = Modifier
                .widthIn(max = 300.dp)
                .then(
                    if (!isUser && !message.isThinking)
                        Modifier.border(
                            width = 1.dp,
                            color = ZeroColors.AccentCyan.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(
                                topStart = 16.dp, topEnd = 16.dp,
                                bottomStart = 4.dp, bottomEnd = 16.dp
                            )
                        )
                    else Modifier
                )
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = if (message.isThinking) ZeroColors.TextSecondary else ZeroColors.TextPrimary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}
