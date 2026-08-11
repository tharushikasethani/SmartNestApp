package com.example.smartnest.activities

import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import com.example.smartnest.DeviceImageMapper
import com.example.smartnest.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class DeviceControlActivity : BaseDeviceControlActivity() {

    private var isOn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_control)
        setupCommonHeader("Device Control")

        val initialStatus = intent.getStringExtra("device_status") ?: "OFF"
        isOn = initialStatus == "ON"

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

        val isLight = deviceType.lowercase().contains("light") || deviceType.lowercase().contains("lamp")
        val isFan = deviceType.lowercase().contains("fan")
        
        if (rowBrightness != null && !isLight) {
            rowBrightness.visibility = android.view.View.GONE
        }
        if (dividerBrightness != null && !isLight) {
            dividerBrightness.visibility = android.view.View.GONE
        }

        val deviceRef = getDeviceRef()

        fun updateUIState(on: Boolean) {
            isOn = on
            tvStatus?.text = if (on) "ON" else "OFF"
            tvStatus?.setTextColor(
                if (on) 0xFF34C759.toInt() else 0xFFFF3B30.toInt()
            )
            if (on && isFan) {
                ivIcon?.startAnimation(fanRotateAnim)
            } else if (on && isLight) {
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
                    val currentStatus = snapshot.child("status").getValue(String::class.java) ?: "OFF"
                    updateUIState(currentStatus == "ON")
                    
                    if (isLight) {
                        val brightness = snapshot.child("brightness").getValue(Int::class.java) ?: 75
                        seekBar?.progress = brightness
                        tvBrightness?.text = "$brightness%"
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }

        updateUIState(isOn)

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
            deviceRef?.child("status")?.setValue("ON")
        }
        findViewById<android.view.View>(R.id.btnOff)?.setOnClickListener {
            deviceRef?.child("status")?.setValue("OFF")
        }

        findViewById<android.view.View>(R.id.btnSchedule)?.setOnClickListener {
            openSchedule()
        }

        loadState()
    }
}
