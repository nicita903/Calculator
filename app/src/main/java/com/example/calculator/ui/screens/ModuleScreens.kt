package com.example.calculator.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calculator.data.HistoryEntry
import com.example.calculator.ui.navigation.Destination
import com.example.calculator.ui.navigation.TerminalIcon
import com.example.calculator.ui.theme.TerminalColors as C

@Composable
fun TerminalHeader(title: String, onMenu: () -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onMenu) { androidx.compose.foundation.layout.Box {
                TerminalIcon(-1, C.Secondary, Modifier.semantics { contentDescription = "Open navigation drawer" })
            } }
            Text("calculator@local:~$", fontFamily = FontFamily.Monospace, color = C.Secondary, fontSize = 13.sp)
        }
        Row(Modifier.padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("> ", color = C.Blue, fontFamily = FontFamily.Monospace, fontSize = 24.sp)
            Text(title, color = C.Text, fontSize = 24.sp, maxLines = 1, modifier = Modifier.weight(1f, fill = false), overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text("_", color = C.Blue, fontFamily = FontFamily.Monospace, fontSize = 24.sp)
        }
    }
}

@Composable
fun PlannedScreen(destination: Destination) {
    Column(Modifier.padding(top = 24.dp)) {
        Text("MODULE / PLANNED", color = C.Blue, fontFamily = FontFamily.Monospace)
        Text(when(destination) {
            Destination.Scientific -> "Scientific functions are not implemented yet."
            Destination.Solver -> "Verified equations and step-by-step explanations are planned."
            else -> "Interactive function plotting is planned."
        }, Modifier.padding(top = 12.dp), color = C.Secondary)
    }
}

@Composable
fun HistoryScreen(entries: List<HistoryEntry>, onRecall: (String) -> Unit, onClear: () -> Unit) {
    TextButton(onClick = onClear, enabled = entries.isNotEmpty()) { Text("Clear history") }
    LazyColumn(Modifier.fillMaxSize()) {
        if(entries.isEmpty()) item { Text("No local calculations yet.", color = C.Secondary) }
        items(entries, key = { it.id }) { entry ->
            Column(Modifier.fillMaxWidth().clickable { onRecall(entry.expression) }.padding(vertical = 14.dp)) {
                Text(entry.expression, color = C.Secondary)
                Text(entry.result, color = C.Blue, fontSize = 24.sp)
                Text("Tap to recall", color = C.Secondary, fontSize = 11.sp)
            }
            HorizontalDivider(color = C.Border)
        }
    }
}

@Composable
fun SettingsScreen(haptics: Boolean, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text("Key feedback"); Text("Optional haptic feedback", color = C.Secondary, fontSize = 12.sp) }
        Switch(checked = haptics, onCheckedChange = { onToggle() })
    }
}

@Composable
fun AboutScreen(version: String) {
    Column(Modifier.padding(top = 20.dp)) {
        Text("CALCULATOR / v$version", color = C.Blue, fontFamily = FontFamily.Monospace)
        Text("Offline mathematics. No accounts, ads or telemetry.", Modifier.padding(top = 12.dp), color = C.Secondary)
        Text("Percent divides the preceding value by 100. Integer powers use ^; non-integer powers are not supported.", Modifier.padding(top = 12.dp), color = C.Secondary)
    }
}
