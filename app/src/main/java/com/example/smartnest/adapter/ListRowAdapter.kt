package com.example.smartnest.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smartnest.R
import com.example.smartnest.model.ListRowItem

class ListRowAdapter(
    private val items: List<ListRowItem>,
    private val onClick: (ListRowItem) -> Unit
) : RecyclerView.Adapter<ListRowAdapter.RowViewHolder>() {

    inner class RowViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.ivRowIcon)
        val title: TextView = view.findViewById(R.id.tvRowTitle)
        val subtitle: TextView = view.findViewById(R.id.tvRowSubtitle)
        val backgroundImage: ImageView = view.findViewById(R.id.ivHomeImage) // Find the background
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_row_card, parent, false)
        return RowViewHolder(view)
    }

    override fun onBindViewHolder(holder: RowViewHolder, position: Int) {
        val item = items[position]
        holder.icon.setImageResource(item.iconRes)
        holder.title.text = item.title
        holder.subtitle.text = item.subtitle

        // Set the dynamic background image here
        holder.backgroundImage.setImageResource(item.backgroundRes)

        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size
}