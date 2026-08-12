package com.example.smartnest.activities

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import com.example.smartnest.R
import com.example.smartnest.util.UsageTracker
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class MultiSwitchControlActivity : BaseDeviceControlActivity() {

    private var switchCount = 2
    private val switchStates = mutableMapOf<String, Boolean>()
    private val switchLabels = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_multi_switch_control)
        setupCommonHeader("Switch Control")

        loadAndBuildSwitches()
        observeOverallStatus()

        findViewById<android.view.View>(R.id.btnUsageReport)?.setOnClickListener {
            openReport()
        }
    }

    private fun observeOverallStatus() {
        getDeviceRef()?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val dbStatus = snapshot.child("status").getValue(String::class.java) ?: "OFF"
                val lastOnTimestamp = snapshot.child("last_on_timestamp").getValue(Long::class.java)

                UsageTracker.checkAndRecordZombieUsage(
                    getDeviceRef()!!, dbStatus, lastOnTimestamp,
                    deviceId!!, deviceName, deviceType, auth.currentUser!!.uid
                )
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadAndBuildSwitches() {
        val ref = getDeviceRef() ?: return
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                switchCount = snapshot.child("switchCount").getValue(Int::class.java) ?: 2
                val switchesSnap = snapshot.child("switches")
                switchStates.clear()
                switchLabels.clear()
                for (i in 1..switchCount) {
                    val key = "switch$i"
                    val s = switchesSnap.child(key)
                    switchLabels[key] = s.child("label").getValue(String::class.java) ?: "Switch $i"
                    switchStates[key] = s.child("isOn").getValue(Boolean::class.java) ?: false
                }
                buildSwitchRows()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun buildSwitchRows() {
        val container = findViewById<LinearLayout>(R.id.containerSwitches)
        container.removeAllViews()

        for (i in 1..switchCount) {
            val key = "switch$i"
            val label = switchLabels[key] ?: "Switch $i"
            val isOn = switchStates[key] ?: false

            val row = layoutInflater.inflate(R.layout.item_switch_row, container, false)
            row.findViewById<TextView>(R.id.tvSwitchLabel).text = label
            val tvStatus = row.findViewById<TextView>(R.id.tvStatusValue)
            val toggle = row.findViewById<SwitchCompat>(R.id.switchToggle)
            
            fun updateRowUI(checked: Boolean) {
                tvStatus.text = if (checked) "ON" else "OFF"
                tvStatus.setTextColor(if (checked) 0xFF34C759.toInt() else 0xFF8E8E93.toInt())
            }

            updateRowUI(isOn)
            toggle.isChecked = isOn
            toggle.setOnCheckedChangeListener { _, checked ->
                updateRowUI(checked)
                val ref = getDeviceRef() ?: return@setOnCheckedChangeListener
                ref.child("switches").child(key).child("isOn").setValue(checked)
                
                var anyOn = checked
                if (!anyOn) {
                    for (i2 in 0 until container.childCount) {
                        val child = container.getChildAt(i2)
                        val t = child?.findViewById<SwitchCompat>(R.id.switchToggle)
                        if (t != null && t.isChecked) { anyOn = true; break }
                    }
                }
                
                if (anyOn) {
                    ref.child("status").get().addOnSuccessListener {
                        if (it.getValue(String::class.java) != "ON") {
                            UsageTracker.turnOn(ref, deviceId!!, deviceName, deviceType, auth.currentUser!!.uid)
                        }
                    }
                } else {
                    UsageTracker.turnOff(ref, deviceId!!, deviceName, deviceType, auth.currentUser!!.uid)
                }
            }

            val divider = android.view.View(this)
            divider.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            )
            divider.setBackgroundColor(0xFFEFEFEF.toInt())
            if (i < switchCount) container.addView(divider)

            container.addView(row)
        }

        findViewById<TextView>(R.id.tvSwitchCount).text = "$switchCount-Gang Switch"
    }
}
