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

    // 主题色预设按钮
    private val colorPresets = mapOf(
        R.id.btnColorPurple to 0xFF6750A4.toInt(),
        R.id.btnColorBlue to 0xFF1976D2.toInt(),
        R.id.btnColorTeal to 0xFF00796B.toInt(),
        R.id.btnColorRed to 0xFFC62828.toInt(),
        R.id.btnColorOrange to 0xFFE65100.toInt(),
        R.id.btnColorGreen to 0xFF2E7D32.toInt(),
        R.id.btnColorPink to 0xFFAD1457.toInt()
    )

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
        applyAccentColor()
        setupButtonListeners()
        updateButtonStates()
    }

    private fun applyAccentColor() {
        val sharedPref = getSharedPreferences("app_settings", MODE_PRIVATE)
        val accentColor = sharedPref.getInt("accent_color", 0xFF6750A4.toInt())
        toolbar.setBackgroundColor(accentColor)
        findViewById<com.google.android.material.appbar.AppBarLayout>(R.id.appBarLayout)
            ?.setBackgroundColor(accentColor)
    }

    private fun updateButtonStates() {
        val sharedPref = getSharedPreferences("app_settings", MODE_PRIVATE)
        val currentTheme = sharedPref.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        val currentUA = sharedPref.getString("user_agent_mode", "mobile") ?: "mobile"
        val currentColor = sharedPref.getInt("accent_color", 0xFF6750A4.toInt())

        updateThemeButtonStyles(currentTheme)
        updateUAButtonStyles(currentUA)
        updateColorButtonStates(currentColor)
    }

    private fun updateColorButtonStates(activeColor: Int) {
        for ((btnId, color) in colorPresets) {
            val btn = findViewById<MaterialButton>(btnId)
            if (color == activeColor) {
                btn.strokeWidth = dpToPx(4)
                btn.strokeColor = android.content.res.ColorStateList.valueOf(Color.WHITE)
            } else {
                btn.strokeWidth = dpToPx(1)
                btn.strokeColor = android.content.res.ColorStateList.valueOf(0x33000000)
            }
        }
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

        // 点击整个关于卡片跳转GitHub
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardAbout).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/knowlily/listen-to"))
            startActivity(intent)
        }

        // 初始化主题色按钮
        for (btnId in colorPresets.keys) {
            findViewById<MaterialButton>(btnId)
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

        // 主题色按钮
        for ((btnId, color) in colorPresets) {
            findViewById<MaterialButton>(btnId).setOnClickListener {
                setAccentColor(color)
            }
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

    private fun setAccentColor(color: Int) {
        val sharedPref = getSharedPreferences("app_settings", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("accent_color", color)
            apply()
        }

        updateColorButtonStates(color)

        // 立即应用到设置页面
        toolbar.setBackgroundColor(color)
        findViewById<com.google.android.material.appbar.AppBarLayout>(R.id.appBarLayout)
            ?.setBackgroundColor(color)

        val colorNames = mapOf(
            0xFF6750A4.toInt() to "紫色",
            0xFF1976D2.toInt() to "蓝色",
            0xFF00796B.toInt() to "青色",
            0xFFC62828.toInt() to "红色",
            0xFFE65100.toInt() to "橙色",
            0xFF2E7D32.toInt() to "绿色",
            0xFFAD1457.toInt() to "粉色"
        )

        Snackbar.make(
            findViewById(android.R.id.content),
            "主题色已切换为${colorNames[color] ?: "自定义"}",
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