package com.example.smartnest.util

import android.util.Log
import com.example.smartnest.model.UsageRecord
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ServerValue

object UsageTracker {
    private const val TAG = "UsageTracker"

    /**
     * Call this when turning a device ON.
     * Sets status to ON and records the start timestamp.
     */
    fun turnOn(
        deviceRef: DatabaseReference,
        deviceId: String,
        deviceName: String,
        deviceType: String,
        userId: String
    ) {
        val updates = mapOf(
            "status" to "ON",
            "last_on_timestamp" to ServerValue.TIMESTAMP
        )
        deviceRef.updateChildren(updates).addOnSuccessListener {
            // Also add to active sessions for easy reporting
            val activeRef = deviceRef.root.child("users").child(userId).child("active_sessions").child(deviceId)
            activeRef.setValue(mapOf(
                "startedAt" to ServerValue.TIMESTAMP,
                "deviceName" to deviceName,
                "deviceType" to deviceType
            ))
        }
    }

    /**
     * Call this when turning a device OFF.
     * Records usage duration and sets status to OFF.
     */
    fun turnOff(
        deviceRef: DatabaseReference,
        deviceId: String,
        deviceName: String,
        deviceType: String,
        userId: String,
        reason: String = "MANUAL_OFF"
    ) {
        // First, fetch the start timestamp to calculate duration
        deviceRef.child("last_on_timestamp").get().addOnSuccessListener { snapshot ->
            val startedAt = snapshot.getValue(Long::class.java)
            if (startedAt != null) {
                val endedAt = System.currentTimeMillis()
                val durationSeconds = (endedAt - startedAt) / 1000

                // Only record if duration is significant (e.g., > 0)
                if (durationSeconds >= 0) {
                    val usageRecord = UsageRecord(
                        deviceId = deviceId,
                        deviceName = deviceName,
                        deviceType = deviceType,
                        startedAt = startedAt,
                        endedAt = endedAt,
                        durationSeconds = durationSeconds,
                        reason = reason
                    )

                    // Push usage record to user's reports node
                    val usageRef = deviceRef.root.child("users").child(userId).child("usage_reports").push()
                    usageRef.setValue(usageRecord).addOnSuccessListener {
                        Log.d(TAG, "Usage record stored: $durationSeconds seconds")
                    }
                }
            }
            
            // Always set status to OFF and remove last_on_timestamp
            val updates = mapOf<String, Any?>(
                "status" to "OFF",
                "last_on_timestamp" to null
            )
            deviceRef.updateChildren(updates)
            
            // Remove from active sessions
            deviceRef.root.child("users").child(userId).child("active_sessions").child(deviceId).removeValue()
            
        }.addOnFailureListener {
            Log.e(TAG, "Failed to get last_on_timestamp", it)
            // Fallback: just turn off
            deviceRef.child("status").setValue("OFF")
            deviceRef.child("last_on_timestamp").removeValue()
        }
    }

    /**
     * Passive check for devices that were turned OFF by other sources (e.g. safety cutoff).
     * If status is OFF but last_on_timestamp still exists, record the usage.
     */
    fun checkAndRecordZombieUsage(
        deviceRef: DatabaseReference,
        status: String,
        lastOnTimestamp: Long?,
        deviceId: String,
        deviceName: String,
        deviceType: String,
        userId: String,
        reason: String = "SAFETY_CUTOFF" // Default to safety cutoff if it turned off without app intervention
    ) {
        if (status == "OFF" && lastOnTimestamp != null) {
            val endedAt = System.currentTimeMillis()
            val durationSeconds = (endedAt - lastOnTimestamp) / 1000
            
            if (durationSeconds >= 0) {
                val usageRecord = UsageRecord(
                    deviceId = deviceId,
                    deviceName = deviceName,
                    deviceType = deviceType,
                    startedAt = lastOnTimestamp,
                    endedAt = endedAt,
                    durationSeconds = durationSeconds,
                    reason = reason
                )
                
                val usageRef = deviceRef.root.child("users").child(userId).child("usage_reports").push()
                usageRef.setValue(usageRecord).addOnSuccessListener {
                    deviceRef.child("last_on_timestamp").removeValue()
                    deviceRef.root.child("users").child(userId).child("active_sessions").child(deviceId).removeValue()
                    Log.d(TAG, "Zombie usage record stored: $durationSeconds seconds")
                }
            } else {
                deviceRef.child("last_on_timestamp").removeValue()
            }
        }
    }
}
