package com.knowlily.browser.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.knowlily.browser.R
import com.knowlily.browser.adapter.HistoryAdapter
import com.knowlily.browser.MainActivity
import com.knowlily.browser.viewmodel.BrowserViewModel
import com.knowlily.browser.viewmodel.HistoryViewModel

class HistoryFragment : Fragment() {

    private val historyViewModel: HistoryViewModel by activityViewModels()
    private val browserViewModel: BrowserViewModel by activityViewModels()

    private lateinit var toolbar: MaterialToolbar
    private lateinit var appBarLayout: AppBarLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmptyState: TextView
    private lateinit var btnClearHistory: MaterialButton
    private lateinit var adapter: HistoryAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar = view.findViewById(R.id.toolbar)
        appBarLayout = view.findViewById(R.id.appBarLayout)
        recyclerView = view.findViewById(R.id.recyclerView)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)
        btnClearHistory = view.findViewById(R.id.btnClearHistory)

        setupRecyclerView()
        observeViewModel()
        applyAccentColor()

        btnClearHistory.setOnClickListener {
            historyViewModel.clearHistory()
            Snackbar.make(requireView(), "历史记录已清除", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        adapter = HistoryAdapter(mutableListOf()) { item ->
            browserViewModel.loadUrl(item.url)
            (requireActivity() as? MainActivity)?.switchToTab(R.id.navigation_home)
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun observeViewModel() {
        historyViewModel.historyList.observe(viewLifecycleOwner) { list ->
            adapter.items.clear()
            adapter.items.addAll(list)
            adapter.notifyDataSetChanged()
            tvEmptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            recyclerView.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
        }

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
}
