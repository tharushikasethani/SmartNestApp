package com.example.smartnest.adapter

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.smartnest.DeviceImageMapper
import com.example.smartnest.R
import com.example.smartnest.model.DeviceStatus
import com.example.smartnest.model.DeviceStatusItem

class DeviceStatusAdapter(
    private val items: List<DeviceStatusItem>,
    private val onClick: (DeviceStatusItem) -> Unit
) : RecyclerView.Adapter<DeviceStatusAdapter.DeviceViewHolder>() {

    var onToggleClick: ((DeviceStatusItem, Int) -> Unit)? = null

    constructor(
        items: List<DeviceStatusItem>,
        onClick: (DeviceStatusItem) -> Unit,
        onToggleClick: ((DeviceStatusItem, Int) -> Unit)?
    ) : this(items, onClick) {
        this.onToggleClick = onToggleClick
    }

    inner class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.ivDeviceIcon)
        val name: TextView = view.findViewById(R.id.tvDeviceName)
        val subtitle: TextView? = view.findViewById(R.id.tvDeviceSubtitle)
        val status: TextView? = view.findViewById(R.id.tvDeviceStatus)
        val dot: View? = view.findViewById(R.id.dotStatus)
        val togglePill: FrameLayout? = view.findViewById(R.id.togglePill)
        val toggleDot: ImageView? = view.findViewById(R.id.toggleDot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device_status, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val item = items[position]
        val isOn = item.status == DeviceStatus.ON

        // Always resolve the image based on status to ensure ON/OFF images work
        val imageRes = DeviceImageMapper.resolve(item.deviceType, isOn)
        
        holder.icon.setImageResource(imageRes)
        holder.icon.colorFilter = null

        holder.name.text = item.name

        if (holder.subtitle != null) {
            holder.subtitle.text = if (item.subtitle.isNotBlank()) item.subtitle else "Main Room"
        }

        if (holder.status != null) {
            holder.status.text = item.status.text
            holder.status.setTextColor(ContextCompat.getColor(holder.itemView.context, item.status.textColorRes))
        }

        if (holder.dot != null) {
            holder.dot.setBackgroundResource(item.status.dotRes)
        }

        if (holder.togglePill != null && holder.toggleDot != null) {
            holder.togglePill.setBackgroundResource(
                if (isOn) R.drawable.bg_toggle_pill else R.drawable.bg_toggle_pill_off
            )

            val params = holder.toggleDot.layoutParams as FrameLayout.LayoutParams
            params.gravity = if (isOn)
                Gravity.END or Gravity.CENTER_VERTICAL
            else
                Gravity.START or Gravity.CENTER_VERTICAL
            holder.toggleDot.layoutParams = params

            holder.togglePill.setOnClickListener {
                onToggleClick?.invoke(item, holder.adapterPosition)
            }
        }

        holder.itemView.setOnClickListener { onClick(item) }

        val isFan = item.deviceType == "fan" || item.deviceType == "ceiling_fan"
        if (isFan && isOn) {
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
