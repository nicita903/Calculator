package com.example.calculator.ui.screens

import android.content.ClipData
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calculator.CalculatorState
import com.example.calculator.CalculatorViewModel
import com.example.calculator.feedback.FeedbackKind
import com.example.calculator.ui.components.ExpressionEditor
import com.example.calculator.ui.components.deleteAtSelection
import com.example.calculator.ui.components.insertAtSelection
import kotlinx.coroutines.launch
import com.example.calculator.ui.theme.TerminalColors as C

@Composable
fun CalculatorScreen(state: CalculatorState, vm: CalculatorViewModel, feedback: (FeedbackKind) -> Unit, modifier: Modifier = Modifier) {
    var editor by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(state.expression, TextRange(state.expression.length)))
    }
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        keyboardController?.hide()
    }
    LaunchedEffect(state.expression) {
        if(editor.text != state.expression) editor = TextFieldValue(state.expression, TextRange(state.expression.length))
    }
    fun insert(text: String) {
        val next = insertAtSelection(editor, text)
        if(next != editor) { editor = next; vm.edit(next.text) }
    }
    fun press(key: String) {
        when(key) {
            "AC" -> { editor = TextFieldValue(""); vm.edit("") }
            "=" -> vm.calculate()
            "±" -> {
                val next = if(editor.text.isEmpty()) "-" else "-(" + editor.text + ")"
                if(next.length <= 512) { editor = TextFieldValue(next, TextRange(next.length)); vm.edit(next) }
            }
            "⌫" -> {
                val next = deleteAtSelection(editor)
                editor = next; vm.edit(next.text)
            }
            else -> insert(key)
        }
    }
    BoxWithConstraints(modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        val landscape = maxWidth > 560.dp && maxHeight < 420.dp
        val compact = maxHeight < 470.dp
        val display: @Composable (Modifier) -> Unit = { displayModifier ->
            Column(displayModifier, verticalArrangement = Arrangement.Center) {
                ExpressionEditor(editor, { editor = it }, compact)
                val scroll = rememberScrollState()
                LaunchedEffect(state.result) { scroll.animateScrollTo(scroll.maxValue) }
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    Text((if(state.approximate) "≈ " else "") + state.result,
                        Modifier.horizontalScroll(scroll), color = C.Blue, maxLines = 1,
                        fontSize = if(compact) 30.sp else 42.sp)
                }
                Text(state.error ?: if(state.approximate) "≈ Rounded to 34 significant digits" else "Exact decimal",
                    Modifier.fillMaxWidth().padding(top = 4.dp).horizontalScroll(rememberScrollState()),
                    color = C.Secondary, fontSize = 11.sp, maxLines = 1, textAlign = TextAlign.End)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(onClick = vm::undo, contentPadding = PaddingValues(4.dp)) { Text("Undo", fontSize = 12.sp) }
                    TextButton(onClick = vm::redo, contentPadding = PaddingValues(4.dp)) { Text("Redo", fontSize = 12.sp) }
                    TextButton(onClick = { scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Result", (if(state.approximate) "≈ " else "") + state.result))) } }, contentPadding = PaddingValues(4.dp)) { Text("Copy", fontSize = 12.sp) }
                    TextButton(onClick = { scope.launch { clipboard.getClipEntry()?.clipData?.let { clip -> if(clip.itemCount > 0) insert(clip.getItemAt(0).coerceToText(context).toString()) } } }, contentPadding = PaddingValues(4.dp)) { Text("Paste", fontSize = 12.sp) }
                    TextButton(onClick = { feedback(FeedbackKind.Clear); press("⌫") }, contentPadding = PaddingValues(4.dp)) { Text("⌫", fontSize = 18.sp) }
                }
            }
        }
        if(landscape) Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            display(Modifier.weight(1f).fillMaxHeight())
            Keypad(Modifier.weight(1f).fillMaxHeight(), ::press, feedback = feedback)
        } else Column(Modifier.fillMaxSize()) {
            display(Modifier.weight(1f).fillMaxWidth())
            Keypad(Modifier.fillMaxWidth().weight(if(compact) 2.2f else 2f), ::press, feedback = feedback)
        }
    }
}

@Composable
private fun Keypad(
    modifier: Modifier,
    onPress: (String) -> Unit,
    feedback: (FeedbackKind) -> Unit

) {
    val keys = remember {
        listOf(
            listOf("AC", "(", ")", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "−"),
            listOf("1", "2", "3", "+"),
            listOf("±", "0", ".", "=")
        )
    }

    BoxWithConstraints(modifier) {

        val gap = if (maxHeight < 280.dp) 6.dp else 10.dp
        val font = if (maxHeight < 280.dp) 23.sp else 30.sp

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            keys.forEach { row ->

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    row.forEach { key ->

                        val interaction = remember {
                            MutableInteractionSource()
                        }

                        val pressed by interaction.collectIsPressedAsState()

                        val currentFeedback by rememberUpdatedState(feedback)
                        val currentKey by rememberUpdatedState(key)

                        LaunchedEffect(interaction) {
                            interaction.interactions.collect { event ->

                                if (event is PressInteraction.Press) {

                                    val feedbackType = when (currentKey) {
                                        "=" -> FeedbackKind.Equals
                                        "AC" -> FeedbackKind.Clear

                                        "+", "−", "×", "÷",
                                        "(", ")", "±" -> FeedbackKind.Operator

                                        else -> FeedbackKind.Number
                                    }

                                    currentFeedback(feedbackType)
                                }
                            }
                        }

                        val scale by animateFloatAsState(
                            targetValue = if (pressed) 0.95f else 1f,
                            animationSpec = tween(
                                durationMillis = 45
                            ),
                            label = "Key press"
                        )

                        // Invisible touch area occupies the full cell.
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = interaction,
                                    indication = null
                                ) {
                                    onPress(key)
                                }
                                .padding(gap / 2),
                            contentAlignment = Alignment.Center
                        ) {

                            // Visible button stays smaller.
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .scale(scale)
                                    .background(
                                        color = if (key == "=") {
                                            C.Blue
                                        } else {
                                            C.Button
                                        },
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {

                                Text(
                                    text = key,
                                    fontSize = font,
                                    color = if (
                                        key in listOf(
                                            "AC", "÷", "×", "−", "+"
                                        )
                                    ) {
                                        C.Blue
                                    } else {
                                        C.Text
                                    },
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
