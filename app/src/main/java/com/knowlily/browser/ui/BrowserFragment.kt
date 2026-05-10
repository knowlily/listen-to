package com.knowlily.browser.ui

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.knowlily.browser.R
import com.knowlily.browser.adapter.TabAdapter
import com.knowlily.browser.viewmodel.BrowserViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BrowserFragment : Fragment() {

    private val browserViewModel: BrowserViewModel by activityViewModels()

    private var currentWebView: WebView? = null
    private var currentTabId = 0
    private val webViewStates = mutableMapOf<Int, Bundle?>()

    private lateinit var etUrl: TextInputEditText
    private lateinit var btnBack: MaterialButton
    private lateinit var btnForward: MaterialButton
    private lateinit var btnRefresh: MaterialButton
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var cardAddressBar: MaterialCardView
    private lateinit var appBarLayout: AppBarLayout
    private lateinit var toolbar: MaterialToolbar
    private lateinit var popupContainer: FrameLayout
    private lateinit var popupWebViewContainer: FrameLayout
    private lateinit var popupToolbar: MaterialToolbar
    private lateinit var webViewContainer: FrameLayout
    private lateinit var rvTabBar: RecyclerView
    private lateinit var tabAdapter: TabAdapter

    private var isNavigationHidden = false
    private val scrollThreshold = 100
    private var popupWebView: WebView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_browser, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupTabBar()
        setupButtonListeners()
        applyAccentColor()
        observeViewModel()

        if (savedInstanceState == null && browserViewModel.currentUrl.value.isNullOrEmpty()) {
            browserViewModel.loadUrl("https://www.bing.com")
        }
    }

    private fun initViews(view: View) {
        toolbar = view.findViewById(R.id.toolbar)
        appBarLayout = view.findViewById(R.id.appBarLayout)
        webViewContainer = view.findViewById(R.id.webViewContainer)
        etUrl = view.findViewById(R.id.etUrl)
        btnBack = view.findViewById(R.id.btnBack)
        btnForward = view.findViewById(R.id.btnForward)
        btnRefresh = view.findViewById(R.id.btnRefresh)
        progressBar = view.findViewById(R.id.progressBar)
        cardAddressBar = view.findViewById(R.id.cardAddressBar)
        popupContainer = view.findViewById(R.id.popupContainer)
        popupWebViewContainer = view.findViewById(R.id.popupWebViewContainer)
        popupToolbar = view.findViewById(R.id.popupToolbar)
        rvTabBar = view.findViewById(R.id.rvTabBar)

        popupToolbar.setNavigationOnClickListener { closePopup() }
    }

    private fun setupTabBar() {
        tabAdapter = TabAdapter(
            onTabClick = { tab -> browserViewModel.switchToTab(tab.id) },
            onTabClose = { tab -> browserViewModel.closeTab(tab.id) },
            onAddTab = { browserViewModel.addTab() },
            onAddIncognitoTab = { browserViewModel.addTab(isIncognito = true) }
        )
        rvTabBar.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvTabBar.adapter = tabAdapter

        browserViewModel.accentColor.value?.let { tabAdapter.setAccentColor(it) }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView(wv: WebView, isIncognito: Boolean) {
        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = !isIncognito
            setSupportMultipleWindows(true)
            builtInZoomControls = true
            displayZoomControls = false
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            allowFileAccess = false
            userAgentString = browserViewModel.userAgent.value ?: userAgentString
            if (isIncognito) {
                cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
                saveFormData = false
            }
        }
        wv.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        if (isIncognito) {
            CookieManager.getInstance().setAcceptCookie(false)
        }

        wv.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                url?.let {
                    etUrl.setText(it)
                    etUrl.setSelection(it.length)
                    browserViewModel.pluginManager.notifyPageStarted(view!!, it)
                    browserViewModel.currentUrl.value = it
                    browserViewModel.updateTabUrl(currentTabId, it)
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                url?.let {
                    if (!browserViewModel.isActiveTabIncognito()) {
                        browserViewModel.onPageFinished(it)
                    }
                    browserViewModel.pluginManager.notifyPageFinished(view!!, it)
                    view?.title?.let { title ->
                        browserViewModel.updateTabTitle(currentTabId, title)
                    }
                }
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                if (request?.isForMainFrame == true) {
                    browserViewModel.isLoading.value = false
                    progressBar.visibility = View.GONE
                }
            }

            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                browserViewModel.canGoBack.value = view?.canGoBack() ?: false
                browserViewModel.canGoForward.value = view?.canGoForward() ?: false
            }

            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler, error: SslError?) {
                val message = when (error?.primaryError) {
                    SslError.SSL_UNTRUSTED -> "证书不受信任"
                    SslError.SSL_EXPIRED -> "证书已过期"
                    SslError.SSL_IDMISMATCH -> "证书域名不匹配"
                    SslError.SSL_NOTYETVALID -> "证书尚未生效"
                    else -> "SSL 证书错误"
                }
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("安全警告")
                    .setMessage("$message\n\n是否继续加载？")
                    .setPositiveButton("继续") { _, _ -> handler.proceed() }
                    .setNegativeButton("取消") { _, _ -> handler.cancel() }
                    .show()
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                if (url == "about:blank") return false
                val result = browserViewModel.pluginManager.notifyUrlLoading(url)
                if (result == "about:blank") return true
                if (result != url) {
                    view?.loadUrl(result)
                    return true
                }
                return false
            }
        }

        wv.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                browserViewModel.updateProgress(newProgress)
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                title?.let { browserViewModel.updateTabTitle(currentTabId, it) }
            }

            override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?): Boolean {
                val newWebView = WebView(requireContext())
                newWebView.settings.javaScriptEnabled = true
                newWebView.settings.domStorageEnabled = true
                newWebView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(v: WebView?, url: String?) {
                        resultMsg?.let {
                            (it.obj as? WebView.WebViewTransport)?.webView = newWebView
                            it.sendToTarget()
                        }
                    }
                }
                popupWebView = newWebView
                popupWebViewContainer.removeAllViews()
                popupWebViewContainer.addView(newWebView)
                popupContainer.visibility = View.VISIBLE
                resultMsg?.obj = newWebView
                return true
            }

            override fun onCloseWindow(window: WebView?) {
                closePopup()
            }
        }

        wv.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            try {
                val request = DownloadManager.Request(Uri.parse(url)).apply {
                    setMimeType(mimetype)
                    addRequestHeader("User-Agent", userAgent)
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, url.substringAfterLast("/"))
                }
                val dm = requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                dm.enqueue(request)
                Snackbar.make(requireView(), "下载已开始", Snackbar.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Snackbar.make(requireView(), "下载失败: ${e.localizedMessage}", Snackbar.LENGTH_SHORT).show()
            }
        }

        wv.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            val dy = scrollY - oldScrollY
            if (dy > scrollThreshold && !isNavigationHidden) hideNavigation()
            else if (dy < -scrollThreshold && isNavigationHidden) showNavigation()
        }
    }

    private fun createWebView(): WebView {
        return WebView(requireContext())
    }

    private fun switchToTab(tabId: Int) {
        browserViewModel.getTabUrl(currentTabId)?.let { browserViewModel.updateTabUrl(currentTabId, it) }

        // Save current WebView state
        currentWebView?.let { wv ->
            webViewStates[currentTabId] = Bundle().also { b -> wv.saveState(b) }
            webViewContainer.removeView(wv)
        }

        currentTabId = tabId
        val tab = browserViewModel.getActiveTab() ?: return

        // Create or restore WebView
        val wv = createWebView()
        configureWebView(wv, tab.isIncognito)
        webViewStates[tabId]?.let { wv.restoreState(it) }
        webViewContainer.addView(wv, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        currentWebView = wv

        // Restore URL to address bar
        val savedUrl = browserViewModel.getTabUrl(tabId)
        etUrl.setText(savedUrl ?: tab.url)

        // If no saved state, navigate to initial URL
        if (webViewStates[tabId] == null && !savedUrl.isNullOrEmpty()) {
            wv.loadUrl(savedUrl)
        }

        // Restore cookie acceptance for normal tabs
        if (!tab.isIncognito) {
            CookieManager.getInstance().setAcceptCookie(true)
        }

        browserViewModel.canGoBack.value = wv.canGoBack()
        browserViewModel.canGoForward.value = wv.canGoForward()
    }

    private fun setupButtonListeners() {
        btnBack.setOnClickListener { currentWebView?.goBack() }
        btnForward.setOnClickListener { currentWebView?.goForward() }
        btnRefresh.setOnClickListener { currentWebView?.reload() }

        etUrl.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                browserViewModel.loadUrl(etUrl.text.toString())
                currentWebView?.requestFocus()
                true
            } else false
        }
    }

    private fun observeViewModel() {
        browserViewModel.navigateUrl.observe(viewLifecycleOwner) { url ->
            if (url.isNotEmpty() && url != "about:blank") {
                currentWebView?.loadUrl(url)
                currentWebView?.requestFocus()
            }
        }

        browserViewModel.loadProgress.observe(viewLifecycleOwner) { progress ->
            progressBar.progress = progress
            progressBar.visibility = if (progress in 1..99) View.VISIBLE else View.GONE
        }

        browserViewModel.canGoBack.observe(viewLifecycleOwner) { can ->
            btnBack.isEnabled = can
        }

        browserViewModel.canGoForward.observe(viewLifecycleOwner) { can ->
            btnForward.isEnabled = can
        }

        browserViewModel.userAgent.observe(viewLifecycleOwner) { ua ->
            currentWebView?.settings?.userAgentString = ua
        }

        browserViewModel.accentColor.observe(viewLifecycleOwner) { color ->
            toolbar.setBackgroundColor(color)
            appBarLayout.setBackgroundColor(color)
            progressBar.setIndicatorColor(color)
            tabAdapter.setAccentColor(color)
        }

        browserViewModel.tabs.observe(viewLifecycleOwner) { tabs ->
            tabAdapter.submitList(tabs)
        }

        browserViewModel.activeTabId.observe(viewLifecycleOwner) { id ->
            if (id != currentTabId || currentWebView == null) {
                switchToTab(id)
            }
            tabAdapter.activeTabId = id
            rvTabBar.smoothScrollToPosition(tabAdapter.currentList.indexOfFirst { it.id == id }.coerceAtLeast(0))
        }
    }

    private fun applyAccentColor() {
        browserViewModel.accentColor.value?.let {
            toolbar.setBackgroundColor(it)
            appBarLayout.setBackgroundColor(it)
            tabAdapter.setAccentColor(it)
        }
    }

    private fun hideNavigation() {
        appBarLayout.setExpanded(false, true)
        cardAddressBar.animate().alpha(0f).translationY(-cardAddressBar.height.toFloat()).setDuration(250)
            .withEndAction { cardAddressBar.visibility = View.GONE }.start()
        rvTabBar.animate().alpha(0f).translationY(-rvTabBar.height.toFloat()).setDuration(250)
            .withEndAction { rvTabBar.visibility = View.GONE }.start()
        browserViewModel.isBottomNavVisible.value = false
        isNavigationHidden = true
    }

    private fun showNavigation() {
        appBarLayout.setExpanded(true, true)
        cardAddressBar.visibility = View.VISIBLE
        cardAddressBar.animate().alpha(1f).translationY(0f).setDuration(250).start()
        rvTabBar.visibility = View.VISIBLE
        rvTabBar.animate().alpha(1f).translationY(0f).setDuration(250).start()
        browserViewModel.isBottomNavVisible.value = true
        isNavigationHidden = false
    }

    fun closePopup() {
        popupContainer.visibility = View.GONE
        popupWebViewContainer.removeAllViews()
        popupWebView?.destroy()
        popupWebView = null
    }

    fun handleBackPressed(): Boolean {
        if (popupContainer.visibility == View.VISIBLE) {
            closePopup()
            return true
        }
        if (currentWebView?.canGoBack() == true) {
            currentWebView?.goBack()
            return true
        }
        return false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        currentWebView?.let {
            val state = Bundle()
            it.saveState(state)
            outState.putBundle("webViewState_$currentTabId", state)
        }
    }
}
