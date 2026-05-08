package com.example.simplebrowser

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.color.DynamicColors
import com.google.android.material.switchmaterial.SwitchMaterial
import com.example.simplebrowser.plugin.PluginManager
import com.example.simplebrowser.plugin.UserPluginConfig
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class SettingsActivity : AppCompatActivity() {
    private lateinit var toolbar: MaterialToolbar
    private lateinit var btnClearCache: MaterialButton
    private lateinit var btnThemeLight: MaterialButton
    private lateinit var btnThemeDark: MaterialButton
    private lateinit var btnThemeSystem: MaterialButton
    private lateinit var btnUserAgentPC: MaterialButton
    private lateinit var btnUserAgentMobile: MaterialButton
    private lateinit var btnInstallFromUrl: MaterialButton
    private lateinit var btnInstallFromFile: MaterialButton
    private lateinit var pluginContainer: LinearLayout

    private val colorPresets = mapOf(
        R.id.btnColorPurple to 0xFF6750A4.toInt(),
        R.id.btnColorBlue to 0xFF1976D2.toInt(),
        R.id.btnColorTeal to 0xFF00796B.toInt(),
        R.id.btnColorRed to 0xFFC62828.toInt(),
        R.id.btnColorOrange to 0xFFE65100.toInt(),
        R.id.btnColorGreen to 0xFF2E7D32.toInt(),
        R.id.btnColorPink to 0xFFAD1457.toInt()
    )

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { installFromFileUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DynamicColors.applyToActivityIfAvailable(this)

        val sharedPref = getSharedPreferences("app_settings", MODE_PRIVATE)
        val savedTheme = sharedPref.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(savedTheme)

        setContentView(R.layout.activity_settings)

        initViews()
        setupToolbar()
        applyAccentColor()
        setupButtonListeners()
        setupPluginList()
        updateButtonStates()
    }

    private fun applyAccentColor() {
        val sharedPref = getSharedPreferences("app_settings", MODE_PRIVATE)
        val accentColor = sharedPref.getInt("accent_color", 0xFF6750A4.toInt())
        toolbar.setBackgroundColor(accentColor)
        findViewById<com.google.android.material.appbar.AppBarLayout>(R.id.appBarLayout)
            ?.setBackgroundColor(accentColor)
    }

    private fun updateButtonStates() {
        val sharedPref = getSharedPreferences("app_settings", MODE_PRIVATE)
        val currentTheme = sharedPref.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        val currentUA = sharedPref.getString("user_agent_mode", "mobile") ?: "mobile"
        val currentColor = sharedPref.getInt("accent_color", 0xFF6750A4.toInt())

        updateThemeButtonStyles(currentTheme)
        updateUAButtonStyles(currentUA)
        updateColorButtonStates(currentColor)
    }

    private fun updateColorButtonStates(activeColor: Int) {
        for ((btnId, color) in colorPresets) {
            val btn = findViewById<MaterialButton>(btnId)
            if (color == activeColor) {
                btn.strokeWidth = dpToPx(4)
                btn.strokeColor = android.content.res.ColorStateList.valueOf(Color.WHITE)
            } else {
                btn.strokeWidth = dpToPx(1)
                btn.strokeColor = android.content.res.ColorStateList.valueOf(0x33000000)
            }
        }
    }

    private fun getPrimaryColor(): Int {
        val attrs = intArrayOf(androidx.appcompat.R.attr.colorPrimary)
        val typedArray = obtainStyledAttributes(attrs)
        val color = typedArray.getColor(0, Color.parseColor("#6750A4"))
        typedArray.recycle()
        return color
    }

    private fun updateThemeButtonStyles(activeMode: Int) {
        val primaryColor = getPrimaryColor()

        fun applyButton(btn: MaterialButton, isActive: Boolean) {
            if (isActive) {
                btn.setBackgroundColor(primaryColor)
                btn.setTextColor(Color.WHITE)
                btn.strokeWidth = 0
            } else {
                btn.setBackgroundColor(Color.TRANSPARENT)
                btn.setTextColor(primaryColor)
                btn.strokeWidth = dpToPx(1)
                btn.strokeColor = android.content.res.ColorStateList.valueOf(primaryColor)
            }
        }

        applyButton(btnThemeLight, activeMode == AppCompatDelegate.MODE_NIGHT_NO)
        applyButton(btnThemeDark, activeMode == AppCompatDelegate.MODE_NIGHT_YES)
        applyButton(btnThemeSystem, activeMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

    private fun updateUAButtonStyles(activeMode: String) {
        val primaryColor = getPrimaryColor()

        fun applyButton(btn: MaterialButton, isActive: Boolean) {
            if (isActive) {
                btn.setBackgroundColor(primaryColor)
                btn.setTextColor(Color.WHITE)
                btn.strokeWidth = 0
            } else {
                btn.setBackgroundColor(Color.TRANSPARENT)
                btn.setTextColor(primaryColor)
                btn.strokeWidth = dpToPx(1)
                btn.strokeColor = android.content.res.ColorStateList.valueOf(primaryColor)
            }
        }

        applyButton(btnUserAgentPC, activeMode == "pc")
        applyButton(btnUserAgentMobile, activeMode == "mobile")
    }

    private fun setupPluginList() {
        pluginContainer.removeAllViews()
        val pm = PluginManager.getInstance(this)

        for (plugin in pm.getPlugins()) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = if (pluginContainer.childCount > 0) dpToPx(8) else 0
                }
                gravity = android.view.Gravity.CENTER_VERTICAL
            }

            val textBlock = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            val nameView = TextView(this).apply {
                text = plugin.name
                textSize = 14f
                setTextColor(ContextCompat.getColor(context, R.color.on_surface))
            }

            val descView = TextView(this).apply {
                text = if (pm.isBuiltinPlugin(plugin.id)) {
                    "${plugin.description}  v${plugin.version}"
                } else {
                    "${plugin.description}  v${plugin.version}  (用户安装)"
                }
                textSize = 12f
                setTextColor(ContextCompat.getColor(context, R.color.on_surface_variant))
            }

            textBlock.addView(nameView)
            textBlock.addView(descView)

            val toggle = SwitchMaterial(this).apply {
                isChecked = plugin.isEnabled
                setOnCheckedChangeListener { _, enabled ->
                    if (enabled) pm.enablePlugin(plugin.id) else pm.disablePlugin(plugin.id)
                }
            }

            row.addView(textBlock)
            row.addView(toggle)

            if (!pm.isBuiltinPlugin(plugin.id)) {
                val uninstallBtn = MaterialButton(this).apply {
                    text = "卸载"
                    textSize = 12f
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { marginStart = dpToPx(8) }
                    setTextColor(ContextCompat.getColor(context, R.color.on_surface_variant))
                    setBackgroundColor(Color.TRANSPARENT)
                    strokeWidth = dpToPx(1)
                    strokeColor = android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.on_surface_variant)
                    )
                    setOnClickListener {
                        confirmUninstall(plugin.id, plugin.name)
                    }
                }
                row.addView(uninstallBtn)
            }

            pluginContainer.addView(row)
        }
    }

    private fun confirmUninstall(id: String, name: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("卸载插件")
            .setMessage("确定要卸载「$name」吗？")
            .setPositiveButton("卸载") { _, _ ->
                PluginManager.getInstance(this).uninstallUserPlugin(id)
                setupPluginList()
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "插件已卸载",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showUrlInstallDialog() {
        val input = EditText(this).apply {
            hint = "https://example.com/plugin.json"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(dpToPx(16), dpToPx(8), dpToPx(16), 0)
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("从 URL 安装插件")
            .setMessage("输入插件 JSON 配置文件的 URL 地址")
            .setView(input)
            .setPositiveButton("安装") { _, _ ->
                val url = input.text.toString().trim()
                if (url.isNotEmpty()) installFromUrl(url)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun installFromUrl(urlString: String) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("正在下载...")
            .setMessage("请稍候")
            .setCancelable(false)
            .create()
        dialog.show()

        Thread {
            try {
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
                val json = reader.readText()
                reader.close()
                conn.disconnect()

                runOnUiThread {
                    dialog.dismiss()
                    installPlugin(json)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    dialog.dismiss()
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "下载失败: ${e.localizedMessage}",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    private fun installFromFileUri(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val json = inputStream?.bufferedReader()?.readText() ?: ""
            inputStream?.close()
            if (json.isNotBlank()) installPlugin(json)
        } catch (e: Exception) {
            Snackbar.make(
                findViewById(android.R.id.content),
                "读取文件失败: ${e.localizedMessage}",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private fun installPlugin(json: String) {
        val result = PluginManager.getInstance(this).installPlugin(json)
        result.fold(
            onSuccess = { id ->
                setupPluginList()
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "插件安装成功: $id",
                    Snackbar.LENGTH_SHORT
                ).show()
            },
            onFailure = { e ->
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "安装失败: ${e.localizedMessage}",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        btnClearCache = findViewById(R.id.btnClearCache)
        btnThemeLight = findViewById(R.id.btnThemeLight)
        btnThemeDark = findViewById(R.id.btnThemeDark)
        btnThemeSystem = findViewById(R.id.btnThemeSystem)
        btnUserAgentPC = findViewById(R.id.btnUserAgentPC)
        btnUserAgentMobile = findViewById(R.id.btnUserAgentMobile)
        btnInstallFromUrl = findViewById(R.id.btnInstallFromUrl)
        btnInstallFromFile = findViewById(R.id.btnInstallFromFile)
        pluginContainer = findViewById(R.id.pluginContainer)

        findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardAbout).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/knowlily/listen-to"))
            startActivity(intent)
        }

        for (btnId in colorPresets.keys) {
            findViewById<MaterialButton>(btnId)
        }
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

        btnUserAgentPC.setOnClickListener {
            setUserAgentMode("pc")
        }

        btnUserAgentMobile.setOnClickListener {
            setUserAgentMode("mobile")
        }

        btnInstallFromUrl.setOnClickListener {
            showUrlInstallDialog()
        }

        btnInstallFromFile.setOnClickListener {
            filePickerLauncher.launch(arrayOf("application/json", "*/*"))
        }

        for ((btnId, color) in colorPresets) {
            findViewById<MaterialButton>(btnId).setOnClickListener {
                setAccentColor(color)
            }
        }
    }

    private fun clearWebViewCache() {
        try {
            WebStorage.getInstance().deleteAllData()

            val cookieManager = CookieManager.getInstance()
            cookieManager.removeAllCookies(null)
            cookieManager.flush()

            deleteDatabase("webview.db")
            deleteDatabase("webviewCache.db")

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

        val sharedPref = getSharedPreferences("app_settings", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("theme_mode", mode)
            apply()
        }

        updateThemeButtonStyles(mode)

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

    private fun setAccentColor(color: Int) {
        val sharedPref = getSharedPreferences("app_settings", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("accent_color", color)
            apply()
        }

        updateColorButtonStates(color)

        toolbar.setBackgroundColor(color)
        findViewById<com.google.android.material.appbar.AppBarLayout>(R.id.appBarLayout)
            ?.setBackgroundColor(color)

        val colorNames = mapOf(
            0xFF6750A4.toInt() to "紫色",
            0xFF1976D2.toInt() to "蓝色",
            0xFF00796B.toInt() to "青色",
            0xFFC62828.toInt() to "红色",
            0xFFE65100.toInt() to "橙色",
            0xFF2E7D32.toInt() to "绿色",
            0xFFAD1457.toInt() to "粉色"
        )

        Snackbar.make(
            findViewById(android.R.id.content),
            "主题色已切换为${colorNames[color] ?: "自定义"}",
            Snackbar.LENGTH_SHORT
        ).show()
    }

    private fun setUserAgentMode(mode: String) {
        val sharedPref = getSharedPreferences("app_settings", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("user_agent_mode", mode)
            apply()
        }

        updateUAButtonStyles(mode)

        val message = when (mode) {
            "pc" -> "已切换到电脑模式"
            "mobile" -> "已切换到手机模式"
            else -> "浏览器标识已更新"
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
