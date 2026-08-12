package com.example.smartnest.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Alert(
    val id: String = "",
    val type: String = "",
    val deviceId: String = "",
    val deviceName: String = "",
    val message: String = "",
    val timestamp: Long = 0,
    var read: Boolean = false
)
