package com.example.calculator.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

import com.example.calculator.CalculatorViewModel
import com.example.calculator.feedback.*

import com.example.calculator.ui.components.CalculatorGestures

import com.example.calculator.ui.navigation.*
import com.example.calculator.ui.screens.*
import com.example.calculator.ui.theme.TerminalColors as C

import kotlinx.coroutines.launch

@Composable
fun CalculatorApp(
    vm: CalculatorViewModel = viewModel()
) {
    // -------------------------------------------------
    // Application state
    // -------------------------------------------------

    val state by vm.state.collectAsStateWithLifecycle()

    var selected by rememberSaveable {
        mutableStateOf(Destination.Calculator)
    }

    // Remember previously visited modules.
    val moduleHistory = remember {
        mutableStateListOf<Destination>()
    }

    fun navigateTo(destination: Destination) {
        if (destination != selected) {
            moduleHistory.add(selected)
            selected = destination
        }
    }

    fun navigateBack() {
        if (moduleHistory.isNotEmpty()) {
            selected = moduleHistory.removeAt(
                moduleHistory.lastIndex
            )
        }
    }

    // -------------------------------------------------
    // Navigation drawer
    // -------------------------------------------------

    val drawer = rememberDrawerState(
        initialValue = DrawerValue.Closed
    )

    val scope = rememberCoroutineScope()

    // -------------------------------------------------
    // Android context and haptics
    // -------------------------------------------------

    val context = LocalContext.current
    val view = LocalView.current

    val haptics = remember(context.applicationContext) {
        HapticFeedbackManager(context.applicationContext)
    }

    DisposableEffect(haptics) {
        onDispose {
            haptics.close()
        }
    }

    val level by rememberUpdatedState(
        state.hapticIntensity
    )

    val feedback: (FeedbackKind) -> Unit =
        remember(haptics, view) {
            { kind ->
                haptics.perform(
                    view,
                    level,
                    kind
                )
            }
        }

    // -------------------------------------------------
    // Application version
    // -------------------------------------------------

    val version = remember {
        context.packageManager
            .getPackageInfo(
                context.packageName,
                0
            )
            .versionName ?: "1.0"
    }

    // -------------------------------------------------
    // Main UI
    // -------------------------------------------------

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val drawerWidth = (maxWidth * 0.84f)
            .coerceAtMost(360.dp)

        ModalNavigationDrawer(
            drawerState = drawer,

            // Standard drawer gestures only work
            // when the menu is already open.
            gesturesEnabled = drawer.isOpen,

            scrimColor = C.Background.copy(
                alpha = 0.7f
            ),

            drawerContent = {

                ModalDrawerSheet(
                    modifier = Modifier.width(drawerWidth),
                    drawerContainerColor = C.Background
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(
                                WindowInsets.safeDrawing
                            )
                            .padding(16.dp)
                    ) {

                        // --------------------------------
                        // Terminal header
                        // --------------------------------

                        Row(
                            modifier = Modifier.padding(
                                vertical = 12.dp
                            ),

                            verticalAlignment =
                                androidx.compose.ui.Alignment.CenterVertically
                        ) {

                            Text(
                                text = "[ >_ ]",
                                color = C.Blue,
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.labelMedium
                            )

                            Spacer(
                                modifier = Modifier.width(8.dp)
                            )

                            Text(
                                text = "calculator@local:~$",
                                color = C.Secondary,
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1
                            )
                        }

                        // --------------------------------
                        // Module section
                        // --------------------------------

                        Text(
                            text = "> MODULES",
                            color = C.Secondary,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(
                                top = 8.dp,
                                bottom = 12.dp
                            )
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(
                                bottom = 8.dp
                            ),
                            color = C.Border
                        )

                        // --------------------------------
                        // Navigation items
                        // --------------------------------

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(
                                    rememberScrollState()
                                )
                        ) {

                            Destination.entries.forEachIndexed {
                                    index, destination ->

                                if (
                                    destination ==
                                    Destination.Settings
                                ) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(
                                            vertical = 12.dp
                                        ),
                                        color = C.Border
                                    )
                                }

                                val activeColor by animateColorAsState(
                                    targetValue = if (
                                        selected == destination
                                    ) {
                                        C.Blue
                                    } else {
                                        C.Secondary
                                    },

                                    animationSpec = tween(100),
                                    label = "ModuleSelection"
                                )

                                NavigationDrawerItem(
                                    modifier = Modifier.heightIn(
                                        min = 52.dp
                                    ),

                                    label = {
                                        Text(
                                            text = if (
                                                destination ==
                                                Destination.Scientific
                                            ) {
                                                "Scientific"
                                            } else {
                                                destination.title
                                            },

                                            style =
                                                MaterialTheme.typography.bodyMedium
                                        )
                                    },

                                    icon = {
                                        TerminalIcon(
                                            index,
                                            activeColor
                                        )
                                    },

                                    selected = selected == destination,

                                    onClick = {

                                        feedback(
                                            FeedbackKind.Navigation
                                        )

                                        navigateTo(destination)

                                        scope.launch {
                                            drawer.close()
                                        }
                                    },

                                    colors =
                                        NavigationDrawerItemDefaults.colors(
                                            selectedContainerColor = C.DeepBlue,
                                            selectedTextColor = C.Blue,
                                            unselectedTextColor = C.Text
                                        )
                                )
                            }
                        }

                        // --------------------------------
                        // Footer
                        // --------------------------------

                        Text(
                            text = "LOCAL ONLY  /  v$version",
                            modifier = Modifier.padding(
                                top = 16.dp
                            ),
                            color = C.Secondary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        ) {

            CalculatorGestures(
                modifier = Modifier.fillMaxSize(),

                enabled = drawer.isClosed,

                canGoBack = moduleHistory.isNotEmpty(),

                swipeDistance = 28.dp,

                holdTime = 130L,

                onOpenMenu = {

                    scope.launch {
                        drawer.open()
                    }
                },

                onGoBack = {

                    navigateBack()
                },

                onHaptic = {

                    feedback(
                        FeedbackKind.Navigation
                    )
                }
            ) {

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = C.Background
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(
                                WindowInsets.safeDrawing
                            )
                            .padding(horizontal = 16.dp)
                    ) {

                        TerminalHeader(selected.title) {

                            feedback(FeedbackKind.Navigation)

                            scope.launch {
                                drawer.open()
                            }
                        }

                        when (selected) {

                            Destination.Calculator -> {

                                CalculatorScreen(
                                    state,
                                    vm,
                                    feedback,
                                    Modifier.weight(1f)
                                )
                            }

                            Destination.History -> {

                                HistoryScreen(
                                    state.history,

                                    { expression ->

                                        vm.edit(expression)

                                        navigateTo(
                                            Destination.Calculator
                                        )
                                    },

                                    vm::clearHistory
                                )
                            }

                            Destination.Settings -> {

                                SettingsScreen(
                                    state.hapticIntensity,
                                    haptics.capabilityNote
                                ) { intensity ->

                                    vm.setHapticIntensity(intensity)

                                    haptics.perform(
                                        view,
                                        intensity,
                                        FeedbackKind.Number
                                    )
                                }
                            }

                            Destination.About -> {

                                AboutScreen(version)
                            }

                            else -> {

                                PlannedScreen(selected)
                            }
                        }
                    }
                }
            }
        }
    }

    // -------------------------------------------------
    // Android Back navigation
    // -------------------------------------------------

    BackHandler(
        enabled = drawer.isOpen ||
                moduleHistory.isNotEmpty()
    ) {

        if (drawer.isOpen) {

            feedback(
                FeedbackKind.Navigation
            )

            scope.launch {
                drawer.close()
            }

        } else {

            feedback(
                FeedbackKind.Navigation
            )

            navigateBack()
        }
    }
}