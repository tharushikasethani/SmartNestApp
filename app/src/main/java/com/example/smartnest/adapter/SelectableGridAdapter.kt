package com.example.smartnest.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smartnest.R
import com.example.smartnest.model.SelectableItem
import com.google.android.material.card.MaterialCardView

class SelectableGridAdapter(
    private val items: List<SelectableItem>,
    private val showLabel: Boolean = true,
    private var selectedIndex: Int = -1,
    private val onSelected: (SelectableItem, Int) -> Unit
) : RecyclerView.Adapter<SelectableGridAdapter.CardViewHolder>() {

    inner class CardViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.cardRoot)
        val icon: ImageView = view.findViewById(R.id.ivIcon)
        val label: TextView = view.findViewById(R.id.tvLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_selectable_card, parent, false)
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val item = items[position]
        holder.icon.setImageResource(item.iconRes)
        holder.label.text = item.label
        holder.label.visibility = if (showLabel) View.VISIBLE else View.GONE

        val isSelected = position == selectedIndex
        if (isSelected) {
            holder.card.setCardBackgroundColor(holder.itemView.context.getColor(R.color.orange_light))
            holder.card.strokeColor = holder.itemView.context.getColor(R.color.orange_primary)
            holder.card.strokeWidth = 4 // thicker border for selected
        } else {
            holder.card.setCardBackgroundColor(Color.WHITE)
            holder.card.strokeColor = Color.parseColor("#ECECEC")
            holder.card.strokeWidth = 2
        }

        holder.itemView.setOnClickListener {
            val previous = selectedIndex
            selectedIndex = holder.adapterPosition
            if (previous != -1) notifyItemChanged(previous)
            notifyItemChanged(selectedIndex)
            onSelected(item, selectedIndex)
        }
    }

    override fun getItemCount() = items.size

    fun getSelected(): SelectableItem? = items.getOrNull(selectedIndex)
}