package com.example.calculator.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.example.calculator.CalculatorState
import com.example.calculator.CalculatorViewModel
import com.example.calculator.ui.theme.TerminalColors as C

@Composable
fun CalculatorScreen(state: CalculatorState, vm: CalculatorViewModel, modifier: Modifier = Modifier) {
    var editor by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(state.expression, TextRange(state.expression.length)))
    }
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(state.expression) {
        if(editor.text != state.expression) editor = TextFieldValue(state.expression, TextRange(state.expression.length))
    }
    fun insert(text: String) {
        val start = editor.selection.min
        val next = editor.text.replaceRange(start, editor.selection.max, text)
        if(next.length <= 512) {
            editor = TextFieldValue(next, TextRange(start + text.length))
            vm.edit(next)
        }
    }
    fun press(key: String) {
        if(state.haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        when(key) {
            "AC" -> vm.edit("")
            "=" -> vm.calculate()
            "±" -> {
                val next = if(editor.text.isEmpty()) "-" else "-(" + editor.text + ")"
                if(next.length <= 512) { editor = TextFieldValue(next, TextRange(next.length)); vm.edit(next) }
            }
            "⌫" -> {
                val start = editor.selection.min
                if(start != editor.selection.max) insert("")
                else if(start > 0) {
                    val next = editor.text.removeRange(start - 1, start)
                    editor = TextFieldValue(next, TextRange(start - 1)); vm.edit(next)
                }
            }
            else -> insert(key)
        }
    }
    BoxWithConstraints(modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        val landscape = maxWidth > 560.dp && maxHeight < 420.dp
        val compact = maxHeight < 470.dp
        val display: @Composable (Modifier) -> Unit = { displayModifier ->
            Column(displayModifier, verticalArrangement = Arrangement.Center) {
                BasicTextField(editor, onValueChange = { if(it.text.length <= 512) { editor = it; vm.edit(it.text) } },
                    modifier = Modifier.fillMaxWidth().padding(vertical = if(compact) 4.dp else 12.dp),
                    singleLine = true, cursorBrush = SolidColor(C.Blue),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, showKeyboardOnFocus = false),
                    textStyle = LocalTextStyle.current.copy(color = C.Text, fontSize = if(compact) 25.sp else 32.sp, textAlign = TextAlign.End),
                    decorationBox = { field -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        if(editor.text.isEmpty()) Text("0", color = C.Secondary, fontSize = if(compact) 25.sp else 32.sp)
                        field()
                    } })
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
                    TextButton(onClick = { clipboard.setText(AnnotatedString((if(state.approximate) "≈ " else "") + state.result)) }, contentPadding = PaddingValues(4.dp)) { Text("Copy", fontSize = 12.sp) }
                    TextButton(onClick = { clipboard.getText()?.text?.let(::insert) }, contentPadding = PaddingValues(4.dp)) { Text("Paste", fontSize = 12.sp) }
                    TextButton(onClick = { press("⌫") }, contentPadding = PaddingValues(4.dp)) { Text("⌫", fontSize = 18.sp) }
                }
            }
        }
        if(landscape) Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            display(Modifier.weight(1f).fillMaxHeight())
            Keypad(Modifier.weight(1f).fillMaxHeight(), ::press)
        } else Column(Modifier.fillMaxSize()) {
            display(Modifier.weight(1f).fillMaxWidth())
            Keypad(Modifier.fillMaxWidth().weight(if(compact) 2.2f else 2f), ::press)
        }
    }
}

@Composable
private fun Keypad(modifier: Modifier, onPress: (String) -> Unit) {
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
                        val scale by animateFloatAsState(if(pressed) .96f else 1f, label = "Key press")
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
