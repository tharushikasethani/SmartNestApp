package com.example.smartnest.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smartnest.R

class CategoryAdapter(
    private val categories: List<String>,
    private var selectedIndex: Int = 0,
    private val onSelected: (String, Int) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.ChipViewHolder>() {

    class ChipViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvChip: TextView = view.findViewById(R.id.tvChip)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_chip, parent, false)
        return ChipViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChipViewHolder, position: Int) {
        val label = categories[position]
        holder.tvChip.text = label

        val isSelected = position == selectedIndex
        holder.tvChip.setBackgroundResource(
            if (isSelected) R.drawable.bg_pill_selected else R.drawable.bg_pill_unselected
        )
        holder.tvChip.setTextColor(
            holder.itemView.context.getColor(
                if (isSelected) android.R.color.white else R.color.text_primary_fallback
            )
        )

        holder.tvChip.setOnClickListener {
            val position = holder.bindingAdapterPosition
            if (position == RecyclerView.NO_POSITION) return@setOnClickListener

            val previous = selectedIndex
            selectedIndex = position
            notifyItemChanged(previous)
            notifyItemChanged(selectedIndex)
            onSelected(label, selectedIndex)
        }
    }

    override fun getItemCount() = categories.size
}