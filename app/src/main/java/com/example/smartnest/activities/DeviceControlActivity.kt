package com.example.smartnest.activities

import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    
    private var activeStartTime: String = ""
    private var activeEndTime: String = ""
    private var activeDays: String = ""
    private var tvScheduleCountdown: TextView? = null

    private val countdownHandler = Handler(Looper.getMainLooper())
    private val countdownRunnable = object : Runnable {
        override fun run() {
            updateCountdownUI()
            countdownHandler.postDelayed(this, 1000)
        }
    }

    private fun updateCountdownUI() {
        if (activeStartTime.isEmpty() || activeEndTime.isEmpty()) {
            tvScheduleCountdown?.visibility = android.view.View.GONE
            return
        }

        val remaining = ScheduleValidator.getRemainingSeconds(activeStartTime, activeEndTime, activeDays)
        if (remaining > 0) {
            val h = remaining / 3600
            val m = (remaining % 3600) / 60
            val s = remaining % 60
            tvScheduleCountdown?.visibility = android.view.View.VISIBLE
            tvScheduleCountdown?.text = String.format("Ends in: %02d:%02d:%02d", h, m, s)
        } else {
            tvScheduleCountdown?.visibility = android.view.View.GONE
            
            // AUTOMATION: If time is up and device is still ON, turn it OFF
            if (currentStatus == DeviceStatus.ON) {
                val shouldBeOn = ScheduleValidator.isDeviceShouldBeOn(activeStartTime, activeEndTime, activeDays)
                if (!shouldBeOn) {
                    UsageTracker.turnOff(getDeviceRef()!!, deviceId!!, deviceName, deviceType, auth.currentUser!!.uid)
                }
            }
        }
    }

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
        tvScheduleCountdown = findViewById<TextView>(R.id.tvScheduleCountdown)

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

            // Update the icon image based on the new ON/OFF state
            val isOn = status == DeviceStatus.ON
            ivIcon?.setImageResource(DeviceImageMapper.resolve(deviceType, isOn))

            if (isOn && isFan) {
                ivIcon?.startAnimation(fanRotateAnim)
            } else if (isOn && isLight) {
                ivIcon?.startAnimation(blinkAnim)
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
                        tvBrightness?.text = String.format("%d%%", brightness)
                    }

                    // Handle Schedule Display
                    val schedule = snapshot.child("schedule")
                    if (schedule.exists() && (schedule.child("enabled").getValue(Boolean::class.java) ?: false)) {
                        val start = schedule.child("startTime").getValue(String::class.java) ?: ""
                        val end = schedule.child("endTime").getValue(String::class.java) ?: ""
                        val daysRaw = schedule.child("days").getValue(String::class.java) ?: ""
                        
                        activeStartTime = start
                        activeEndTime = end
                        activeDays = daysRaw

                        if (start.isNotEmpty() && end.isNotEmpty()) {
                            cardSchedule?.visibility = android.view.View.VISIBLE
                            tvScheduleTime?.text = String.format("%s - %s", start, end)
                            
                            val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
                            val daysText = daysRaw.split(",")
                                .filter { it.isNotEmpty() }
                                .mapNotNull { it.trim().toIntOrNull() }
                                .sorted()
                                .joinToString(", ") { dayLabels[it] }
                            
                            tvScheduleDays?.text = if (daysText.isNotEmpty()) daysText else "Once"

                            // AUTOMATION: Check if device should be forced ON
                            val shouldBeOn = ScheduleValidator.isDeviceShouldBeOn(start, end, daysRaw)
                            if (shouldBeOn && currentStatus == DeviceStatus.OFF) {
                                UsageTracker.turnOn(deviceRef!!, deviceId!!, deviceName, deviceType, auth.currentUser!!.uid)
                            }
                        } else {
                            cardSchedule?.visibility = android.view.View.GONE
                            activeStartTime = ""
                        }
                    } else {
                        cardSchedule?.visibility = android.view.View.GONE
                        activeStartTime = ""
                    }
                    updateCountdownUI()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }

        updateUIState(currentStatus)

        seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvBrightness?.text = String.format("%d%%", progress)
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

    override fun onResume() {
        super.onResume()
        countdownHandler.post(countdownRunnable)
    }

    override fun onPause() {
        super.onPause()
        countdownHandler.removeCallbacks(countdownRunnable)
    }
}
