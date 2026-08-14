package com.example.smartnest.activities

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.graphics.Color
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.smartnest.R
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ScheduleActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private var deviceRef: DatabaseReference? = null

    private val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
    private val selectedDays = mutableSetOf<Int>()
    private val dayViews = mutableListOf<TextView>()
    
    private lateinit var tvStart: TextView
    private lateinit var tvEnd: TextView
    private lateinit var switchEnabled: com.google.android.material.switchmaterial.SwitchMaterial
    private lateinit var btnDelete: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Make activity edge-to-edge
        window.apply {
            clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = android.graphics.Color.TRANSPARENT
            navigationBarColor = android.graphics.Color.TRANSPARENT
            decorView.systemUiVisibility =
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        setContentView(R.layout.activity_schedule)

        val deviceName = intent.getStringExtra("device_name") ?: "Device"
        val deviceId = intent.getStringExtra("device_id") ?: intent.getStringExtra("deviceId")
        val homeId = intent.getStringExtra("homeId")
        val floorId = intent.getStringExtra("floorId")
        val roomId = intent.getStringExtra("roomId")
        val uid = auth.currentUser?.uid

        findViewById<TextView>(R.id.tvScheduleTitle).text = "$deviceName Schedule"
        findViewById<FrameLayout>(R.id.btnBack).setOnClickListener { finish() }

        tvStart = findViewById(R.id.tvStartTime)
        tvEnd = findViewById(R.id.tvEndTime)
        switchEnabled = findViewById(R.id.switchEnabled)
        btnDelete = findViewById(R.id.btnDeleteSchedule)

        findViewById<LinearLayout>(R.id.rowStartTime).setOnClickListener {
            showTimePicker("Select Start Time") { time -> tvStart.text = time }
        }
        findViewById<LinearLayout>(R.id.rowEndTime).setOnClickListener {
            showTimePicker("Select End Time") { time -> tvEnd.text = time }
        }

        buildDayPills()

        if (!uid.isNullOrEmpty() && !homeId.isNullOrEmpty() && !floorId.isNullOrEmpty() && !roomId.isNullOrEmpty() && !deviceId.isNullOrEmpty()) {
            deviceRef = database.getReference("users")
                .child(uid).child("homes").child(homeId)
                .child("floors").child(floorId).child("rooms")
                .child(roomId).child("devices").child(deviceId!!)
            
            loadSchedule()
        } else {
            // Default selection if no device info
            selectedDays.addAll(listOf(0, 1, 2, 3, 4))
            updateDayPillsUI()
        }

        findViewById<TextView>(R.id.btnSaveSchedule).setOnClickListener {
            saveSchedule()
        }

        btnDelete.setOnClickListener {
            deleteSchedule()
        }
    }

    private fun loadSchedule() {
        deviceRef?.child("schedule")?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    btnDelete.visibility = View.VISIBLE
                    tvStart.text = snapshot.child("startTime").getValue(String::class.java) ?: "06:00 PM"
                    tvEnd.text = snapshot.child("endTime").getValue(String::class.java) ?: "11:00 PM"
                    switchEnabled.isChecked = snapshot.child("enabled").getValue(Boolean::class.java) ?: true
                    
                    val days = snapshot.child("days").getValue(String::class.java) ?: ""
                    selectedDays.clear()
                    if (days.isNotEmpty()) {
                        days.split(",").forEach { 
                            it.trim().toIntOrNull()?.let { idx -> selectedDays.add(idx) } 
                        }
                    }
                } else {
                    btnDelete.visibility = View.GONE
                    selectedDays.addAll(listOf(0, 1, 2, 3, 4))
                }
                updateDayPillsUI()
            }
            override fun onCancelled(error: DatabaseError) {
                selectedDays.addAll(listOf(0, 1, 2, 3, 4))
                updateDayPillsUI()
            }
        })
    }

    private fun deleteSchedule() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Delete Schedule")
            .setMessage("Are you sure you want to remove the schedule for this device?")
            .setPositiveButton("Delete") { _, _ ->
                deviceRef?.child("schedule")?.removeValue()?.addOnSuccessListener {
                    Toast.makeText(this, "Schedule deleted", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(Color.parseColor("#C62828"))

            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(Color.parseColor("#757575"))
        }

        dialog.show()
    }

    private fun saveSchedule() {
        if (deviceRef == null) {
            Toast.makeText(this, "Cannot save: Device info missing", Toast.LENGTH_SHORT).show()
            return
        }

        val scheduleData = mapOf(
            "startTime" to tvStart.text.toString(),
            "endTime" to tvEnd.text.toString(),
            "enabled" to switchEnabled.isChecked,
            "days" to selectedDays.sorted().joinToString(",")
        )

        deviceRef?.child("schedule")?.setValue(scheduleData)
            ?.addOnSuccessListener {
                Toast.makeText(this, "Schedule saved", Toast.LENGTH_SHORT).show()
                finish()
            }
            ?.addOnFailureListener {
                Toast.makeText(this, "Failed to save: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun buildDayPills() {
        val container = findViewById<LinearLayout>(R.id.rowDays)
        container.removeAllViews()
        dayViews.clear()

        val density = resources.displayMetrics.density
        val sizePx = (36 * density).toInt()
        val marginPx = (6 * density).toInt()

        dayLabels.forEachIndexed { index, label ->
            val pill = TextView(this).apply {
                text = label
                textSize = 14f
                gravity = Gravity.CENTER
                background = ContextCompat.getDrawable(context, R.drawable.bg_day_circle_unselected)
            }
            val params = LinearLayout.LayoutParams(sizePx, sizePx).apply { marginEnd = marginPx }
            pill.layoutParams = params
            pill.setOnClickListener {
                if (index in selectedDays) selectedDays.remove(index) else selectedDays.add(index)
                updateDayPillsUI()
            }
            dayViews.add(pill)
            container.addView(pill)
        }
    }

    private fun updateDayPillsUI() {
        dayViews.forEachIndexed { index, textView ->
            val isSelected = index in selectedDays
            textView.setTextColor(ContextCompat.getColor(this, if (isSelected) android.R.color.white else R.color.text_primary))
            textView.setBackgroundResource(if (isSelected) R.drawable.bg_day_circle_selected else R.drawable.bg_day_circle_unselected)
        }
    }

    private fun showTimePicker(title: String, onPicked: (String) -> Unit) {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(12)
            .setMinute(0)
            .setTitleText(title)
            .setTheme(R.style.ThemeOverlay_SmartNest_TimePicker)
            .build()

        picker.addOnPositiveButtonClickListener {
            val hour = picker.hour
            val minute = picker.minute
            val amPm = if (hour < 12) "AM" else "PM"
            val hour12 = if (hour % 12 == 0) 12 else hour % 12
            onPicked(String.format("%02d:%02d %s", hour12, minute, amPm))
        }

        picker.show(supportFragmentManager, "time_picker")
    }
}