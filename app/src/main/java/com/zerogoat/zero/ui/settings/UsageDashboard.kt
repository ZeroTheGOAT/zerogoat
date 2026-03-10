package com.zerogoat.zero.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerogoat.zero.storage.ConversationMemory
import com.zerogoat.zero.storage.TaskHistory
import com.zerogoat.zero.ui.theme.ZeroColors

/**
 * Token usage dashboard — shows cost, token breakdown, and task history.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageDashboard(onBack: () -> Unit) {
    val context = LocalContext.current
    val taskHistory = remember { TaskHistory(context) }
    val memory = remember { ConversationMemory(context) }
    val tasks = remember { taskHistory.getAll().take(50) }
    val sessions = remember { memory.getAllSessions() }

    var totalTokens by remember { mutableIntStateOf(0) }
    var totalTasks by remember { mutableIntStateOf(tasks.size) }
    var totalSessions by remember { mutableIntStateOf(sessions.size) }

    LaunchedEffect(Unit) {
        totalTokens = sessions.sumOf { it.totalTokens }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Usage Dashboard", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ZeroColors.BgPrimary)
            )
        },
        containerColor = ZeroColors.BgPrimary
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Summary Cards
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UsageCard("Total Tokens", formatNumber(totalTokens), "📊", Modifier.weight(1f))
                    UsageCard("Tasks", "$totalTasks", "✅", Modifier.weight(1f))
                    UsageCard("Sessions", "$totalSessions", "💬", Modifier.weight(1f))
                }
            }

            // Estimated cost
            item {
                val estimatedCost = totalTokens * 0.15 / 1_000_000  // Rough avg
                Surface(color = ZeroColors.Surface, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Estimated Cost", color = ZeroColors.TextSecondary, fontSize = 12.sp)
                        Text(
                            "\$${String.format("%.4f", estimatedCost)}",
                            color = ZeroColors.AccentCyan, fontSize = 28.sp, fontWeight = FontWeight.Bold
                        )
                        Text("Based on average model pricing", color = ZeroColors.TextMuted, fontSize = 11.sp)
                    }
                }
            }

            // Recent sessions
            item {
                Text("Recent Sessions", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ZeroColors.AccentCyan,
                    modifier = Modifier.padding(top = 8.dp))
            }

            for (session in sessions.take(10)) {
                item {
                    Surface(color = ZeroColors.Surface, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(session.name, color = ZeroColors.TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Text(
                                    "${session.messageCount} msgs · ${formatNumber(session.totalTokens)} tokens",
                                    color = ZeroColors.TextSecondary, fontSize = 12.sp
                                )
                            }
                            Text(
                                formatTimestamp(session.updatedAt),
                                color = ZeroColors.TextMuted, fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Recent tasks
            item {
                Text("Recent Tasks", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ZeroColors.AccentCyan,
                    modifier = Modifier.padding(top = 8.dp))
            }

            for (task in tasks.take(10)) {
                item {
                    Surface(color = ZeroColors.Surface, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(task.command, color = ZeroColors.TextPrimary, fontSize = 13.sp, maxLines = 2)
                            Row {
                                Text(
                                    "${task.steps} steps · ${if (task.success) "✅" else "❌"}",
                                    color = if (task.success) ZeroColors.AccentGreen else ZeroColors.AccentRed,
                                    fontSize = 11.sp
                                )
                                Spacer(Modifier.weight(1f))
                                Text(formatTimestamp(task.timestamp), color = ZeroColors.TextMuted, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun UsageCard(title: String, value: String, emoji: String, modifier: Modifier = Modifier) {
    Surface(color = ZeroColors.Surface, shape = RoundedCornerShape(16.dp), modifier = modifier) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 20.sp)
            Text(value, fontWeight = FontWeight.Bold, color = ZeroColors.TextPrimary, fontSize = 18.sp)
            Text(title, color = ZeroColors.TextSecondary, fontSize = 11.sp)
        }
    }
}

fun formatNumber(n: Int): String = when {
    n >= 1_000_000 -> "${String.format("%.1f", n / 1_000_000.0)}M"
    n >= 1_000 -> "${String.format("%.1f", n / 1_000.0)}K"
    else -> "$n"
}

fun formatTimestamp(ms: Long): String {
    val diff = System.currentTimeMillis() - ms
    return when {
        diff < 60_000 -> "just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        else -> "${diff / 86_400_000}d ago"
    }
}
