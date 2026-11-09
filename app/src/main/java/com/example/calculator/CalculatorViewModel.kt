package com.example.calculator

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import com.example.calculator.data.*
import com.calcterminal.math.ArithmeticEngine
import com.example.calculator.feedback.HapticIntensity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

private data class ComputedResult(val text: String, val approximate: Boolean)

data class CalculatorState(val expression: String = "", val result: String = "0", val error: String? = null,
 val approximate: Boolean = false, val history: List<HistoryEntry> = emptyList(), val hapticIntensity: HapticIntensity = HapticIntensity.Strong)
class CalculatorViewModel(app: Application, private val saved: SavedStateHandle) : AndroidViewModel(app) {
 private val db = Room.databaseBuilder(app, HistoryDatabase::class.java, "history.db").build()
 private val engine = ArithmeticEngine()
 private val mutable = MutableStateFlow(CalculatorState(expression = saved["expression"] ?: ""))
 val state = mutable.asStateFlow()
 private val undo = ArrayDeque<String>()
 private val redo = ArrayDeque<String>()
 private var preview: Job? = null
 private val settings = SettingsRepository(app)
 init {
  viewModelScope.launch { settings.intensity.collect { level -> mutable.update { it.copy(hapticIntensity = level) } } }
  viewModelScope.launch { refresh() }
  evaluatePreview()
 }
 fun edit(value: String, remember: Boolean = true) {
  if(value.length > 512) { mutable.update { it.copy(error = "Maximum 512 characters") }; return }
  if (value == state.value.expression) return
  if(remember) { undo.addLast(state.value.expression); if(undo.size > 100) undo.removeFirst(); redo.clear() }
  saved["expression"] = value
  mutable.update { it.copy(expression = value, error = null) }; evaluatePreview()
 }
 fun undo() { if(undo.isNotEmpty()) { redo.addLast(state.value.expression); edit(undo.removeLast(),false) } }
 fun redo() { if(redo.isNotEmpty()) { undo.addLast(state.value.expression); edit(redo.removeLast(),false) } }
 private fun evaluatePreview() {
  preview?.cancel()
  val expression = state.value.expression
  preview = viewModelScope.launch {
   val answer = withContext(Dispatchers.Default) { runCatching { engine.evaluate(expression).let { ComputedResult(it.text, it.approximate) } } }
   mutable.update { it.copy(result = answer.getOrNull()?.text ?: if(expression.isBlank()) "0" else "…",
    approximate = answer.getOrNull()?.approximate ?: false) }
  }
 }
 fun calculate() {
  preview?.cancel()
  val expression = state.value.expression
  viewModelScope.launch {
   val answer = withContext(Dispatchers.Default) { runCatching { engine.evaluate(expression).let { ComputedResult(it.text, it.approximate) } } }
   answer.fold(onSuccess = { a ->
    if(state.value.expression == expression) mutable.update { it.copy(result=a.text, approximate=a.approximate,error=null) }
    try {
     withContext(Dispatchers.IO) { db.history().record(HistoryEntry().apply {
      this.expression=expression; result=(if(a.approximate) "≈ " else "")+a.text; createdAt=System.currentTimeMillis()
     }) }; refresh()
    } catch (e: CancellationException) { throw e } catch (_: Exception) { mutable.update { it.copy(error="Could not save history") } }
   }, onFailure = { e -> if(state.value.expression == expression) mutable.update { it.copy(error=e.message ?: "Invalid expression") } })
  }
 }
 private suspend fun refresh() {
  try { val entries=withContext(Dispatchers.IO) { db.history().recent() }; mutable.update { it.copy(history=entries) } }
  catch (e: CancellationException) { throw e } catch (_: Exception) { mutable.update { it.copy(error="Could not load history") } }
 }
 fun clearHistory() { viewModelScope.launch {
  try { withContext(Dispatchers.IO) { db.history().clear() }; refresh() }
  catch (e: CancellationException) { throw e }
  catch (_: Exception) { mutable.update { it.copy(error="Could not clear history") } }
 } }
 fun setHapticIntensity(level: HapticIntensity) {
  mutable.update { it.copy(hapticIntensity = level) }
  viewModelScope.launch {
   try { settings.setIntensity(level) }
   catch (e: CancellationException) { throw e }
   catch (_: Exception) { mutable.update { it.copy(error="Could not save haptic setting") } }
  }
 }
 override fun onCleared() { db.close() }
}
