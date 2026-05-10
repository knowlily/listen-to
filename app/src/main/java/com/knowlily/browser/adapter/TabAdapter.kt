package com.knowlily.browser.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.knowlily.browser.R
import com.knowlily.browser.model.TabItem

class TabAdapter(
    private val onTabClick: (TabItem) -> Unit,
    private val onTabClose: (TabItem) -> Unit,
    private val onAddTab: () -> Unit,
    private val onAddIncognitoTab: () -> Unit
) : ListAdapter<TabItem, RecyclerView.ViewHolder>(TabDiffCallback()) {

    companion object {
        private const val TYPE_TAB = 0
        private const val TYPE_ADD = 1
    }

    var activeTabId: Int = 0
        set(value) {
            if (field != value) {
                val old = field
                field = value
                notifyItemChanged(indexOfId(old))
                notifyItemChanged(indexOfId(value))
            }
        }

    private var accentColor: Int = Color.parseColor("#6750A4")

    fun setAccentColor(color: Int) {
        accentColor = color
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < currentList.size) TYPE_TAB else TYPE_ADD
    }

    override fun getItemCount(): Int = currentList.size + 1 // +1 for add button

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_ADD) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_tab, parent, false)
            AddViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_tab, parent, false)
            TabViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is TabViewHolder) {
            val tab = currentList[position]
            holder.bind(tab)
        } else if (holder is AddViewHolder) {
            holder.bind()
        }
    }

    private fun indexOfId(id: Int): Int = currentList.indexOfFirst { it.id == id }

    inner class TabViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTabTitle)
        private val btnClose: ImageButton = itemView.findViewById(R.id.btnCloseTab)

        fun bind(tab: TabItem) {
            tvTitle.text = tab.title
            btnClose.visibility = if (currentList.size > 1) View.VISIBLE else View.GONE
            btnClose.setOnClickListener { onTabClose(tab) }
            itemView.setOnClickListener { onTabClick(tab) }

            val card = itemView as com.google.android.material.card.MaterialCardView
            val isActive = tab.id == activeTabId

            if (tab.isIncognito) {
                card.setCardBackgroundColor(Color.parseColor("#2C2C2C"))
                tvTitle.setTextColor(Color.WHITE)
                btnClose.imageTintList = ColorStateList.valueOf(Color.WHITE)
            } else if (isActive) {
                card.setCardBackgroundColor(accentColor)
                tvTitle.setTextColor(Color.WHITE)
                btnClose.imageTintList = ColorStateList.valueOf(Color.WHITE)
            } else {
                card.setCardBackgroundColor(Color.TRANSPARENT)
                card.strokeWidth = 1
                card.strokeColor = accentColor and 0x40FFFFFF or 0x20000000
                tvTitle.setTextColor(itemView.context.getColor(R.color.on_surface))
                btnClose.imageTintList = ColorStateList.valueOf(
                    itemView.context.getColor(R.color.on_surface_variant)
                )
            }
        }
    }

    inner class AddViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind() {
            val tvTitle: TextView = itemView.findViewById(R.id.tvTabTitle)
            val btnClose: ImageButton = itemView.findViewById(R.id.btnCloseTab)
            tvTitle.text = "+"
            btnClose.visibility = View.GONE

            val card = itemView as com.google.android.material.card.MaterialCardView
            card.setCardBackgroundColor(Color.TRANSPARENT)
            card.strokeWidth = 1
            card.strokeColor = accentColor and 0x40FFFFFF or 0x20000000
            tvTitle.setTextColor(itemView.context.getColor(R.color.on_surface_variant))

            itemView.setOnClickListener { onAddTab() }
            itemView.setOnLongClickListener {
                onAddIncognitoTab()
                true
            }
        }
    }

    private class TabDiffCallback : DiffUtil.ItemCallback<TabItem>() {
        override fun areItemsTheSame(old: TabItem, new: TabItem): Boolean = old.id == new.id
        override fun areContentsTheSame(old: TabItem, new: TabItem): Boolean = old == new
    }
}
