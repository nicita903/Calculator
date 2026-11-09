package com.example.calculator

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.example.calculator.ui.components.deleteAtSelection
import com.example.calculator.ui.components.insertAtSelection
import org.junit.Assert.*
import org.junit.Test

class ExpressionEditingTest {
    @Test fun insertsAtCursor() {
        val next = insertAtSelection(TextFieldValue("12+4", TextRange(3)), "3")
        assertEquals("12+34", next.text)
        assertEquals(TextRange(4), next.selection)
    }
    @Test fun replacesReversedSelection() {
        val next = insertAtSelection(TextFieldValue("12345", TextRange(4, 1)), "9")
        assertEquals("195", next.text); assertEquals(TextRange(2), next.selection)
    }
    @Test fun deletesSelectionAndCharacters() {
        assertEquals("15", deleteAtSelection(TextFieldValue("12345", TextRange(1, 4))).text)
        val next = deleteAtSelection(TextFieldValue("12+4", TextRange(2)))
        assertEquals("1+4", next.text); assertEquals(TextRange(1), next.selection)
        assertEquals("12", deleteAtSelection(TextFieldValue("12", TextRange.Zero)).text)
    }
    @Test fun rapidEditsKeepEveryDigit() {
        var value = TextFieldValue("")
        repeat(100) { value = insertAtSelection(value, "7") }
        assertEquals("7".repeat(100), value.text); assertEquals(TextRange(100), value.selection)
    }
    @Test fun enforcesLengthWithoutChangingCursor() {
        val original = TextFieldValue("1".repeat(512), TextRange(12))
        assertEquals(original, insertAtSelection(original, "2"))
    }
    @Test fun supplementaryCharacterDeletesAsOne() {
        assertEquals("12", deleteAtSelection(TextFieldValue("1\uD83D\uDE002", TextRange(3))).text)
    }
}
