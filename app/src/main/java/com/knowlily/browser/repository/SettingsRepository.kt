package com.knowlily.browser.repository

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext context: Context
) {

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

    fun isHttpsOnly(): Boolean =
        prefs.getBoolean(KEY_HTTPS_ONLY, false)

    fun setHttpsOnly(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HTTPS_ONLY, enabled).apply()
    }

    companion object {
        const val PREFS_NAME = "app_settings"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_ACCENT_COLOR = "accent_color"
        const val KEY_UA_MODE = "user_agent_mode"
        const val KEY_HTTPS_ONLY = "https_only"
        val DEFAULT_PURPLE = 0xFF6750A4.toInt()
    }
}
