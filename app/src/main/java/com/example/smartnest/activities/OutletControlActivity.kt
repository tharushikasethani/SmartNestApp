package com.example.smartnest.activities

import android.os.Bundle
import android.widget.TextView
import com.example.smartnest.R

import com.example.smartnest.util.UsageTracker
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class OutletControlActivity : BaseDeviceControlActivity() {

    private var isOn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_outlet_control)
        setupCommonHeader("Outlet Control")

        val tvStatus = findViewById<TextView>(R.id.tvStatusValue)

        fun updateUI(on: Boolean) {
            isOn = on
            tvStatus.text = if (on) "ON" else "OFF"
            tvStatus.setTextColor(if (on) 0xFF34C759.toInt() else 0xFFFF3B30.toInt())
        }

        findViewById<TextView>(R.id.btnOn).setOnClickListener {
            UsageTracker.turnOn(getDeviceRef()!!, deviceId!!, deviceName, deviceType, auth.currentUser!!.uid)
        }
        findViewById<TextView>(R.id.btnOff).setOnClickListener {
            UsageTracker.turnOff(getDeviceRef()!!, deviceId!!, deviceName, deviceType, auth.currentUser!!.uid)
        }
        findViewById<TextView>(R.id.btnSchedule).setOnClickListener { openSchedule() }
        findViewById<TextView>(R.id.btnUsageReport).setOnClickListener { openReport() }

        getDeviceRef()?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val dbStatus = snapshot.child("status").getValue(String::class.java) ?: "OFF"
                val lastOnTimestamp = snapshot.child("last_on_timestamp").getValue(Long::class.java)

                UsageTracker.checkAndRecordZombieUsage(
                    getDeviceRef()!!, dbStatus, lastOnTimestamp,
                    deviceId!!, deviceName, deviceType, auth.currentUser!!.uid
                )

                updateUI(dbStatus == "ON")
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
