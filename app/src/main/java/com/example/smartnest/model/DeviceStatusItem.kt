package com.example.smartnest.model

import com.example.smartnest.R

enum class DeviceStatus(val text: String) {
    ON("ON"),
    OFF("OFF"),
    ERROR("ERROR"),
    DISCONNECTED("DISCONNECTED");

    val dotRes: Int
        get() = when (this) {
            ON -> R.drawable.bg_dot_green
            OFF -> R.drawable.bg_dot_gray
            ERROR -> R.drawable.bg_dot_red
            DISCONNECTED -> R.drawable.bg_dot_orange
        }

    val textColorRes: Int
        get() = when (this) {
            ON -> R.color.status_on_green
            OFF -> R.color.status_off_gray
            ERROR -> android.R.color.holo_red_dark
            DISCONNECTED -> R.color.orange_primary
        }
}

data class DeviceStatusItem(
    val id: String,
    val name: String,
    val iconRes: Int,
    val status: DeviceStatus = DeviceStatus.OFF,
    val deviceType: String = "light",
    val subtitle: String = ""
) {
    val statusText: String get() = status.text
    val isActive: Boolean get() = status == DeviceStatus.ON
}
