package com.example.smartnest.activities

import android.os.Bundle
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.smartnest.R
import com.example.smartnest.model.DeviceStatus
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class OutletControlActivity : BaseDeviceControlActivity() {

    private var currentStatus: DeviceStatus = DeviceStatus.OFF

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_outlet_control)
        setupCommonHeader("Outlet Control")

        val tvStatus = findViewById<TextView>(R.id.tvStatusValue)

        fun updateUIState(status: DeviceStatus) {
            currentStatus = status
            tvStatus?.text = status.text
            tvStatus?.setTextColor(ContextCompat.getColor(this, status.textColorRes))
        }

        findViewById<TextView>(R.id.btnOn).setOnClickListener {
            getDeviceRef()?.let { ref ->
                com.example.smartnest.util.UsageTracker.turnOn(ref, deviceId!!, deviceName, deviceType, auth.currentUser!!.uid)
            }
        }
        findViewById<TextView>(R.id.btnOff).setOnClickListener {
            getDeviceRef()?.let { ref ->
                com.example.smartnest.util.UsageTracker.turnOff(ref, deviceId!!, deviceName, deviceType, auth.currentUser!!.uid)
            }
        }
        findViewById<TextView>(R.id.btnSchedule).setOnClickListener { openSchedule() }

        getDeviceRef()?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val statusStr = snapshot.child("status").getValue(String::class.java) ?: "OFF"
                val status = try { DeviceStatus.valueOf(statusStr) } catch (_: Exception) { DeviceStatus.OFF }
                updateUIState(status)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
