package com.knowlily.browser.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.knowlily.browser.R
import com.knowlily.browser.viewmodel.BrowserViewModel
import com.knowlily.browser.plugin.PluginManager

class BrowserFragment : Fragment() {

    private val browserViewModel: BrowserViewModel by activityViewModels()

    private var webView: WebView? = null
    private lateinit var etUrl: TextInputEditText
    private lateinit var btnBack: MaterialButton
    private lateinit var btnForward: MaterialButton
    private lateinit var btnRefresh: MaterialButton
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var cardAddressBar: MaterialCardView
    private lateinit var appBarLayout: AppBarLayout
    private lateinit var toolbar: MaterialToolbar
    private lateinit var popupContainer: android.widget.FrameLayout
    private lateinit var popupWebViewContainer: android.widget.FrameLayout
    private lateinit var popupToolbar: MaterialToolbar

    private var isNavigationHidden = false
    private var lastScrollY = 0
    private val scrollThreshold = 100
    private var popupWebView: WebView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_browser, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        savedInstanceState?.getBundle("webViewState")?.let { webView?.restoreState(it) }

        if (!browserViewModel.isWebViewConfigured) {
            setupWebView()
            browserViewModel.isWebViewConfigured = true
        }

        setupButtonListeners()
        applyAccentColor()
        observeViewModel()

        if (browserViewModel.currentUrl.value.isNullOrEmpty()) {
            browserViewModel.loadUrl("https://www.bing.com")
        }
    }

    private fun initViews(view: View) {
        toolbar = view.findViewById(R.id.toolbar)
        appBarLayout = view.findViewById(R.id.appBarLayout)
        webView = view.findViewById(R.id.webView)
        etUrl = view.findViewById(R.id.etUrl)
        btnBack = view.findViewById(R.id.btnBack)
        btnForward = view.findViewById(R.id.btnForward)
        btnRefresh = view.findViewById(R.id.btnRefresh)
        progressBar = view.findViewById(R.id.progressBar)
        cardAddressBar = view.findViewById(R.id.cardAddressBar)
        popupContainer = view.findViewById(R.id.popupContainer)
        popupWebViewContainer = view.findViewById(R.id.popupWebViewContainer)
        popupToolbar = view.findViewById(R.id.popupToolbar)

        popupToolbar.setNavigationOnClickListener { closePopup() }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView?.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.setSupportMultipleWindows(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.allowFileAccess = false
            settings.userAgentString = browserViewModel.userAgent.value ?: settings.userAgentString

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    url?.let {
                        etUrl.setText(it)
                        etUrl.setSelection(it.length)
                        browserViewModel.pluginManager.notifyPageStarted(view!!, it)
                        browserViewModel.currentUrl.value = it
                    }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    url?.let {
                        browserViewModel.onPageFinished(it)
                        browserViewModel.pluginManager.notifyPageFinished(view!!, it)
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

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    browserViewModel.updateProgress(newProgress)
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

            setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                val dy = scrollY - oldScrollY
                if (dy > scrollThreshold && !isNavigationHidden) hideNavigation()
                else if (dy < -scrollThreshold && isNavigationHidden) showNavigation()
                lastScrollY = scrollY
            }
        }
    }

    private fun setupButtonListeners() {
        btnBack.setOnClickListener { webView?.goBack() }
        btnForward.setOnClickListener { webView?.goForward() }
        btnRefresh.setOnClickListener { webView?.reload() }

        etUrl.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                browserViewModel.loadUrl(etUrl.text.toString())
                webView?.requestFocus()
                true
            } else false
        }
    }

    private fun observeViewModel() {
        browserViewModel.currentUrl.observe(viewLifecycleOwner) { url ->
            if (url.isNotEmpty() && url != "about:blank") {
                webView?.loadUrl(url)
            }
        }

        browserViewModel.loadProgress.observe(viewLifecycleOwner) { progress ->
            progressBar.progress = progress
            progressBar.visibility = if (progress in 1..99) View.VISIBLE else View.GONE
        }

        browserViewModel.userAgent.observe(viewLifecycleOwner) { ua ->
            webView?.settings?.userAgentString = ua
        }

        browserViewModel.accentColor.observe(viewLifecycleOwner) { color ->
            toolbar.setBackgroundColor(color)
            appBarLayout.setBackgroundColor(color)
            progressBar.setIndicatorColor(color)
        }
    }

    private fun applyAccentColor() {
        browserViewModel.accentColor.value?.let {
            toolbar.setBackgroundColor(it)
            appBarLayout.setBackgroundColor(it)
        }
    }

    private fun hideNavigation() {
        appBarLayout.setExpanded(false, true)
        cardAddressBar.animate().alpha(0f).translationY(-cardAddressBar.height.toFloat()).setDuration(250)
            .withEndAction { cardAddressBar.visibility = View.GONE }.start()
        browserViewModel.isBottomNavVisible.value = false
        isNavigationHidden = true
    }

    private fun showNavigation() {
        appBarLayout.setExpanded(true, true)
        cardAddressBar.visibility = View.VISIBLE
        cardAddressBar.animate().alpha(1f).translationY(0f).setDuration(250).start()
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
        if (webView?.canGoBack() == true) {
            webView?.goBack()
            return true
        }
        return false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val state = Bundle()
        webView?.saveState(state)
        outState.putBundle("webViewState", state)
    }
}
