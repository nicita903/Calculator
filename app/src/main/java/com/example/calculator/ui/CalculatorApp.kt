package com.example.calculator.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalView
import com.example.calculator.feedback.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calculator.CalculatorViewModel
import com.example.calculator.ui.navigation.*
import com.example.calculator.ui.screens.*
import com.example.calculator.ui.theme.TerminalColors as C
import kotlinx.coroutines.launch

@Composable
fun CalculatorApp(vm: CalculatorViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableStateOf(Destination.Calculator) }
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val view = LocalView.current
    val haptics = remember(context.applicationContext) { HapticFeedbackManager(context.applicationContext) }
    DisposableEffect(haptics) { onDispose { haptics.close() } }
    val level by rememberUpdatedState(state.hapticIntensity)
    val feedback: (FeedbackKind) -> Unit = remember(haptics, view) { { kind -> haptics.perform(view, level, kind) } }
    LaunchedEffect(drawer.currentValue) {
        if(drawer.currentValue == DrawerValue.Open) feedback(FeedbackKind.Navigation)
    }
    val version = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0" }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val drawerWidth = (maxWidth * .84f).coerceAtMost(360.dp)
        ModalNavigationDrawer(
            drawerState = drawer, gesturesEnabled = true,
            scrimColor = C.Background.copy(alpha = .7f),
            drawerContent = {
                ModalDrawerSheet(Modifier.width(drawerWidth), drawerContainerColor = C.Background) {
                    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(16.dp)) {
                        Row(Modifier.padding(vertical = 12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text("[ >_ ]", color = C.Blue, fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.width(8.dp))
                            Text("calculator@local:~$", color = C.Secondary, fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.labelMedium, maxLines = 1)
                        }
                        Text("> MODULES", color = C.Secondary, fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
                        HorizontalDivider(Modifier.padding(bottom = 8.dp), color = C.Border)
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                            Destination.entries.forEachIndexed { index, destination ->
                                if (destination == Destination.Settings) HorizontalDivider(Modifier.padding(vertical = 12.dp), color = C.Border)
                                val activeColor by animateColorAsState(if(selected == destination) C.Blue else C.Secondary,
                                    tween(100), label = "Module selection")
                                NavigationDrawerItem(
                                    modifier = Modifier.heightIn(min = 52.dp),
                                    label = { Text(if(destination == Destination.Scientific) "Scientific" else destination.title, style = MaterialTheme.typography.bodyMedium) },
                                    icon = { TerminalIcon(index, activeColor) },
                                    selected = selected == destination,
                                    onClick = { feedback(FeedbackKind.Navigation); selected = destination; scope.launch { drawer.close() } },
                                    colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = C.DeepBlue,
                                        selectedTextColor = C.Blue, unselectedTextColor = C.Text),
                                )
                            }
                        }
                        Text("LOCAL ONLY  /  v$version", Modifier.padding(top = 16.dp), color = C.Secondary,
                            fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                }
            },
        ) {
            Surface(Modifier.fillMaxSize(), color = C.Background) {
                Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 16.dp)) {
                    TerminalHeader(selected.title) { scope.launch { drawer.open() } }
                    when(selected) {
                        Destination.Calculator -> CalculatorScreen(state, vm, feedback, Modifier.weight(1f))
                        Destination.History -> HistoryScreen(state.history, { vm.edit(it); selected = Destination.Calculator }, vm::clearHistory)
                        Destination.Settings -> SettingsScreen(state.hapticIntensity, haptics.capabilityNote) { intensity -> vm.setHapticIntensity(intensity); haptics.perform(view, intensity, FeedbackKind.Number) }
                        Destination.About -> AboutScreen(version)
                        else -> PlannedScreen(selected)
                    }
                }
            }
        }
    }
    // Register after the drawer so its internal callback cannot shadow module navigation.
    BackHandler(enabled = drawer.isOpen || selected != Destination.Calculator) {
        if (drawer.isOpen) { feedback(FeedbackKind.Navigation); scope.launch { drawer.close() } }
        else selected = Destination.Calculator
    }

}
