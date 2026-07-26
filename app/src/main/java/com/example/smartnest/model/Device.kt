package com.example.smartnest.model

data class Device(
    val name: String,
    val subtitle: String,
    val statusLabel: String,
    val iconRes: Int,
    var isOn: Boolean
)
