package com.knowlily.browser.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.knowlily.browser.R
import com.knowlily.browser.model.BookmarkItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BookmarksAdapter(
    val items: MutableList<BookmarkItem>,
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
        holder.root.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = items.size

}
