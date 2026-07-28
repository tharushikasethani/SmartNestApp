package com.example.smartnest.model

data class DeviceStatusItem(
    val id: String,
    val name: String,
    val iconRes: Int,
    val statusText: String,
    val isActive: Boolean
)