package com.example.simplebrowser

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.DynamicColors
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BookmarksActivity : AppCompatActivity() {
    private lateinit var toolbar: MaterialToolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnAddBookmark: MaterialButton
    private lateinit var btnClearBookmarks: MaterialButton
    private lateinit var tvCurrentUrl: TextView
    private lateinit var adapter: BookmarksAdapter
    private val bookmarksList = mutableListOf<BookmarkItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 应用动态取色（Android 12+）
        DynamicColors.applyToActivityIfAvailable(this)

        // 应用保存的主题设置
        val sharedPref = getSharedPreferences("app_settings", MODE_PRIVATE)
        val savedTheme = sharedPref.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(savedTheme)

        setContentView(R.layout.activity_bookmarks)

        initViews()
        setupToolbar()
        applyAccentColor()
        setupButtonListeners()
        updateCurrentUrlDisplay()
        loadBookmarks()
        setupRecyclerView()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.recyclerView)
        btnAddBookmark = findViewById(R.id.btnAddBookmark)
        btnClearBookmarks = findViewById(R.id.btnClearBookmarks)
        tvCurrentUrl = findViewById(R.id.tvCurrentUrl)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun updateCurrentUrlDisplay() {
        val url = intent.getStringExtra("url")
        if (url != null && url.isNotEmpty()) {
            tvCurrentUrl.text = "当前网址: ${url.take(50)}${if (url.length > 50) "..." else ""}"
        } else {
            tvCurrentUrl.text = "当前网址: 无"
        }
    }

    private fun setupButtonListeners() {
        btnAddBookmark.setOnClickListener {
            // 从Intent获取传递过来的URL
            val url = intent.getStringExtra("url")
            if (url != null && url.isNotEmpty()) {
                addBookmark(url, System.currentTimeMillis())
            } else {
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "没有可添加的网址",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }

        btnClearBookmarks.setOnClickListener {
            clearBookmarks()
        }
    }

    private fun addBookmark(url: String, timestamp: Long) {
        try {
            val sharedPref = getSharedPreferences("browser_bookmarks", MODE_PRIVATE)
            val key = "$timestamp-$url"

            // 检查是否已存在相同URL的书签
            val existingUrls = sharedPref.all.values.filterIsInstance<String>()
            if (existingUrls.contains(url)) {
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "该书签已存在",
                    Snackbar.LENGTH_SHORT
                ).show()
                return
            }

            with(sharedPref.edit()) {
                putString(key, url)
                apply()
            }

            // 重新加载书签
            loadBookmarks()

            Snackbar.make(
                findViewById(android.R.id.content),
                "书签已添加",
                Snackbar.LENGTH_SHORT
            ).show()

        } catch (e: Exception) {
            Snackbar.make(
                findViewById(android.R.id.content),
                "添加书签失败: ${e.localizedMessage}",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private fun clearBookmarks() {
        try {
            val sharedPref = getSharedPreferences("browser_bookmarks", MODE_PRIVATE)
            sharedPref.edit().clear().apply()

            // 重新加载书签（会显示空状态）
            loadBookmarks()

            Snackbar.make(
                findViewById(android.R.id.content),
                "所有书签已清除",
                Snackbar.LENGTH_SHORT
            ).show()

        } catch (e: Exception) {
            Snackbar.make(
                findViewById(android.R.id.content),
                "清除书签失败: ${e.localizedMessage}",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private fun loadBookmarks() {
        // 从SharedPreferences加载书签
        val sharedPref = getSharedPreferences("browser_bookmarks", MODE_PRIVATE)
        val bookmarkEntries = sharedPref.all

        bookmarksList.clear()

        bookmarkEntries.forEach { (key, url) ->
            if (url is String) {
                // 从key中提取时间戳（格式：timestamp-url）
                val timestamp = try {
                    key.substringBefore("-").toLong()
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }
                bookmarksList.add(BookmarkItem(url, timestamp))
            }
        }

        // 按时间戳倒序排序（最新的在前）
        bookmarksList.sortByDescending { it.timestamp }

        // 如果没有书签，显示空状态
        if (bookmarksList.isEmpty()) {
            findViewById<TextView>(R.id.tvEmptyState).visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            findViewById<TextView>(R.id.tvEmptyState).visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }

        // 如果adapter已初始化，通知数据更新
        if (::adapter.isInitialized) {
            adapter.notifyDataSetChanged()
        }
    }

    private fun setupRecyclerView() {
        adapter = BookmarksAdapter(bookmarksList) { bookmarkItem ->
            // 点击书签项时，返回结果并关闭
            val resultIntent = android.content.Intent().apply {
                putExtra("url", bookmarkItem.url)
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun applyAccentColor() {
        val sharedPref = getSharedPreferences("app_settings", MODE_PRIVATE)
        val accentColor = sharedPref.getInt("accent_color", 0xFF6750A4.toInt())
        toolbar.setBackgroundColor(accentColor)
        findViewById<com.google.android.material.appbar.AppBarLayout>(R.id.appBarLayout)
            ?.setBackgroundColor(accentColor)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

data class BookmarkItem(
    val url: String,
    val timestamp: Long // 时间戳（毫秒）
)

class BookmarksAdapter(
    private val items: List<BookmarkItem>,
    private val onItemClick: (BookmarkItem) -> Unit
) : RecyclerView.Adapter<BookmarksAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvUrl: TextView = view.findViewById(R.id.tvUrl)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val root: View = view
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bookmark, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvUrl.text = item.url
        holder.tvTime.text = dateFormat.format(Date(item.timestamp))

        holder.root.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = items.size
}