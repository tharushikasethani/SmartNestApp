package com.example.smartnest.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smartnest.R
import com.example.smartnest.model.DeviceStatusItem

class DeviceStatusAdapter(
    private val items: List<DeviceStatusItem>,
    private val onClick: (DeviceStatusItem) -> Unit
) : RecyclerView.Adapter<DeviceStatusAdapter.DeviceViewHolder>() {

    inner class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.ivDeviceIcon)
        val name: TextView = view.findViewById(R.id.tvDeviceName)
        val status: TextView = view.findViewById(R.id.tvDeviceStatus)
        val dot: View = view.findViewById(R.id.dotStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device_status, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val item = items[position]
        holder.icon.setImageResource(item.iconRes)
        holder.name.text = item.name
        holder.status.text = item.statusText
        holder.status.setTextColor(
            holder.itemView.context.getColor(
                if (item.isActive) R.color.status_on_green else R.color.status_off_gray
            )
        )
        holder.dot.setBackgroundResource(
            if (item.isActive) R.drawable.bg_dot_green else R.drawable.bg_dot_gray
        )
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size
}