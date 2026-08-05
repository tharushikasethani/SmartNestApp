package com.example.smartnest.activities

import android.os.Bundle
import android.widget.TextView
import com.example.smartnest.R

class OutletControlActivity : BaseDeviceControlActivity() {

    private var isOn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_outlet_control)
        setupCommonHeader("Outlet Control")

        val tvStatus = findViewById<TextView>(R.id.tvStatusValue)

        fun updateState(on: Boolean) {
            isOn = on
            tvStatus.text = if (on) "ON" else "OFF"
            tvStatus.setTextColor(if (on) 0xFF34C759.toInt() else 0xFFFF3B30.toInt())
            getDeviceRef()?.child("status")?.setValue(if (on) "ON" else "OFF")
        }

        findViewById<TextView>(R.id.btnOn).setOnClickListener { updateState(true) }
        findViewById<TextView>(R.id.btnOff).setOnClickListener { updateState(false) }
        findViewById<TextView>(R.id.btnSchedule).setOnClickListener { openSchedule() }

        val initial = intent.getStringExtra("device_status") ?: "OFF"
        updateState(initial == "ON")
        getDeviceRef()?.child("status")?.get()?.addOnSuccessListener {
            updateState(it.getValue(String::class.java) == "ON")
        }
    }
}
