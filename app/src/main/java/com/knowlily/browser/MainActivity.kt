package com.knowlily.browser

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.DynamicColors
import com.knowlily.browser.ui.BrowserFragment
import com.knowlily.browser.ui.HistoryFragment
import com.knowlily.browser.ui.BookmarksFragment
import com.knowlily.browser.ui.SettingsFragment
import com.knowlily.browser.viewmodel.BrowserViewModel
import com.knowlily.browser.repository.SettingsRepository

class MainActivity : AppCompatActivity() {

    private lateinit var browserViewModel: BrowserViewModel
    private lateinit var browserFragment: BrowserFragment
    private lateinit var historyFragment: HistoryFragment
    private lateinit var bookmarksFragment: BookmarksFragment
    private lateinit var settingsFragment: SettingsFragment
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var cardBottomBar: MaterialCardView

    private var selectedTabId = R.id.navigation_home

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved theme before super.onCreate
        val settingsRepo = SettingsRepository(applicationContext)
        AppCompatDelegate.setDefaultNightMode(settingsRepo.getThemeMode())

        super.onCreate(savedInstanceState)

        DynamicColors.applyToActivityIfAvailable(this)

        setContentView(R.layout.activity_main)

        browserViewModel = ViewModelProvider(this)[BrowserViewModel::class.java]

        cardBottomBar = findViewById(R.id.cardBottomBar)
        bottomNav = findViewById(R.id.bottomNavigationView)

        savedInstanceState?.let {
            selectedTabId = it.getInt("selectedTab", R.id.navigation_home)
        }

        if (savedInstanceState == null) {
            browserFragment = BrowserFragment()
            historyFragment = HistoryFragment()
            bookmarksFragment = BookmarksFragment()
            settingsFragment = SettingsFragment()

            supportFragmentManager.beginTransaction()
                .add(R.id.fragmentContainer, browserFragment, TAG_BROWSER)
                .add(R.id.fragmentContainer, historyFragment, TAG_HISTORY)
                .add(R.id.fragmentContainer, bookmarksFragment, TAG_BOOKMARKS)
                .add(R.id.fragmentContainer, settingsFragment, TAG_SETTINGS)
                .hide(historyFragment)
                .hide(bookmarksFragment)
                .hide(settingsFragment)
                .commit()
        } else {
            browserFragment = supportFragmentManager.findFragmentByTag(TAG_BROWSER) as BrowserFragment
            historyFragment = supportFragmentManager.findFragmentByTag(TAG_HISTORY) as HistoryFragment
            bookmarksFragment = supportFragmentManager.findFragmentByTag(TAG_BOOKMARKS) as BookmarksFragment
            settingsFragment = supportFragmentManager.findFragmentByTag(TAG_SETTINGS) as SettingsFragment
        }

        setupBottomNavigation()
        observeViewModel()
    }

    private fun setupBottomNavigation() {
        bottomNav.selectedItemId = selectedTabId

        bottomNav.setOnItemSelectedListener { item ->
            selectedTabId = item.itemId
            val tx = supportFragmentManager.beginTransaction()
            hideAllFragments(tx)
            when (item.itemId) {
                R.id.navigation_home -> tx.show(browserFragment)
                R.id.navigation_bookmarks -> tx.show(bookmarksFragment)
                R.id.navigation_history -> tx.show(historyFragment)
                R.id.navigation_settings -> tx.show(settingsFragment)
            }
            tx.commit()
            true
        }

        bottomNav.setOnItemReselectedListener { item ->
            if (item.itemId == R.id.navigation_home) {
                browserViewModel.loadUrl("https://www.bing.com")
            }
        }
    }

    private fun hideAllFragments(tx: androidx.fragment.app.FragmentTransaction) {
        tx.hide(browserFragment)
        tx.hide(historyFragment)
        tx.hide(bookmarksFragment)
        tx.hide(settingsFragment)
    }

    private fun observeViewModel() {
        browserViewModel.isBottomNavVisible.observe(this) { visible ->
            if (visible) {
                cardBottomBar.visibility = View.VISIBLE
                cardBottomBar.animate().alpha(1f).translationY(0f).setDuration(250).start()
            } else {
                cardBottomBar.animate().alpha(0f).translationY(cardBottomBar.height.toFloat()).setDuration(250)
                    .withEndAction { cardBottomBar.visibility = View.INVISIBLE }.start()
            }
        }

        browserViewModel.accentColor.observe(this) { color ->
            cardBottomBar.setCardBackgroundColor(color)
            bottomNav.itemIconTintList = android.content.res.ColorStateList.valueOf(
                if (isColorDark(color)) android.graphics.Color.WHITE else android.graphics.Color.BLACK
            )
            bottomNav.itemTextColor = bottomNav.itemIconTintList
        }
    }

    fun switchToTab(tabId: Int) {
        bottomNav.selectedItemId = tabId
    }

    override fun onBackPressed() {
        if (browserFragment.handleBackPressed()) return
        if (selectedTabId != R.id.navigation_home) {
            bottomNav.selectedItemId = R.id.navigation_home
            return
        }
        super.onBackPressed()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("selectedTab", selectedTabId)
    }

    private fun isColorDark(color: Int): Boolean {
        val grey = (0.299 * android.graphics.Color.red(color) +
                     0.587 * android.graphics.Color.green(color) +
                     0.114 * android.graphics.Color.blue(color))
        return grey < 128
    }

    companion object {
        private const val TAG_BROWSER = "BrowserFragment"
        private const val TAG_HISTORY = "HistoryFragment"
        private const val TAG_BOOKMARKS = "BookmarksFragment"
        private const val TAG_SETTINGS = "SettingsFragment"
    }
}
