package com.example.simplebrowser

import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar

class SettingsActivity : AppCompatActivity() {
    private lateinit var toolbar: MaterialToolbar
    private lateinit var btnClearCache: MaterialButton

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

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}