package com.example.calculator.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.*
import com.example.calculator.feedback.HapticIntensity
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settings by preferencesDataStore("settings")
class SettingsRepository(private val context: Context) {
    private val intensityKey = stringPreferencesKey("haptic_intensity")
    private val legacyKey = booleanPreferencesKey("haptics")
    val intensity = context.settings.data.catch { e ->
        if(e is IOException) emit(emptyPreferences()) else throw e
    }.map { p ->
        p[intensityKey]?.let { name -> HapticIntensity.entries.find { it.name == name } }
            ?: if(p[legacyKey] == false) HapticIntensity.Off else HapticIntensity.Strong
    }
    suspend fun setIntensity(level: HapticIntensity) {
        context.settings.edit { it[intensityKey] = level.name }
    }
}
