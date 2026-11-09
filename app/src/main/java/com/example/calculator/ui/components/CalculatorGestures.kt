package com.example.calculator.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.shape.RoundedCornerShape

import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import kotlin.math.abs

private enum class GestureDirection {
    NONE,
    LEFT,
    RIGHT
}

@Composable
fun CalculatorGestures(
    modifier: Modifier = Modifier,

    enabled: Boolean = true,
    canGoBack: Boolean = false,

    onOpenMenu: () -> Unit,
    onGoBack: () -> Unit,

    onHaptic: () -> Unit,

    swipeDistance: Dp = 28.dp,
    holdTime: Long = 180L,

    content: @Composable BoxScope.() -> Unit
) {

    val density = LocalDensity.current

    val swipeThreshold = with(density) {
        swipeDistance.toPx()
    }

    val reverseTolerance = with(density) {
        14.dp.toPx()
    }

    val verticalTolerance = with(density) {
        56.dp.toPx()
    }

    val currentOpenMenu = rememberUpdatedState(onOpenMenu)
    val currentGoBack = rememberUpdatedState(onGoBack)
    val currentHaptic = rememberUpdatedState(onHaptic)

    var gestureConfirmed by remember {
        mutableStateOf(false)
    }

    val scale by animateFloatAsState(
        targetValue = if (gestureConfirmed) {
            0.95f
        } else {
            1f
        },

        animationSpec = tween(
            durationMillis = 160,
            easing = FastOutSlowInEasing
        ),

        label = "CalculatorGestureScale"
    )

    Box(
        modifier = modifier.pointerInput(
            enabled,
            canGoBack,
            swipeThreshold,
            reverseTolerance,
            verticalTolerance,
            holdTime
        ) {

            if (!enabled) {
                gestureConfirmed = false
                return@pointerInput
            }

            coroutineScope {

                awaitEachGesture {

                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial
                    )

                    var direction = GestureDirection.NONE

                    var confirmed = false

                    var holdJob: Job? = null

                    // Position where the swipe activates.
                    var activationX = down.position.x

                    try {

                        while (true) {

                            val event = awaitPointerEvent(
                                PointerEventPass.Initial
                            )

                            // Ignore multitouch gestures.
                            if (event.changes.size != 1) {
                                break
                            }

                            val change = event.changes.firstOrNull {
                                it.id == down.id
                            } ?: break

                            val dx =
                                change.position.x -
                                        down.position.x

                            val dy =
                                change.position.y -
                                        down.position.y

                            // Finger released.
                            if (!change.pressed) {

                                if (confirmed) {

                                    change.consume()

                                    when (direction) {

                                        GestureDirection.RIGHT -> {
                                            currentOpenMenu.value()
                                        }

                                        GestureDirection.LEFT -> {
                                            currentGoBack.value()
                                        }

                                        GestureDirection.NONE -> Unit
                                    }
                                }

                                break
                            }

                            // Ignore predominantly vertical gestures.
                            if (
                                abs(dy) > verticalTolerance &&
                                direction == GestureDirection.NONE
                            ) {
                                break
                            }

                            // Detect gesture direction.
                            if (
                                direction == GestureDirection.NONE
                            ) {

                                val horizontalMovement = abs(dx)
                                val verticalMovement = abs(dy)

                                if (
                                    horizontalMovement >= swipeThreshold &&
                                    horizontalMovement >
                                    verticalMovement * 1.3f
                                ) {

                                    val detectedDirection =
                                        if (dx > 0f) {
                                            GestureDirection.RIGHT
                                        } else {
                                            GestureDirection.LEFT
                                        }

                                    if (
                                        detectedDirection ==
                                        GestureDirection.LEFT &&
                                        !canGoBack
                                    ) {
                                        break
                                    }

                                    direction = detectedDirection

                                    activationX = change.position.x

                                    // Cancel underlying button clicks
                                    // after recognizing a deliberate swipe.
                                    change.consume()

                                    holdJob = launch {

                                        delay(holdTime)

                                        confirmed = true

                                        gestureConfirmed = true

                                        currentHaptic.value()
                                    }
                                }

                            } else {

                                // Cancel if the finger reverses direction
                                // beyond the allowed tolerance.
                                val movementAfterActivation =
                                    change.position.x - activationX

                                val reversed = when (direction) {

                                    GestureDirection.RIGHT -> {
                                        movementAfterActivation <
                                                -reverseTolerance
                                    }

                                    GestureDirection.LEFT -> {
                                        movementAfterActivation >
                                                reverseTolerance
                                    }

                                    GestureDirection.NONE -> false
                                }

                                if (
                                    reversed ||
                                    abs(dy) > verticalTolerance
                                ) {
                                    break
                                }

                                // Once recognized, prevent the gesture
                                // from activating calculator buttons.
                                change.consume()
                            }
                        }

                    } finally {

                        holdJob?.cancel()

                        gestureConfirmed = false
                    }
                }
            }
        }
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {

                    scaleX = scale
                    scaleY = scale

                    transformOrigin = TransformOrigin.Center

                    clip = scale < 0.999f

                    shape = RoundedCornerShape(20.dp)
                },

            content = content
        )
    }
}