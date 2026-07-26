package com.example.smartnest.adapter

import android.view.LayoutInflater
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smartnest.R
import com.example.smartnest.model.Device

class DeviceAdapter(
    private var devices: List<Device>,
    private val onDeviceClick: (Device) -> Unit,
    private val onToggleClick: (Device, Int) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    inner class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivDeviceIcon)
        val tvName: TextView = view.findViewById(R.id.tvDeviceName)
        val tvSubtitle: TextView = view.findViewById(R.id.tvDeviceSubtitle)
        val tvStatus: TextView = view.findViewById(R.id.tvDeviceStatus)
        val togglePill: FrameLayout = view.findViewById(R.id.togglePill)
        val toggleDot: ImageView = view.findViewById(R.id.toggleDot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]

        holder.ivIcon.setImageResource(device.iconRes)
        holder.tvName.text = device.name
        holder.tvSubtitle.text = device.subtitle
        holder.tvStatus.text = device.statusLabel

        holder.togglePill.setBackgroundResource(
            if (device.isOn) R.drawable.bg_toggle_pill else R.drawable.bg_toggle_pill_off
        )
        holder.tvStatus.setBackgroundResource(
            if (device.isOn) R.drawable.bg_status_pill_on else R.drawable.bg_status_pill_off
        )

        val params = holder.toggleDot.layoutParams as FrameLayout.LayoutParams
        params.gravity = if (device.isOn)
            Gravity.END or Gravity.CENTER_VERTICAL
        else
            Gravity.START or Gravity.CENTER_VERTICAL
        holder.toggleDot.layoutParams = params

        holder.itemView.setOnClickListener { onDeviceClick(device) }
        holder.togglePill.setOnClickListener { onToggleClick(device, holder.adapterPosition) }
    }

    override fun getItemCount() = devices.size

    fun updateList(newList: List<Device>) {
        devices = newList
        notifyDataSetChanged()
    }

    fun toggleAt(position: Int) {
        if (position in devices.indices) {
            devices[position].isOn = !devices[position].isOn
            notifyItemChanged(position)
        }
    }
}