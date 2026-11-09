package com.example.calculator.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.example.calculator.ui.theme.TerminalColors as C

/** Text and hit testing only: no focus target, InputConnection, or platform text field. */
@Composable
fun ExpressionEditor(value: TextFieldValue, onSelection: (TextFieldValue) -> Unit, compact: Boolean) {
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val scroll = rememberScrollState()
    val current by rememberUpdatedState(value)
    val update by rememberUpdatedState(onSelection)
    val transition = rememberInfiniteTransition(label = "Expression cursor")
    val cursorAlpha = transition.animateFloat(1f, .2f,
        infiniteRepeatable(tween(520), RepeatMode.Reverse), label = "Cursor opacity")
    LaunchedEffect(value.text, value.selection, layout, scroll.viewportSize) {
        val result = layout ?: return@LaunchedEffect
        val caret = result.getCursorRect(value.selection.end.coerceIn(0, value.text.length))
        val left = scroll.value.toFloat()
        val right = left + scroll.viewportSize
        val target = when {
            caret.right + 12 > right -> (caret.right + 12 - scroll.viewportSize).toInt()
            caret.left - 12 < left -> (caret.left - 12).toInt()
            else -> scroll.value
        }.coerceIn(0, scroll.maxValue)
        scroll.animateScrollTo(target, tween(90))
    }
    BoxWithConstraints(Modifier.fillMaxWidth().padding(vertical = if(compact) 4.dp else 8.dp)
        .semantics(mergeDescendants = true) {
            contentDescription = "Mathematical expression"
            text = AnnotatedString(value.text)
            textSelectionRange = value.selection
            setSelection { start, end, _ ->
                if(start in 0..value.text.length && end in 0..value.text.length) {
                    onSelection(value.copy(selection = TextRange(start, end))); true
                } else false
            }
        }) {
        val viewport = maxWidth
        Box(Modifier.fillMaxWidth().horizontalScroll(scroll)) {
            Text(value.text.ifEmpty { " " }, color = C.Text, fontSize = if(compact) 25.sp else 32.sp,
                textAlign = TextAlign.End, softWrap = false, maxLines = 1,
                onTextLayout = { layout = it },
                modifier = Modifier.widthIn(min = viewport).padding(horizontal = 6.dp).heightIn(min = 48.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { position ->
                            val offset = layout?.getOffsetForPosition(position) ?: 0
                            update(current.copy(selection = TextRange(offset.coerceIn(0, current.text.length))))
                        }, onLongPress = {
                            update(current.copy(selection = TextRange(0, current.text.length)))
                        })
                    }.drawWithContent {
                        val result = layout
                        if(result != null && !value.selection.collapsed) {
                            drawPath(result.getPathForRange(value.selection.min, value.selection.max), C.Blue.copy(alpha = .25f))
                        }
                        drawContent()
                        if(result != null && value.selection.collapsed) {
                            val caret = result.getCursorRect(value.selection.end.coerceIn(0, value.text.length))
                            drawLine(C.Blue.copy(alpha = cursorAlpha.value), Offset(caret.left, caret.top),
                                Offset(caret.left, caret.bottom), 1.5.dp.toPx())
                        }
                    })
        }
    }
}

fun insertAtSelection(value: TextFieldValue, inserted: String): TextFieldValue {
    val next = value.text.replaceRange(value.selection.min, value.selection.max, inserted)
    if(next.length > 512) return value
    return TextFieldValue(next, TextRange(value.selection.min + inserted.length))
}
fun deleteAtSelection(value: TextFieldValue): TextFieldValue {
    if(!value.selection.collapsed) return insertAtSelection(value, "")
    val end = value.selection.end
    if(end == 0) return value
    // Keep pasted supplementary characters intact even though the parser rejects them.
    val start = Character.offsetByCodePoints(value.text, end, -1)
    return TextFieldValue(value.text.removeRange(start, end), TextRange(start))
}
