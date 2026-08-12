package com.example.smartnest.activities

import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.smartnest.DeviceImageMapper
import com.example.smartnest.R
import com.example.smartnest.model.DeviceStatus
import com.example.smartnest.utils.ScheduleValidator
import com.example.smartnest.util.UsageTracker
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class DeviceControlActivity : BaseDeviceControlActivity() {

    private var currentStatus: DeviceStatus = DeviceStatus.OFF

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_control)
        setupCommonHeader("Device Control")

        val initialStatusStr = intent.getStringExtra("device_status") ?: "OFF"
        currentStatus = try { DeviceStatus.valueOf(initialStatusStr) } catch (_: Exception) { DeviceStatus.OFF }

        val ivIcon = findViewById<ImageView>(R.id.ivDeviceIcon)
        ivIcon?.setImageResource(DeviceImageMapper.resolve(deviceType))
        ivIcon?.imageTintList = null // Clear tint for realistic images
        
        val blinkAnim = AnimationUtils.loadAnimation(this, R.anim.blink)
        val fanRotateAnim = AnimationUtils.loadAnimation(this, R.anim.fan_rotate)

        val tvStatus = findViewById<TextView>(R.id.tvStatusValue)
        val tvBrightness = findViewById<TextView>(R.id.tvBrightnessValue)
        val seekBar = findViewById<SeekBar>(R.id.seekBrightness)
        val rowBrightness = findViewById<android.widget.LinearLayout>(R.id.rowBrightness)
        val dividerBrightness = findViewById<android.view.View>(R.id.dividerBrightness)
        
        val cardSchedule = findViewById<android.view.View>(R.id.cardCurrentSchedule)
        val tvScheduleTime = findViewById<TextView>(R.id.tvActiveScheduleTime)
        val tvScheduleDays = findViewById<TextView>(R.id.tvActiveScheduleDays)

        val isLight = deviceType.lowercase().contains("light") || deviceType.lowercase().contains("lamp")
        val isFan = deviceType.lowercase().contains("fan")
        
        if (rowBrightness != null && !isLight) {
            rowBrightness.visibility = android.view.View.GONE
        }
        if (dividerBrightness != null && !isLight) {
            dividerBrightness.visibility = android.view.View.GONE
        }

        val deviceRef = getDeviceRef()

        fun updateUIState(status: DeviceStatus) {
            currentStatus = status
            tvStatus?.text = status.text
            tvStatus?.setTextColor(ContextCompat.getColor(this, status.textColorRes))

            // Handle visual feedback/animations based on status
            val isOn = status == DeviceStatus.ON

            // ADD THIS LINE: Update the icon image based on the new ON/OFF state
            ivIcon?.setImageResource(DeviceImageMapper.resolve(deviceType, isOn))

            if (isOn && isFan) {
                ivIcon?.startAnimation(fanRotateAnim)
            } else {
                ivIcon?.clearAnimation()
                ivIcon?.alpha = 1.0f
                ivIcon?.rotation = 0f
            }
        }

        fun loadState() {
            deviceRef?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val statusStr = snapshot.child("status").getValue(String::class.java) ?: "OFF"
                    val status = try { DeviceStatus.valueOf(statusStr) } catch (_: Exception) { DeviceStatus.OFF }
                    updateUIState(status)
                    
                    if (isLight) {
                        val brightness = snapshot.child("brightness").getValue(Int::class.java) ?: 75
                        seekBar?.progress = brightness
                        tvBrightness?.text = "$brightness%"
                    }

                    // Handle Schedule Display
                    val schedule = snapshot.child("schedule")
                    if (schedule.exists() && (schedule.child("enabled").getValue(Boolean::class.java) ?: false)) {
                        val start = schedule.child("startTime").getValue(String::class.java) ?: ""
                        val end = schedule.child("endTime").getValue(String::class.java) ?: ""
                        val daysRaw = schedule.child("days").getValue(String::class.java) ?: ""
                        
                        if (start.isNotEmpty() && end.isNotEmpty()) {
                            cardSchedule?.visibility = android.view.View.VISIBLE
                            tvScheduleTime?.text = "$start - $end"
                            
                            val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
                            val daysText = daysRaw.split(",")
                                .filter { it.isNotEmpty() }
                                .mapNotNull { it.trim().toIntOrNull() }
                                .sorted()
                                .joinToString(", ") { dayLabels[it] }
                            
                            tvScheduleDays?.text = if (daysText.isNotEmpty()) daysText else "Once"

                            // AUTOMATION: Check if device should be forced ON/OFF
                            val shouldBeOn = ScheduleValidator.isDeviceShouldBeOn(start, end, daysRaw)
                            
                            if (shouldBeOn && currentStatus == DeviceStatus.OFF) {
                                deviceRef.child("status").setValue("ON")
                            }
                        } else {
                            cardSchedule?.visibility = android.view.View.GONE
                        }
                    } else {
                        cardSchedule?.visibility = android.view.View.GONE
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }

        updateUIState(currentStatus)

        seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvBrightness?.text = "$progress%"
            }
            override fun onStartTrackingTouch(bar: SeekBar?) {}
            override fun onStopTrackingTouch(bar: SeekBar?) {
                deviceRef?.child("brightness")?.setValue(seekBar?.progress)
            }
        })

        findViewById<android.view.View>(R.id.btnOn)?.setOnClickListener {
            UsageTracker.turnOn(deviceRef!!, deviceId!!, deviceName, deviceType, auth.currentUser!!.uid)
        }
        findViewById<android.view.View>(R.id.btnOff)?.setOnClickListener {
            UsageTracker.turnOff(deviceRef!!, deviceId!!, deviceName, deviceType, auth.currentUser!!.uid)
        }

        findViewById<android.view.View>(R.id.btnSchedule)?.setOnClickListener {
            openSchedule()
        }

        findViewById<android.view.View>(R.id.btnUsageReport)?.setOnClickListener {
            openReport()
        }

        loadState()
    }
}
