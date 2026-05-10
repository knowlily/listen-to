package com.knowlily.browser.ui

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.WebView.FindListener
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
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
    private val maxSavedStates = 5

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
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvTabBar: RecyclerView
    private lateinit var tabAdapter: TabAdapter
    private lateinit var rvSuggestions: RecyclerView
    private val suggestionAdapter = SuggestionAdapter { suggestion -> loadSuggestion(suggestion) }
    private var errorView: View? = null

    // Find bar
    private lateinit var findBar: View
    private lateinit var etFindQuery: TextInputEditText
    private lateinit var tvFindCount: android.widget.TextView
    private var findListener: FindListener? = null

    private var isNavigationHidden = false
    private val scrollThreshold = 100
    private val searchDebounceHandler = Handler(Looper.getMainLooper())
    private var searchDebounceRunnable: Runnable? = null
    private var popupWebView: WebView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_browser, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        initViews(view)
        setupTabBar()
        setupButtonListeners()
        applyAccentColor()
        observeViewModel()

        if (savedInstanceState == null && browserViewModel.currentUrl.value.isNullOrEmpty()) {
            browserViewModel.loadUrl("https://www.bing.com")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.toolbar_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_find_in_page -> {
                openFindBar()
                true
            }
            else -> super.onOptionsItemSelected(item)
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
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        rvSuggestions = view.findViewById(R.id.rvSuggestions)
        rvSuggestions.layoutManager = LinearLayoutManager(requireContext())
        rvSuggestions.adapter = suggestionAdapter

        swipeRefresh.setOnRefreshListener { currentWebView?.reload() }

        popupToolbar.setNavigationOnClickListener { closePopup() }

        // Find bar
        findBar = view.findViewById(R.id.findBar)
        etFindQuery = view.findViewById(R.id.etFindQuery)
        tvFindCount = view.findViewById(R.id.tvFindCount)
        view.findViewById<MaterialButton>(R.id.btnFindPrev).setOnClickListener { currentWebView?.findNext(false) }
        view.findViewById<MaterialButton>(R.id.btnFindNext).setOnClickListener { currentWebView?.findNext(true) }
        view.findViewById<MaterialButton>(R.id.btnFindClose).setOnClickListener { closeFindBar() }
        etFindQuery.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                val query = s?.toString() ?: ""
                if (query.isNotEmpty()) currentWebView?.findAllAsync(query)
                else currentWebView?.clearMatches()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        etFindQuery.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                currentWebView?.findNext(true)
                true
            } else false
        }

        // Error page
        errorView = LayoutInflater.from(requireContext()).inflate(R.layout.view_error_page, webViewContainer, false)
        errorView!!.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnErrorRetry)
            .setOnClickListener { retryLoad() }
        errorView!!.visibility = View.GONE
        webViewContainer.addView(errorView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
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
                errorView?.visibility = View.GONE
                currentWebView?.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                swipeRefresh.isRefreshing = false
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
                    currentWebView?.visibility = View.GONE
                    errorView?.visibility = View.VISIBLE
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

        wv.setFindListener { activeMatchOrdinal, numberOfMatches, isDoneCounting ->
            if (isDoneCounting) {
                tvFindCount.text = if (numberOfMatches > 0) "$activeMatchOrdinal/$numberOfMatches" else "0/0"
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
        closeFindBar()
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

        // LRU eviction: keep max 5 saved WebView states
        while (webViewStates.size > maxSavedStates) {
            val oldestKey = webViewStates.keys.firstOrNull { it != tabId && it != currentTabId }
            if (oldestKey != null) webViewStates.remove(oldestKey) else break
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
                browserViewModel.clearSuggestions()
                browserViewModel.loadUrl(etUrl.text.toString())
                currentWebView?.requestFocus()
                true
            } else false
        }

        // Autocomplete debounce
        etUrl.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                searchDebounceRunnable?.let { searchDebounceHandler.removeCallbacks(it) }
                searchDebounceRunnable = Runnable {
                    browserViewModel.searchSuggestions(s?.toString() ?: "")
                }
                searchDebounceHandler.postDelayed(searchDebounceRunnable!!, 200)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
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
            // Clean up webViewStates for removed tabs
            val activeIds = tabs.map { it.id }.toSet()
            webViewStates.keys.removeAll { it !in activeIds }
        }

        browserViewModel.maxTabReached.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), "最多同时打开 20 个标签页", Toast.LENGTH_SHORT).show()
        }

        browserViewModel.suggestions.observe(viewLifecycleOwner) { suggestions ->
            suggestionAdapter.submitList(suggestions)
            rvSuggestions.visibility = if (suggestions.isNotEmpty()) View.VISIBLE else View.GONE
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

    private fun openFindBar() {
        findBar.visibility = View.VISIBLE
        etFindQuery.requestFocus()
    }

    private fun closeFindBar() {
        findBar.visibility = View.GONE
        currentWebView?.clearMatches()
        tvFindCount.text = "0/0"
        etFindQuery.text?.clear()
        etFindQuery.clearFocus()
    }

    private fun retryLoad() {
        errorView?.visibility = View.GONE
        currentWebView?.visibility = View.VISIBLE
        currentWebView?.reload()
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

    private fun loadSuggestion(item: com.knowlily.browser.viewmodel.SuggestItem) {
        browserViewModel.clearSuggestions()
        etUrl.setText(item.url)
        etUrl.setSelection(item.url.length)
        browserViewModel.loadUrl(item.url)
        currentWebView?.requestFocus()
    }
}

class SuggestionAdapter(
    private val onClick: (com.knowlily.browser.viewmodel.SuggestItem) -> Unit
) : androidx.recyclerview.widget.RecyclerView.Adapter<SuggestionAdapter.ViewHolder>() {

    private var items = listOf<com.knowlily.browser.viewmodel.SuggestItem>()

    fun submitList(list: List<com.knowlily.browser.viewmodel.SuggestItem>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_suggestion, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], onClick)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        private val tvUrl: android.widget.TextView = itemView.findViewById(R.id.tvSuggestionUrl)
        private val tvType: android.widget.TextView = itemView.findViewById(R.id.tvSuggestionType)

        fun bind(item: com.knowlily.browser.viewmodel.SuggestItem, onClick: (com.knowlily.browser.viewmodel.SuggestItem) -> Unit) {
            tvUrl.text = item.url
            tvType.text = if (item.type == com.knowlily.browser.viewmodel.SuggestType.HISTORY) "历史记录" else "书签"
            itemView.setOnClickListener { onClick(item) }
        }
    }
}
