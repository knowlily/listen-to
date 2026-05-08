package com.example.simplebrowser

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.color.DynamicColors

class SettingsActivity : AppCompatActivity() {
    private lateinit var toolbar: MaterialToolbar
    private lateinit var btnClearCache: MaterialButton
    private lateinit var btnThemeLight: MaterialButton
    private lateinit var btnThemeDark: MaterialButton
    private lateinit var btnThemeSystem: MaterialButton
    private lateinit var btnUserAgentPC: MaterialButton
    private lateinit var btnUserAgentMobile: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 应用动态取色（Android 12+）
        DynamicColors.applyToActivityIfAvailable(this)

        // 应用保存的主题设置
        val sharedPref = getSharedPreferences("app_settings", MODE_PRIVATE)
        val savedTheme = sharedPref.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(savedTheme)

        setContentView(R.layout.activity_settings)

        initViews()
        setupToolbar()
        setupButtonListeners()
        updateButtonStates()
    }

    private fun updateButtonStates() {
        val sharedPref = getSharedPreferences("app_settings", MODE_PRIVATE)
        val currentTheme = sharedPref.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        val currentUA = sharedPref.getString("user_agent_mode", "mobile") ?: "mobile"

        updateThemeButtonStyles(currentTheme)
        updateUAButtonStyles(currentUA)
    }

    private fun getPrimaryColor(): Int {
        val attrs = intArrayOf(androidx.appcompat.R.attr.colorPrimary)
        val typedArray = obtainStyledAttributes(attrs)
        val color = typedArray.getColor(0, Color.parseColor("#6750A4"))
        typedArray.recycle()
        return color
    }

    private fun updateThemeButtonStyles(activeMode: Int) {
        val primaryColor = getPrimaryColor()

        fun applyButton(btn: MaterialButton, isActive: Boolean) {
            if (isActive) {
                btn.setBackgroundColor(primaryColor)
                btn.setTextColor(Color.WHITE)
                btn.strokeWidth = 0
            } else {
                btn.setBackgroundColor(Color.TRANSPARENT)
                btn.setTextColor(primaryColor)
                btn.strokeWidth = dpToPx(1)
                btn.strokeColor = android.content.res.ColorStateList.valueOf(primaryColor)
            }
        }

        applyButton(btnThemeLight, activeMode == AppCompatDelegate.MODE_NIGHT_NO)
        applyButton(btnThemeDark, activeMode == AppCompatDelegate.MODE_NIGHT_YES)
        applyButton(btnThemeSystem, activeMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

    private fun updateUAButtonStyles(activeMode: String) {
        val primaryColor = getPrimaryColor()

        fun applyButton(btn: MaterialButton, isActive: Boolean) {
            if (isActive) {
                btn.setBackgroundColor(primaryColor)
                btn.setTextColor(Color.WHITE)
                btn.strokeWidth = 0
            } else {
                btn.setBackgroundColor(Color.TRANSPARENT)
                btn.setTextColor(primaryColor)
                btn.strokeWidth = dpToPx(1)
                btn.strokeColor = android.content.res.ColorStateList.valueOf(primaryColor)
            }
        }

        applyButton(btnUserAgentPC, activeMode == "pc")
        applyButton(btnUserAgentMobile, activeMode == "mobile")
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        btnClearCache = findViewById(R.id.btnClearCache)
        btnThemeLight = findViewById(R.id.btnThemeLight)
        btnThemeDark = findViewById(R.id.btnThemeDark)
        btnThemeSystem = findViewById(R.id.btnThemeSystem)
        btnUserAgentPC = findViewById(R.id.btnUserAgentPC)
        btnUserAgentMobile = findViewById(R.id.btnUserAgentMobile)

        // 设置GitHub链接点击事件
        findViewById<TextView>(R.id.tvGitHub).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/knowlily/listen-to"))
            startActivity(intent)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupButtonListeners() {
        btnClearCache.setOnClickListener {
            clearWebViewCache()
        }

        btnThemeLight.setOnClickListener {
            setThemeMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        btnThemeDark.setOnClickListener {
            setThemeMode(AppCompatDelegate.MODE_NIGHT_YES)
        }

        btnThemeSystem.setOnClickListener {
            setThemeMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }

        btnUserAgentPC.setOnClickListener {
            setUserAgentMode("pc")
        }

        btnUserAgentMobile.setOnClickListener {
            setUserAgentMode("mobile")
        }
    }

    private fun clearWebViewCache() {
        try {
            // 清除WebView缓存
            WebStorage.getInstance().deleteAllData()

            // 清除Cookie
            val cookieManager = CookieManager.getInstance()
            cookieManager.removeAllCookies(null)
            cookieManager.flush()

            // 清除本地存储
            deleteDatabase("webview.db")
            deleteDatabase("webviewCache.db")

            // 删除缓存目录
            cacheDir.deleteRecursively()

            Snackbar.make(
                findViewById(android.R.id.content),
                "缓存已清除",
                Snackbar.LENGTH_LONG
            ).show()

            Toast.makeText(this, "缓存清除完成", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Snackbar.make(
                findViewById(android.R.id.content),
                "清除缓存失败: ${e.localizedMessage}",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private fun setThemeMode(mode: Int) {
        AppCompatDelegate.setDefaultNightMode(mode)

        // 保存用户选择
        val sharedPref = getSharedPreferences("app_settings", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("theme_mode", mode)
            apply()
        }

        updateThemeButtonStyles(mode)

        // 显示确认消息
        val message = when (mode) {
            AppCompatDelegate.MODE_NIGHT_NO -> "已切换到浅色主题"
            AppCompatDelegate.MODE_NIGHT_YES -> "已切换到深色主题"
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> "已设置为跟随系统"
            else -> "主题已更新"
        }

        Snackbar.make(
            findViewById(android.R.id.content),
            message,
            Snackbar.LENGTH_SHORT
        ).show()
    }

    private fun setUserAgentMode(mode: String) {
        // 保存用户选择
        val sharedPref = getSharedPreferences("app_settings", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("user_agent_mode", mode)
            apply()
        }

        updateUAButtonStyles(mode)

        // 显示确认消息
        val message = when (mode) {
            "pc" -> "已切换到电脑模式"
            "mobile" -> "已切换到手机模式"
            else -> "浏览器标识已更新"
        }

        Snackbar.make(
            findViewById(android.R.id.content),
            message,
            Snackbar.LENGTH_SHORT
        ).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}