package com.example.simplebrowser

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.TextView
import android.util.Log
import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"

    private lateinit var webView: WebView
    private lateinit var etUrl: TextInputEditText
    private lateinit var btnBack: MaterialButton
    private lateinit var btnForward: MaterialButton
    private lateinit var btnRefresh: MaterialButton
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var progressBar: com.google.android.material.progressindicator.LinearProgressIndicator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 应用保存的主题设置
        val sharedPref = getSharedPreferences("app_settings", MODE_PRIVATE)
        val savedTheme = sharedPref.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(savedTheme)

        setContentView(R.layout.activity_main)

        // 初始化视图
        initViews()

        // 配置WebView
        setupWebView()

        // 设置按钮点击事件
        setupButtonListeners()

        // 设置底部导航
        setupBottomNavigation()

        // 设置动态取色
        setupDynamicColors()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)  // 隐藏默认标题

        webView = findViewById(R.id.webView)
        etUrl = findViewById(R.id.etUrl)
        btnBack = findViewById(R.id.btnBack)
        btnForward = findViewById(R.id.btnForward)
        btnRefresh = findViewById(R.id.btnRefresh)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

            return when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }

    private fun setupWebView() {
        // 配置WebView设置
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true
        webSettings.setSupportZoom(true)
        webSettings.builtInZoomControls = true
        webSettings.displayZoomControls = false
        webSettings.allowFileAccess = true
        webSettings.allowContentAccess = true
        webSettings.allowUniversalAccessFromFileURLs = true
        webSettings.mixedContentMode = 0  // MIXED_CONTENT_ALWAYS_ALLOW

        // 设置WebView客户端
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                updateUrl(url)
                showLoading(true)
                progressBar.visibility = View.VISIBLE
                progressBar.progress = 0
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                updateUrl(url)
                updateNavigationButtons()
                showLoading(false)
                progressBar.visibility = View.GONE
                progressBar.progress = 0
            }

            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                try {
                    Log.e(TAG, "网页加载错误: code=$errorCode, desc=$description, url=$failingUrl")
                    showError(getString(R.string.error_loading_page))
                    showLoading(false)
                    progressBar.visibility = View.GONE
                } catch (e: Exception) {
                    Log.e(TAG, "处理错误时出错: ${e.message}", e)
                }
            }

            @Suppress("DEPRECATION")
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val errorDesc = error?.description?.toString() ?: "Unknown"
                        val errorCode = error?.errorCode?.toString() ?: "Unknown"
                        val url = request?.url?.toString() ?: "Unknown"
                        Log.e(TAG, "网页加载错误(新API): code=$errorCode, desc=$errorDesc, url=$url, isForMainFrame=${request?.isForMainFrame}")

                        if (request?.isForMainFrame == true) {
                            showError(getString(R.string.error_loading_page))
                            showLoading(false)
                            progressBar.visibility = View.GONE
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "处理错误时出错(新API): ${e.message}", e)
                }
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        // 拦截file://协议，阻止加载本地文件
                        val url = request?.url?.toString() ?: ""
                        url.startsWith("file://")
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "shouldOverrideUrlLoading失败: ${e.message}", e)
                    false
                }
            }

            @Suppress("DEPRECATION")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return try {
                    // 拦截file://协议，阻止加载本地文件
                    url != null && url.startsWith("file://")
                } catch (e: Exception) {
                    Log.e(TAG, "shouldOverrideUrlLoading失败: ${e.message}", e)
                    false
                }
            }
        }

        // 设置WebChromeClient以获取加载进度和标题
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                updateProgress(newProgress)
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                title?.let {
                    if (it.isNotEmpty() && it != "about:blank") {
                        supportActionBar?.title = it
                        supportActionBar?.setDisplayShowTitleEnabled(true)
                    } else {
                        supportActionBar?.setDisplayShowTitleEnabled(false)
                    }
                }
            }
        }

        // 加载默认主页
        loadUrl("https://www.google.com")
    }

    private fun setupButtonListeners() {
        // 后退按钮
        btnBack.setOnClickListener {
            if (webView.canGoBack()) {
                webView.goBack()
            }
        }

        // 前进按钮
        btnForward.setOnClickListener {
            if (webView.canGoForward()) {
                webView.goForward()
            }
        }

        // 刷新按钮
        btnRefresh.setOnClickListener {
            webView.reload()
        }

        // 地址栏输入监听
        etUrl.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                val url = etUrl.text.toString().trim()
                loadUrl(url)
                true
            } else {
                false
            }
        }

        // 地址栏焦点变化监听
        etUrl.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val url = etUrl.text.toString().trim()
                if (url.isNotEmpty()) {
                    loadUrl(url)
                }
            }
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    loadUrl("https://www.google.com")
                    true
                }
                R.id.navigation_bookmarks -> {
                    showSnackbar(getString(R.string.bookmarks))
                    true
                }
                R.id.navigation_history -> {
                    showSnackbar(getString(R.string.history))
                    true
                }
                R.id.navigation_settings -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }

    private fun setupDynamicColors() {
        // 动态取色已通过主题自动启用
        // 这里可以添加其他动态取色相关的配置
    }

    private fun loadUrl(url: String) {
        if (!isNetworkAvailable()) {
            showError(getString(R.string.no_internet))
            return
        }

        // 检查URL是否为空或仅包含空白字符
        val trimmedUrl = url.trim()
        if (trimmedUrl.isEmpty()) {
            showError("请输入有效网址")
            return
        }

        try {
            var processedUrl = trimmedUrl
            // 添加协议前缀如果缺失
            if (!processedUrl.startsWith("http://") && !processedUrl.startsWith("https://")) {
                processedUrl = "https://$processedUrl"
            }

            // 注释掉严格的URL格式验证，让WebView自己处理无效URL
            // if (!android.util.Patterns.WEB_URL.matcher(processedUrl).matches()) {
            //     showError("网址格式无效")
            //     return
            // }

            webView.loadUrl(processedUrl)
            etUrl.clearFocus()
            Log.d(TAG, "加载URL: $processedUrl")
        } catch (e: Exception) {
            Log.e(TAG, "加载URL失败: ${e.message}", e)
            showError("加载页面时出错: ${e.localizedMessage}")
        }
    }

    private fun updateUrl(url: String?) {
        url?.let {
            etUrl.setText(it)
        }
    }

    private fun updateNavigationButtons() {
        btnBack.isEnabled = webView.canGoBack()
        btnForward.isEnabled = webView.canGoForward()
    }

    private fun updateProgress(progress: Int) {
        if (progress < 100) {
            if (progressBar.visibility != View.VISIBLE) {
                progressBar.visibility = View.VISIBLE
            }
            progressBar.progress = progress
        } else {
            progressBar.visibility = View.GONE
            progressBar.progress = 0
        }
    }

    private fun showLoading(loading: Boolean) {
        btnRefresh.icon = if (loading) {
            // 加载时显示旋转图标
            resources.getDrawable(R.drawable.ic_loading, theme)
        } else {
            resources.getDrawable(R.drawable.ic_refresh, theme)
        }
    }

    private fun showError(message: String) {
        Snackbar.make(
            findViewById(android.R.id.content),
            message,
            Snackbar.LENGTH_LONG
        ).show()
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(
            findViewById(android.R.id.content),
            message,
            Snackbar.LENGTH_SHORT
        ).show()
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}