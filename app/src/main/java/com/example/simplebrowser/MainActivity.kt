package com.example.simplebrowser

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.color.DynamicColors
import androidx.core.content.ContextCompat

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
    private lateinit var cardAddressBar: com.google.android.material.card.MaterialCardView
    private lateinit var cardBottomBar: com.google.android.material.card.MaterialCardView
    private lateinit var appBarLayout: com.google.android.material.appbar.AppBarLayout
    private lateinit var popupContainer: android.widget.FrameLayout
    private lateinit var popupWebViewContainer: android.widget.FrameLayout
    private var isNavigationHidden = false
    private var lastScrollY = 0
    private val scrollThreshold = 100 // 滚动阈值，单位像素

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

        // 应用自定义主题色
        applyAccentColor()

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
        cardAddressBar = findViewById(R.id.cardAddressBar)
        cardBottomBar = findViewById(R.id.cardBottomBar)
        appBarLayout = findViewById(R.id.appBarLayout)
        popupContainer = findViewById(R.id.popupContainer)
        popupWebViewContainer = findViewById(R.id.popupWebViewContainer)

        // 设置弹窗工具栏返回按钮
        val popupToolbar = findViewById<MaterialToolbar>(R.id.popupToolbar)
        popupToolbar.setNavigationOnClickListener {
            closePopupWindow()
        }
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
        webSettings.setSupportMultipleWindows(true)
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true)
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true
        webSettings.setSupportZoom(true)
        webSettings.builtInZoomControls = true
        webSettings.displayZoomControls = false
        webSettings.allowFileAccess = false
        webSettings.allowContentAccess = true
        webSettings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW

        // 设置缓存
        webSettings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT

        // 设置用户代理
        val defaultUserAgent = webSettings.userAgentString
        val sharedPref = getSharedPreferences("app_settings", MODE_PRIVATE)
        val userAgentMode = sharedPref.getString("user_agent_mode", "mobile") ?: "mobile"

        val userAgent = when (userAgentMode) {
            "pc" -> {
                // 桌面版Chrome用户代理（Windows）
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 SimpleBrowser/1.5"
            }
            else -> {
                // 手机模式：使用默认Android用户代理，添加应用标识
                "$defaultUserAgent SimpleBrowser/1.5"
            }
        }

        webSettings.userAgentString = userAgent
        Log.d(TAG, "设置用户代理模式: $userAgentMode, 用户代理: $userAgent")

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

                // 保存历史记录
                url?.let {
                    if (it.isNotEmpty() && it != "about:blank") {
                        saveHistory(it)
                    }
                }
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

                        // 拦截file://协议，阻止加载本地文件
                        if (url.startsWith("file://")) {
                            Log.w(TAG, "阻止加载本地文件: $url")
                            return true
                        }

                        // 对于tel:、mailto:等特殊协议，允许系统处理
                        if (url.startsWith("tel:") || url.startsWith("mailto:") ||
                            url.startsWith("sms:") || url.startsWith("geo:")) {
                            Log.d(TAG, "允许系统处理特殊协议: $url")
                            return false
                        }

                        // 其他所有http/https链接都在WebView中打开
                        Log.d(TAG, "在WebView中加载URL: $url")
                        return false
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

                    // 拦截file://协议，阻止加载本地文件
                    if (urlStr.startsWith("file://")) {
                        Log.w(TAG, "阻止加载本地文件: $urlStr")
                        return true
                    }

                    // 对于tel:、mailto:等特殊协议，允许系统处理
                    if (urlStr.startsWith("tel:") || urlStr.startsWith("mailto:") ||
                        urlStr.startsWith("sms:") || urlStr.startsWith("geo:")) {
                        Log.d(TAG, "允许系统处理特殊协议: $urlStr")
                        return false
                    }

                    // 其他所有http/https链接都在WebView中打开
                    Log.d(TAG, "在WebView中加载URL: $urlStr")
                    return false
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

            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: android.os.Message?
            ): Boolean {
                // 在弹窗容器中创建新的WebView
                val ctx = view?.context ?: this@MainActivity
                val newWebView = WebView(ctx).apply {
                    layoutParams = android.widget.FrameLayout.LayoutParams(
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.setSupportMultipleWindows(false)
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(wv: WebView?, url: String?) {
                            super.onPageFinished(wv, url)
                            url?.let {
                                if (it.isNotEmpty() && it != "about:blank") {
                                    saveHistory(it)
                                }
                            }
                        }
                    }
                }

                popupWebViewContainer.removeAllViews()
                popupWebViewContainer.addView(newWebView)
                popupContainer.visibility = View.VISIBLE

                val transport = resultMsg?.obj as? android.webkit.WebView.WebViewTransport
                transport?.webView = newWebView
                resultMsg?.sendToTarget()
                return true
            }

            override fun onCloseWindow(window: WebView?) {
                closePopupWindow()
            }
        }

        // 设置滚动监听，实现自动隐藏导航栏
        webView.setOnScrollChangeListener { _, scrollX, scrollY, oldScrollX, oldScrollY ->
            handleScroll(scrollY, oldScrollY)
        }

        // 加载默认主页
        loadUrl("https://www.bing.com")
    }

    private fun handleScroll(scrollY: Int, oldScrollY: Int) {
        val scrollingDown = scrollY > oldScrollY
        val scrollingUp = scrollY < oldScrollY
        val scrollDelta = Math.abs(scrollY - oldScrollY)

        // 只有当滚动距离超过阈值时才处理
        if (scrollDelta < scrollThreshold) {
            return
        }

        if (scrollingDown && scrollY > scrollThreshold && !isNavigationHidden) {
            // 向下滚动，隐藏导航栏（更多阅读空间）
            hideNavigation()
        } else if (scrollingUp && isNavigationHidden) {
            // 向上滚动，显示导航栏（用户想导航）
            showNavigation()
        }

        lastScrollY = scrollY
    }

    private fun hideNavigation() {
        if (!isNavigationHidden) {
            // 使用 AppBarLayout 收起动画，会自动调整 CoordinatorLayout 子视图
            appBarLayout.setExpanded(false, true)

            cardAddressBar.animate()
                .alpha(0f)
                .translationY(-cardAddressBar.height.toFloat())
                .setDuration(250)
                .withEndAction {
                    cardAddressBar.visibility = View.GONE
                    // 强制重新布局让 WebView 填充空间
                    cardAddressBar.parent?.requestLayout()
                }
                .start()

            cardBottomBar.animate()
                .alpha(0f)
                .translationY(cardBottomBar.height.toFloat())
                .setDuration(250)
                .withEndAction { cardBottomBar.visibility = View.INVISIBLE }
                .start()

            isNavigationHidden = true
        }
    }

    private fun showNavigation() {
        if (isNavigationHidden) {
            // 展开 AppBarLayout
            appBarLayout.setExpanded(true, true)

            cardAddressBar.visibility = View.VISIBLE
            cardAddressBar.alpha = 0f
            cardAddressBar.translationY = -cardAddressBar.height.toFloat()
            cardAddressBar.animate().alpha(1f).translationY(0f).setDuration(250)
                .withEndAction {
                    cardAddressBar.parent?.requestLayout()
                }
                .start()

            cardBottomBar.visibility = View.VISIBLE
            cardBottomBar.alpha = 0f
            cardBottomBar.translationY = cardBottomBar.height.toFloat()
            cardBottomBar.animate().alpha(1f).translationY(0f).setDuration(250).start()

            isNavigationHidden = false
        }
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
            if (actionId == EditorInfo.IME_ACTION_GO) {
                // Only handle IME action — physical Enter keys also trigger IME_ACTION_GO,
                // so we skip the raw KeyEvent to avoid double-loadUrl
                val url = etUrl.text.toString().trim()
                loadUrl(url)
                true
            } else {
                false
            }
        }

    }

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    loadUrl("https://www.bing.com")
                    true
                }
                R.id.navigation_bookmarks -> {
                    val intent = Intent(this, BookmarksActivity::class.java)
                    // 传递当前URL，用于添加书签
                    val currentUrl = webView.url ?: etUrl.text.toString()
                    if (currentUrl.isNotEmpty() && currentUrl != "about:blank") {
                        intent.putExtra("url", currentUrl)
                    }
                    startActivity(intent)
                    true
                }
                R.id.navigation_history -> {
                    val intent = Intent(this, HistoryActivity::class.java)
                    startActivity(intent)
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

    private fun applyAccentColor() {
        val sharedPref = getSharedPreferences("app_settings", MODE_PRIVATE)
        val accentColor = sharedPref.getInt("accent_color", ContextCompat.getColor(this, R.color.md_theme_light_primary))

        toolbar.setBackgroundColor(accentColor)
        appBarLayout.setBackgroundColor(accentColor)
        progressBar.setIndicatorColor(accentColor)
        cardBottomBar.setCardBackgroundColor(accentColor)
        bottomNavigationView.itemIconTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
        bottomNavigationView.itemTextColor = android.content.res.ColorStateList.valueOf(Color.WHITE)
    }

    private fun setupDynamicColors() {
        val isDynamicColorAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        Log.d(TAG, "动态取色可用: $isDynamicColorAvailable")
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
            e.printStackTrace()

            // 提供更具体的错误信息
            val errorMsg = when {
                e is android.webkit.WebResourceError -> "网页资源错误: ${e.description}"
                e is java.net.UnknownHostException -> "无法找到服务器，请检查网络连接"
                e is java.net.SocketTimeoutException -> "连接超时，请重试"
                e is javax.net.ssl.SSLHandshakeException -> "安全连接失败，请检查日期和时间设置"
                e is java.io.IOException -> "网络错误: ${e.message}"
                else -> "加载页面时出错: ${e.localizedMessage ?: "未知错误"}"
            }

            showError(errorMsg)
            showLoading(false)
        }
    }

    private fun updateUrl(url: String?) {
        url?.let {
            etUrl.setText(it)
            etUrl.setSelection(it.length)
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
        // 始终使用刷新图标，避免动画drawable问题
        btnRefresh.icon = ContextCompat.getDrawable(this, R.drawable.ic_refresh)

        // 可以添加旋转动画，但暂时简化
        if (loading) {
            // 未来可以添加旋转动画
            btnRefresh.rotation = 0f
            btnRefresh.animate().rotationBy(360f).setDuration(1000).withEndAction {
                if (loading) {
                    btnRefresh.animate().rotationBy(360f).setDuration(1000).start()
                }
            }.start()
        } else {
            btnRefresh.animate().cancel()
            btnRefresh.rotation = 0f
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

    private fun saveHistory(url: String) {
        try {
            val timestamp = System.currentTimeMillis()
            val sharedPref = getSharedPreferences("browser_history", MODE_PRIVATE)

            // 使用时间戳作为key，确保唯一性
            with(sharedPref.edit()) {
                putString(timestamp.toString(), url)
                apply()
            }

            Log.d(TAG, "保存历史记录: $url, 时间戳: $timestamp")
        } catch (e: Exception) {
            Log.e(TAG, "保存历史记录失败: ${e.message}", e)
        }
    }

    override fun onBackPressed() {
        if (popupContainer.visibility == View.VISIBLE) {
            closePopupWindow()
        } else if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    private fun closePopupWindow() {
        popupContainer.visibility = View.GONE
        popupWebViewContainer.removeAllViews()
    }
}