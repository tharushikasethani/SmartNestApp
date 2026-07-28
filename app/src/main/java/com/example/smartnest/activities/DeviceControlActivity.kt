package com.example.smartnest.activities

import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.smartnest.R

class DeviceControlActivity : AppCompatActivity() {

    private var isOn = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_control)

        val deviceName = intent.getStringExtra("device_name") ?: "Living Room Light"
        val deviceRoom = intent.getStringExtra("device_room") ?: "Living Room"
        val deviceId = intent.getStringExtra("device_id") ?: ""

        findViewById<TextView>(R.id.tvDeviceName).text = deviceName
        findViewById<TextView>(R.id.tvDeviceRoom).text = deviceRoom

        val tvStatus = findViewById<TextView>(R.id.tvStatusValue)
        val tvBrightness = findViewById<TextView>(R.id.tvBrightnessValue)
        val seekBar = findViewById<SeekBar>(R.id.seekBrightness)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvBrightness.text = "$progress%"
            }
            override fun onStartTrackingTouch(bar: SeekBar?) {}
            override fun onStopTrackingTouch(bar: SeekBar?) {}
        })

        fun updateState(on: Boolean) {
            isOn = on
            tvStatus.text = if (on) "ON" else "OFF"
            tvStatus.setTextColor(
                if (on) 0xFF34C759.toInt() else 0xFFFF3B30.toInt()
            )
        }

        findViewById<TextView>(R.id.btnOn).setOnClickListener { updateState(true) }
        findViewById<TextView>(R.id.btnOff).setOnClickListener { updateState(false) }

        findViewById<FrameLayout>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<TextView>(R.id.btnSchedule).setOnClickListener {
            val intent = Intent(this, ScheduleActivity::class.java)
            intent.putExtra("device_name", deviceName)
            intent.putExtra("device_id", deviceId)
            startActivity(intent)
        }

        findViewById<TextView>(R.id.btnUsageReport).setOnClickListener {
            // TODO: startActivity(Intent(this, UsageReportActivity::class.java))
        }
    }
}