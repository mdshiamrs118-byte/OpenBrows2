package com.openbrows.app.ui.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.openbrows.app.R
import com.openbrows.app.data.TabModel

class TabsAdapter(
    private val isHorizontal: Boolean,
    private val onTabClick: (TabModel) -> Unit,
    private val onCloseClick: (TabModel) -> Unit
) : RecyclerView.Adapter<TabsAdapter.TabViewHolder>() {

    private var tabs: List<TabModel> = emptyList()
    private var currentId: String? = null

    inner class TabViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView =
            view.findViewById(if (isHorizontal) R.id.tvTabTitle else R.id.tvVerticalTabTitle)
        val btnClose: ImageButton =
            view.findViewById(if (isHorizontal) R.id.btnCloseTab else R.id.btnCloseVerticalTab)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun update(newTabs: List<TabModel>, newCurrentId: String?) {
        tabs = newTabs
        currentId = newCurrentId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabViewHolder {
        val layoutRes = if (isHorizontal) R.layout.item_tab_horizontal else R.layout.item_tab_vertical
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return TabViewHolder(view)
    }

    override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
        val tab = tabs[position]
        holder.tvTitle.text = when {
            tab.title.isNotEmpty() -> tab.title
            tab.url.isNotEmpty() -> tab.url
            else -> holder.itemView.context.getString(R.string.new_tab_title)
        }
        holder.itemView.alpha = if (tab.id == currentId) 1f else 0.6f
        holder.itemView.setOnClickListener { onTabClick(tab) }
        holder.btnClose.setOnClickListener { onCloseClick(tab) }
    }

    override fun getItemCount(): Int = tabs.size
}
