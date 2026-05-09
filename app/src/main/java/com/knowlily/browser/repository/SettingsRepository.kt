package com.knowlily.browser.repository

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.MutableLiveData

class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val themeMode = MutableLiveData(getThemeMode())
    val accentColor = MutableLiveData(getAccentColor())
    val userAgentMode = MutableLiveData(getUserAgentMode())

    fun getThemeMode(): Int =
        prefs.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

    fun setThemeMode(mode: Int) {
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply()
        themeMode.value = mode
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun getAccentColor(): Int =
        prefs.getInt(KEY_ACCENT_COLOR, DEFAULT_PURPLE)

    fun setAccentColor(color: Int) {
        prefs.edit().putInt(KEY_ACCENT_COLOR, color).apply()
        accentColor.value = color
    }

    fun getUserAgentMode(): String =
        prefs.getString(KEY_UA_MODE, "mobile") ?: "mobile"

    fun setUserAgentMode(mode: String) {
        prefs.edit().putString(KEY_UA_MODE, mode).apply()
        userAgentMode.value = mode
    }

    companion object {
        const val PREFS_NAME = "app_settings"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_ACCENT_COLOR = "accent_color"
        const val KEY_UA_MODE = "user_agent_mode"
        val DEFAULT_PURPLE = 0xFF6750A4.toInt()
    }
}
