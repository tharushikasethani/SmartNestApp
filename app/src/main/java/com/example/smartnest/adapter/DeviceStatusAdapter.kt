package com.example.smartnest.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.smartnest.R
import com.example.smartnest.model.DeviceStatus
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
        holder.status.text = item.status.text
        holder.status.setTextColor(
            ContextCompat.getColor(holder.itemView.context, item.status.textColorRes)
        )
        holder.dot.setBackgroundResource(item.status.dotRes)
        holder.itemView.setOnClickListener { onClick(item) }

        val isFan = item.deviceType == "fan" || item.deviceType == "ceiling_fan"
        if (isFan && item.status == DeviceStatus.ON) {
            holder.icon.startAnimation(
                AnimationUtils.loadAnimation(holder.itemView.context, R.anim.fan_rotate)
            )
        } else {
            holder.icon.clearAnimation()
            holder.icon.rotation = 0f
        }
    }

    override fun getItemCount() = items.size
}
