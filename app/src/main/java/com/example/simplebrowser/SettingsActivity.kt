package com.example.simplebrowser

import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar

class SettingsActivity : AppCompatActivity() {
    private lateinit var toolbar: MaterialToolbar
    private lateinit var btnClearCache: MaterialButton
    private lateinit var btnThemeLight: MaterialButton
    private lateinit var btnThemeDark: MaterialButton
    private lateinit var btnThemeSystem: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        initViews()
        setupToolbar()
        setupButtonListeners()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        btnClearCache = findViewById(R.id.btnClearCache)
        btnThemeLight = findViewById(R.id.btnThemeLight)
        btnThemeDark = findViewById(R.id.btnThemeDark)
        btnThemeSystem = findViewById(R.id.btnThemeSystem)
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

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}