package com.knowlily.browser.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.knowlily.browser.R
import com.knowlily.browser.adapter.BookmarksAdapter
import com.knowlily.browser.MainActivity
import com.knowlily.browser.viewmodel.BookmarksViewModel
import com.knowlily.browser.viewmodel.BrowserViewModel

@AndroidEntryPoint
class BookmarksFragment : Fragment() {

    private val bookmarksViewModel: BookmarksViewModel by activityViewModels()
    private val browserViewModel: BrowserViewModel by activityViewModels()

    private lateinit var toolbar: MaterialToolbar
    private lateinit var appBarLayout: AppBarLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var tvCurrentUrl: TextView
    private lateinit var btnAddBookmark: MaterialButton
    private lateinit var btnClearBookmarks: MaterialButton
    private lateinit var btnExportBookmarks: MaterialButton
    private lateinit var btnImportBookmarks: MaterialButton
    private lateinit var adapter: BookmarksAdapter

    private val importFilePicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { importFromUri(it) } }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_bookmarks, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar = view.findViewById(R.id.toolbar)
        appBarLayout = view.findViewById(R.id.appBarLayout)
        recyclerView = view.findViewById(R.id.recyclerView)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)
        tvCurrentUrl = view.findViewById(R.id.tvCurrentUrl)
        btnAddBookmark = view.findViewById(R.id.btnAddBookmark)
        btnClearBookmarks = view.findViewById(R.id.btnClearBookmarks)
        btnExportBookmarks = view.findViewById(R.id.btnExportBookmarks)
        btnImportBookmarks = view.findViewById(R.id.btnImportBookmarks)

        setupRecyclerView()
        observeViewModel()
        applyAccentColor()

        btnAddBookmark.setOnClickListener {
            val url = browserViewModel.currentUrl.value.orEmpty()
            if (url.isNotEmpty() && url != "about:blank") {
                bookmarksViewModel.addBookmark(url) { ok ->
                    Snackbar.make(requireView(),
                        if (ok) getString(R.string.bookmark_added) else getString(R.string.bookmark_already_exists),
                        Snackbar.LENGTH_SHORT).show()
                }
            } else {
                Snackbar.make(requireView(), getString(R.string.no_url_to_bookmark), Snackbar.LENGTH_SHORT).show()
            }
        }

        btnClearBookmarks.setOnClickListener {
            bookmarksViewModel.clearBookmarks()
            Snackbar.make(requireView(), getString(R.string.bookmarks_cleared), Snackbar.LENGTH_SHORT).show()
        }

        btnExportBookmarks.setOnClickListener {
            bookmarksViewModel.exportBookmarks { json ->
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_TEXT, json)
                }
                startActivity(Intent.createChooser(shareIntent, getString(R.string.export_bookmarks)))
            }
        }

        btnImportBookmarks.setOnClickListener {
            importFilePicker.launch(arrayOf("application/json", "*/*"))
        }
    }

    private fun setupRecyclerView() {
        adapter = BookmarksAdapter(mutableListOf()) { item ->
            browserViewModel.loadUrl(item.url)
            (requireActivity() as? MainActivity)?.switchToTab(R.id.navigation_home)
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun observeViewModel() {
        bookmarksViewModel.bookmarksList.observe(viewLifecycleOwner) { list ->
            adapter.items.clear()
            adapter.items.addAll(list)
            adapter.notifyDataSetChanged()
            tvEmptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            recyclerView.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
        }

        browserViewModel.currentUrl.observe(viewLifecycleOwner) { url ->
            tvCurrentUrl.text = if (!url.isNullOrEmpty() && url != "about:blank")
                getString(R.string.current_url_label, "${url.take(60)}${if (url.length > 60) "…" else ""}")
            else getString(R.string.no_current_url)
        }

        browserViewModel.accentColor.observe(viewLifecycleOwner) { color ->
            toolbar.setBackgroundColor(color)
            appBarLayout.setBackgroundColor(color)
        }
    }

    private fun importFromUri(uri: Uri) {
        try {
            val json = requireContext().contentResolver.openInputStream(uri)
                ?.bufferedReader()?.readText() ?: ""
            if (json.isBlank()) {
                Snackbar.make(requireView(), getString(R.string.file_empty), Snackbar.LENGTH_SHORT).show()
                return
            }
            bookmarksViewModel.importBookmarks(json) { count ->
                Snackbar.make(requireView(), getString(R.string.bookmarks_imported, count), Snackbar.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Snackbar.make(requireView(), getString(R.string.import_failed, e.localizedMessage), Snackbar.LENGTH_LONG).show()
        }
    }

    private fun applyAccentColor() {
        browserViewModel.accentColor.value?.let {
            toolbar.setBackgroundColor(it)
            appBarLayout.setBackgroundColor(it)
        }
    }
}
