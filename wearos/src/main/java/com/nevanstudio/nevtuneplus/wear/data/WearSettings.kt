// Copyright (c) 2026 NevanStudio. All rights reserved.
// Project: NevTune+
package com.nevanstudio.nevtuneplus.wear.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Persistent appearance preferences for the Wear OS watch app.
 */
object WearSettings {
    private lateinit var prefs: SharedPreferences

    val accentPreset = MutableStateFlow("default")
    val meshBackgroundEnabled = MutableStateFlow(true)
    val oledBlack = MutableStateFlow(true)

    fun init(context: Context) {
        prefs = context.getSharedPreferences("audify_wear_settings", Context.MODE_PRIVATE)
        accentPreset.value = prefs.getString(KEY_ACCENT_PRESET, "default") ?: "default"
        meshBackgroundEnabled.value = prefs.getBoolean(KEY_MESH_BG, true)
        oledBlack.value = prefs.getBoolean(KEY_OLED_BLACK, true)
    }

    fun setAccentPreset(id: String) {
        accentPreset.value = id
        if (this::prefs.isInitialized) {
            prefs.edit { putString(KEY_ACCENT_PRESET, id) }
        }
    }

    fun setMeshBackgroundEnabled(enabled: Boolean) {
        meshBackgroundEnabled.value = enabled
        if (this::prefs.isInitialized) {
            prefs.edit { putBoolean(KEY_MESH_BG, enabled) }
        }
    }

    fun setOledBlack(enabled: Boolean) {
        oledBlack.value = enabled
        if (this::prefs.isInitialized) {
            prefs.edit { putBoolean(KEY_OLED_BLACK, enabled) }
        }
    }

    private const val KEY_ACCENT_PRESET = "wear_accent_preset"
    private const val KEY_MESH_BG = "wear_mesh_bg"
    private const val KEY_OLED_BLACK = "wear_oled_black"
}
