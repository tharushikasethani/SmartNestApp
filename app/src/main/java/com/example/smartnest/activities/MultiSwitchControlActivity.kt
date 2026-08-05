package com.example.smartnest.activities

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import com.example.smartnest.R
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
            val toggle = row.findViewById<SwitchCompat>(R.id.switchToggle)
            toggle.isChecked = isOn
            toggle.setOnCheckedChangeListener { _, checked ->
                val ref = getDeviceRef()
                ref?.child("switches")?.child(key)?.child("isOn")?.setValue(checked)
                var anyOn = checked
                if (!anyOn) {
                    for (i2 in 0 until container.childCount) {
                        val child = container.getChildAt(i2)
                        val t = child?.findViewById<SwitchCompat>(R.id.switchToggle)
                        if (t != null && t.isChecked) { anyOn = true; break }
                    }
                }
                ref?.child("status")?.setValue(if (anyOn) "ON" else "OFF")
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
