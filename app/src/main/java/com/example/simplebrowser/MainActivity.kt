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
import com.google.android.material.color.DynamicColors

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

        // 应用动态取色（Android 12+）
        DynamicColors.applyToActivityIfAvailable(this)

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
        webSettings.databaseEnabled = true
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true
        webSettings.setSupportZoom(true)
        webSettings.builtInZoomControls = true
        webSettings.displayZoomControls = false
        webSettings.allowFileAccess = true
        webSettings.allowContentAccess = true
        webSettings.allowUniversalAccessFromFileURLs = true
        webSettings.allowFileAccessFromFileURLs = true
        webSettings.mixedContentMode = 0  // MIXED_CONTENT_ALWAYS_ALLOW

        // 设置缓存
        webSettings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT

        // 设置用户代理
        val defaultUserAgent = webSettings.userAgentString
        webSettings.userAgentString = "$defaultUserAgent SimpleBrowser/1.1"

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
                        val url = request?.url?.toString() ?: ""
                        val isForMainFrame = request?.isForMainFrame ?: false

                        Log.d(TAG, "shouldOverrideUrlLoading (新API): url=$url, isForMainFrame=$isForMainFrame")

                        // 只拦截file://协议，其他URL都在WebView中打开
                        if (url.startsWith("file://")) {
                            Log.w(TAG, "阻止加载本地文件: $url")
                            return true
                        }

                        // 对于主框架请求，确保在WebView中打开
                        if (isForMainFrame) {
                            Log.d(TAG, "允许WebView加载主框架URL: $url")
                        }

                        false
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
                    val urlStr = url ?: ""
                    Log.d(TAG, "shouldOverrideUrlLoading (旧API): url=$urlStr")

                    // 只拦截file://协议，其他URL都在WebView中打开
                    if (urlStr.startsWith("file://")) {
                        Log.w(TAG, "阻止加载本地文件: $urlStr")
                        return true
                    }

                    false
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
        // 动态取色已在onCreate中通过DynamicColors.applyToActivityIfAvailable启用
        // 这里可以添加其他动态取色相关的配置

        // 检查动态取色是否可用
        val isDynamicColorAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        Log.d(TAG, "动态取色可用: $isDynamicColorAvailable")

        // 可以根据需要添加额外的动态取色配置
    }

    private fun loadUrl(url: String) {
        // 检查URL是否为空或仅包含空白字符
        val trimmedUrl = url.trim()
        if (trimmedUrl.isEmpty()) {
            showError("请输入有效网址")
            return
        }

        // 检查网络连接
        if (!isNetworkAvailable()) {
            showError(getString(R.string.no_internet))
            return
        }

        try {
            var processedUrl = trimmedUrl

            // 处理特殊URL
            when {
                // 如果已经是完整的URL，保持不变
                trimmedUrl.startsWith("http://") || trimmedUrl.startsWith("https://") -> {
                    processedUrl = trimmedUrl
                }
                // 处理www开头的地址
                trimmedUrl.startsWith("www.") -> {
                    processedUrl = "https://$trimmedUrl"
                }
                // 处理IP地址或localhost
                trimmedUrl.matches(Regex("^\\d+\\.\\d+\\.\\d+\\.\\d+.*")) || trimmedUrl.startsWith("localhost") -> {
                    processedUrl = "http://$trimmedUrl"
                }
                // 默认添加https://前缀
                else -> {
                    processedUrl = "https://$trimmedUrl"
                }
            }

            Log.d(TAG, "开始加载URL: $processedUrl")

            // 加载网页
            webView.loadUrl(processedUrl)
            etUrl.clearFocus()
            showLoading(true)

        } catch (e: Exception) {
            Log.e(TAG, "加载URL失败: ${e.message}", e)
            showError("加载页面时出错: ${e.localizedMessage}")
            showLoading(false)
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