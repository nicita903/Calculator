package com.example.calculator.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.PressInteraction
import android.content.ClipData
import com.example.calculator.feedback.*
import com.example.calculator.ui.components.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import kotlinx.coroutines.launch
import com.example.calculator.CalculatorState
import com.example.calculator.CalculatorViewModel
import com.example.calculator.ui.theme.TerminalColors as C
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

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
            Keypad(Modifier.weight(1f).fillMaxHeight(), ::press, feedback)
        } else Column(Modifier.fillMaxSize()) {
            display(Modifier.weight(1f).fillMaxWidth())
            Keypad(Modifier.fillMaxWidth().weight(if(compact) 2.2f else 2f), ::press, feedback)
        }
    }
}

@Composable
private fun Keypad(modifier: Modifier, onPress: (String) -> Unit, feedback: (FeedbackKind) -> Unit) {
    val keys = remember { listOf(listOf("AC", "(", ")", "÷"), listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "−"), listOf("1", "2", "3", "+"), listOf("±", "0", ".", "=")) }
    BoxWithConstraints(modifier) {
        val gap = if(maxHeight < 280.dp) 6.dp else 10.dp
        val font = if(maxHeight < 280.dp) 23.sp else 30.sp
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(gap)) {
            keys.forEach { row ->
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach { key ->
                        val interaction = remember { MutableInteractionSource() }
                        val pressed by interaction.collectIsPressedAsState()
                        val scale by animateFloatAsState(if(pressed) .96f else 1f, tween(85), label = "Key press")
                        val latestFeedback by rememberUpdatedState(feedback)
                        LaunchedEffect(interaction) {
                            interaction.interactions.collect { event ->
                                if(event is PressInteraction.Press) latestFeedback(when(key) {
                                    "=" -> FeedbackKind.Equals; "AC" -> FeedbackKind.Clear
                                    in "0".."9", "." -> FeedbackKind.Number
                                    else -> FeedbackKind.Operator
                                })
                            }
                        }
                        Button(onClick = { onPress(key) }, modifier = Modifier.weight(1f).fillMaxHeight().scale(scale),
                            interactionSource = interaction, shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if(key == "=") C.Blue else C.Button,
                                contentColor = if(key in listOf("AC", "÷", "×", "−", "+")) C.Blue else C.Text)) {
                            Text(key, fontSize = font, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
