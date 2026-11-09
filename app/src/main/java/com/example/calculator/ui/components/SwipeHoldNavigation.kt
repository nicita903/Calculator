package com.example.calculator.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun SwipeHoldNavigation(
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onHoldConfirmed: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    val currentOnOpenDrawer = rememberUpdatedState(onOpenDrawer)
    val currentOnHoldConfirmed = rememberUpdatedState(onHoldConfirmed)

    var targetScale by remember {
        mutableFloatStateOf(1f)
    }

    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(
            durationMillis = 200
        ),
        label = "CalculatorZoom"
    )

    val swipeThreshold = with(density) { 55.dp.toPx() }
    val cancelDistance = with(density) { 25.dp.toPx() }
    val verticalTolerance = with(density) { 40.dp.toPx() }

    Box(
        modifier = modifier.pointerInput(
            enabled,
            swipeThreshold,
            cancelDistance,
            verticalTolerance
        ) {
            if (!enabled) return@pointerInput

            coroutineScope {
                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false
                    )



                    var tracking = false
                    var confirmed = false
                    var cancelled = false

                    var holdJob: Job? = null

                    try {
                        while (true) {
                            val event = awaitPointerEvent()

                            // This gesture only tracks one finger.
                            if (event.changes.size > 1) {
                                cancelled = true
                                break
                            }

                            val change = event.changes
                                .firstOrNull {
                                    it.id == down.id
                                } ?: break

                            val dx =
                                change.position.x -
                                        down.position.x

                            val dy =
                                change.position.y -
                                        down.position.y

                            if (change.changedToUpIgnoreConsumed()) {

                                if (confirmed && !cancelled) {
                                    currentOnOpenDrawer.value()
                                }

                                break
                            }

                            if (change.isConsumed && !tracking) {
                                cancelled = true
                                break
                            }

                            // Ignore vertical gestures.
                            if (abs(dy) > verticalTolerance) {
                                cancelled = true
                                break
                            }

                            // Detect a right swipe.
                            if (!tracking) {
                                if (dx >= swipeThreshold) {
                                    tracking = true

                                    holdJob = launch {
                                        delay(350)

                                        confirmed = true
                                        targetScale = 0.94f

                                        val customHaptic =
                                            currentOnHoldConfirmed.value

                                        if (customHaptic != null) {
                                            customHaptic()
                                        } else {
                                            haptic.performHapticFeedback(
                                                HapticFeedbackType.LongPress
                                            )
                                        }
                                    }
                                }
                            } else {

                                // Moving back cancels the gesture.
                                if (
                                    dx <
                                    swipeThreshold - cancelDistance
                                ) {
                                    cancelled = true
                                    break
                                }

                                // Prevent the swipe from affecting
                                // the calculator underneath.
                                change.consume()
                            }
                        }
                    } finally {
                        holdJob?.cancel()
                        targetScale = 1f
                    }
                }
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                }
                .clip(
                    RoundedCornerShape(
                        if (animatedScale < 0.995f) {
                            20.dp
                        } else {
                            0.dp
                        }
                    )
                ),
            content = content
        )
    }
}