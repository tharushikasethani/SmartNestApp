package com.example.smartnest.utils

import java.text.SimpleDateFormat
import java.util.*

object ScheduleValidator {

    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)

    fun isDeviceShouldBeOn(startTimeStr: String, endTimeStr: String, daysStr: String): Boolean {
        if (startTimeStr.isEmpty() || endTimeStr.isEmpty()) return false
        
        try {
            val now = Calendar.getInstance()
            
            // 1. Check if today is a scheduled day
            val currentDayIndex = when (now.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> 0
                Calendar.TUESDAY -> 1
                Calendar.WEDNESDAY -> 2
                Calendar.THURSDAY -> 3
                Calendar.FRIDAY -> 4
                Calendar.SATURDAY -> 5
                Calendar.SUNDAY -> 6
                else -> -1
            }
            
            val scheduledDays = daysStr.split(",")
                .filter { it.isNotEmpty() }
                .mapNotNull { it.trim().toIntOrNull() }
                
            if (currentDayIndex !in scheduledDays) return false

            // 2. Parse and normalize times
            val startD = timeFormat.parse(startTimeStr) ?: return false
            val endD = timeFormat.parse(endTimeStr) ?: return false

            val calNow = Calendar.getInstance()
            val hourNow = calNow.get(Calendar.HOUR_OF_DAY)
            val minNow = calNow.get(Calendar.MINUTE)
            val currentMinutes = hourNow * 60 + minNow

            val calS = Calendar.getInstance().apply { time = startD }
            val startMinutes = calS.get(Calendar.HOUR_OF_DAY) * 60 + calS.get(Calendar.MINUTE)

            val calE = Calendar.getInstance().apply { time = endD }
            val endMinutes = calE.get(Calendar.HOUR_OF_DAY) * 60 + calE.get(Calendar.MINUTE)

            // Handle overnight schedules
            return if (endMinutes < startMinutes) {
                // If it's 10 PM to 2 AM
                currentMinutes >= startMinutes || currentMinutes < endMinutes
            } else {
                // Standard day schedule
                currentMinutes in startMinutes until endMinutes
            }
            
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * Returns remaining seconds until the current schedule ends.
     * Returns -1 if no schedule is active right now.
     */
    fun getRemainingSeconds(startTimeStr: String, endTimeStr: String, daysStr: String): Long {
        if (!isDeviceShouldBeOn(startTimeStr, endTimeStr, daysStr)) return -1

        try {
            val endD = timeFormat.parse(endTimeStr) ?: return -1
            val now = Calendar.getInstance()
            
            val calE = Calendar.getInstance().apply {
                time = endD
                set(Calendar.YEAR, now.get(Calendar.YEAR))
                set(Calendar.DAY_OF_YEAR, now.get(Calendar.DAY_OF_YEAR))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // Handle overnight crossing
            if (calE.timeInMillis <= now.timeInMillis) {
                calE.add(Calendar.DAY_OF_YEAR, 1)
            }

            return (calE.timeInMillis - now.timeInMillis) / 1000
        } catch (e: Exception) {
            return -1
        }
    }
}
