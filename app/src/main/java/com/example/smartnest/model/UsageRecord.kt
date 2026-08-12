package com.example.smartnest.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class UsageRecord(
    val deviceId: String = "",
    val deviceName: String = "",
    val deviceType: String = "",
    val startedAt: Long = 0,
    val endedAt: Long = 0,
    val durationSeconds: Long = 0,
    val reason: String = ""
)
