package com.knowlily.browser.ui

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.knowlily.browser.R
import com.knowlily.browser.viewmodel.BrowserViewModel
import com.knowlily.browser.viewmodel.SettingsViewModel
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class SettingsFragment : Fragment() {

    private val settingsViewModel: SettingsViewModel by activityViewModels()
    private val browserViewModel: BrowserViewModel by activityViewModels()

    private lateinit var toolbar: MaterialToolbar
    private lateinit var appBarLayout: AppBarLayout
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
    ) { uri: Uri? -> uri?.let { installFromFileUri(it) } }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupButtonListeners()
        setupPluginList()
        observeViewModel()
        applyAccentColor()
    }

    private fun initViews(view: View) {
        toolbar = view.findViewById(R.id.toolbar)
        appBarLayout = view.findViewById(R.id.appBarLayout)
        btnClearCache = view.findViewById(R.id.btnClearCache)
        btnThemeLight = view.findViewById(R.id.btnThemeLight)
        btnThemeDark = view.findViewById(R.id.btnThemeDark)
        btnThemeSystem = view.findViewById(R.id.btnThemeSystem)
        btnUserAgentPC = view.findViewById(R.id.btnUserAgentPC)
        btnUserAgentMobile = view.findViewById(R.id.btnUserAgentMobile)
        btnInstallFromUrl = view.findViewById(R.id.btnInstallFromUrl)
        btnInstallFromFile = view.findViewById(R.id.btnInstallFromFile)
        pluginContainer = view.findViewById(R.id.pluginContainer)

        view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardAbout).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/knowlily/listen-to")))
        }
    }

    private fun setupButtonListeners() {
        btnClearCache.setOnClickListener {
            settingsViewModel.clearCache(requireContext())
            Snackbar.make(requireView(), "缓存已清除", Snackbar.LENGTH_SHORT).show()
        }

        btnThemeLight.setOnClickListener {
            settingsViewModel.setTheme(AppCompatDelegate.MODE_NIGHT_NO)
            requireActivity().recreate()
        }
        btnThemeDark.setOnClickListener {
            settingsViewModel.setTheme(AppCompatDelegate.MODE_NIGHT_YES)
            requireActivity().recreate()
        }
        btnThemeSystem.setOnClickListener {
            settingsViewModel.setTheme(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            requireActivity().recreate()
        }

        btnUserAgentPC.setOnClickListener { settingsViewModel.setUserAgent("pc") }
        btnUserAgentMobile.setOnClickListener { settingsViewModel.setUserAgent("mobile") }

        btnInstallFromUrl.setOnClickListener { showUrlInstallDialog() }
        btnInstallFromFile.setOnClickListener {
            filePickerLauncher.launch(arrayOf("application/json", "*/*"))
        }

        for ((btnId, color) in colorPresets) {
            requireView().findViewById<MaterialButton>(btnId).setOnClickListener {
                settingsViewModel.setAccentColor(color)
            }
        }
    }

    private fun observeViewModel() {
        settingsViewModel.themeMode.observe(viewLifecycleOwner) { updateThemeButtons(it) }
        settingsViewModel.userAgentMode.observe(viewLifecycleOwner) { updateUAButtons(it) }
        settingsViewModel.accentColor.observe(viewLifecycleOwner) { updateColorButtons(it) }
        settingsViewModel.plugins.observe(viewLifecycleOwner) { setupPluginList() }

        browserViewModel.accentColor.observe(viewLifecycleOwner) { color ->
            toolbar.setBackgroundColor(color)
            appBarLayout.setBackgroundColor(color)
        }
    }

    private fun applyAccentColor() {
        browserViewModel.accentColor.value?.let {
            toolbar.setBackgroundColor(it)
            appBarLayout.setBackgroundColor(it)
        }
    }

    private fun getPrimaryColor(): Int {
        val attrs = intArrayOf(androidx.appcompat.R.attr.colorPrimary)
        val typedArray = requireContext().obtainStyledAttributes(attrs)
        val color = typedArray.getColor(0, Color.parseColor("#6750A4"))
        typedArray.recycle()
        return color
    }

    private fun updateThemeButtons(mode: Int) {
        val primary = getPrimaryColor()
        fun apply(btn: MaterialButton, active: Boolean) {
            if (active) {
                btn.setBackgroundColor(primary); btn.setTextColor(Color.WHITE); btn.strokeWidth = 0
            } else {
                btn.setBackgroundColor(Color.TRANSPARENT); btn.setTextColor(primary)
                btn.strokeWidth = dpToPx(1); btn.strokeColor = android.content.res.ColorStateList.valueOf(primary)
            }
        }
        apply(btnThemeLight, mode == AppCompatDelegate.MODE_NIGHT_NO)
        apply(btnThemeDark, mode == AppCompatDelegate.MODE_NIGHT_YES)
        apply(btnThemeSystem, mode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

    private fun updateUAButtons(mode: String) {
        val primary = getPrimaryColor()
        fun apply(btn: MaterialButton, active: Boolean) {
            if (active) {
                btn.setBackgroundColor(primary); btn.setTextColor(Color.WHITE); btn.strokeWidth = 0
            } else {
                btn.setBackgroundColor(Color.TRANSPARENT); btn.setTextColor(primary)
                btn.strokeWidth = dpToPx(1); btn.strokeColor = android.content.res.ColorStateList.valueOf(primary)
            }
        }
        apply(btnUserAgentPC, mode == "pc")
        apply(btnUserAgentMobile, mode == "mobile")
    }

    private fun updateColorButtons(active: Int) {
        for ((btnId, color) in colorPresets) {
            val btn = requireView().findViewById<MaterialButton>(btnId)
            if (color == active) {
                btn.strokeWidth = dpToPx(4); btn.strokeColor = android.content.res.ColorStateList.valueOf(Color.WHITE)
            } else {
                btn.strokeWidth = dpToPx(1); btn.strokeColor = android.content.res.ColorStateList.valueOf(0x33000000)
            }
        }
    }

    private fun setupPluginList() {
        pluginContainer.removeAllViews()
        val plugins = settingsViewModel.plugins.value ?: return

        for (plugin in plugins) {
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = if (pluginContainer.childCount > 0) dpToPx(8) else 0 }
                gravity = android.view.Gravity.CENTER_VERTICAL
            }

            val textBlock = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val nameView = TextView(requireContext()).apply {
                text = plugin.name; textSize = 14f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface))
            }

            val descView = TextView(requireContext()).apply {
                text = if (settingsViewModel.isBuiltinPlugin(plugin.id))
                    "${plugin.description}  v${plugin.version}"
                else "${plugin.description}  v${plugin.version}  (用户安装)"
                textSize = 12f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface_variant))
            }

            textBlock.addView(nameView)
            textBlock.addView(descView)

            val toggle = SwitchMaterial(requireContext()).apply {
                isChecked = plugin.isEnabled
                setOnCheckedChangeListener { _, en ->
                    settingsViewModel.togglePlugin(plugin.id, en)
                }
            }

            row.addView(textBlock)
            row.addView(toggle)

            if (!settingsViewModel.isBuiltinPlugin(plugin.id)) {
                val uninstallBtn = MaterialButton(requireContext()).apply {
                    text = "卸载"; textSize = 12f
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { marginStart = dpToPx(8) }
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface_variant))
                    setBackgroundColor(Color.TRANSPARENT)
                    strokeWidth = dpToPx(1)
                    strokeColor = android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.on_surface_variant))
                    setOnClickListener { confirmUninstall(plugin.id, plugin.name) }
                }
                row.addView(uninstallBtn)
            }

            pluginContainer.addView(row)
        }
    }

    private fun confirmUninstall(id: String, name: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("卸载插件")
            .setMessage("确定要卸载「$name」吗？")
            .setPositiveButton("卸载") { _, _ ->
                settingsViewModel.uninstallPlugin(id)
                Snackbar.make(requireView(), "插件已卸载", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showUrlInstallDialog() {
        val input = EditText(requireContext()).apply {
            hint = "https://example.com/plugin.json"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(dpToPx(16), dpToPx(8), dpToPx(16), 0) }
        }

        MaterialAlertDialogBuilder(requireContext())
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
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("正在下载...").setMessage("请稍候").setCancelable(false).create()
        dialog.show()

        Thread {
            try {
                val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10000; readTimeout = 10000
                }
                val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
                val json = reader.readText()
                reader.close(); conn.disconnect()

                requireActivity().runOnUiThread {
                    dialog.dismiss()
                    installPlugin(json)
                }
            } catch (e: Exception) {
                requireActivity().runOnUiThread {
                    dialog.dismiss()
                    Snackbar.make(requireView(), "下载失败: ${e.localizedMessage}", Snackbar.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun installFromFileUri(uri: Uri) {
        try {
            val json = requireContext().contentResolver.openInputStream(uri)
                ?.bufferedReader()?.readText() ?: ""
            if (json.isNotBlank()) installPlugin(json)
        } catch (e: Exception) {
            Snackbar.make(requireView(), "读取文件失败: ${e.localizedMessage}", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun installPlugin(json: String) {
        settingsViewModel.installPlugin(json).fold(
            onSuccess = {
                Snackbar.make(requireView(), "插件安装成功", Snackbar.LENGTH_SHORT).show()
            },
            onFailure = { e ->
                Snackbar.make(requireView(), "安装失败: ${e.localizedMessage}", Snackbar.LENGTH_LONG).show()
            }
        )
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}
