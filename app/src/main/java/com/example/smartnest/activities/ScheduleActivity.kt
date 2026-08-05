package com.example.smartnest.activities

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.smartnest.R

class ScheduleActivity : AppCompatActivity() {

    private val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
    private val selectedDays = mutableSetOf(0, 1, 2, 3, 4) // Mon–Fri selected by default
    private val dayViews = mutableListOf<TextView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule)

        val deviceName = intent.getStringExtra("device_name") ?: "Device"
        findViewById<TextView>(R.id.tvScheduleTitle).text = "$deviceName Schedule"

        findViewById<FrameLayout>(R.id.btnBack).setOnClickListener { finish() }

        val tvStart = findViewById<TextView>(R.id.tvStartTime)
        val tvEnd = findViewById<TextView>(R.id.tvEndTime)

        findViewById<LinearLayout>(R.id.rowStartTime).setOnClickListener {
            showTimePicker { time -> tvStart.text = time }
        }
        findViewById<LinearLayout>(R.id.rowEndTime).setOnClickListener {
            showTimePicker { time -> tvEnd.text = time }
        }

        buildDayPills()

        findViewById<TextView>(R.id.btnSaveSchedule).setOnClickListener {
            val days = selectedDays.sorted().joinToString(",") { dayLabels[it] }
            // TODO: persist start/end time + selectedDays to Firebase under
            // devices/{deviceId}/schedule
            Toast.makeText(
                this,
                "Saved: ${tvStart.text}–${tvEnd.text} on $days",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }
    }

    private fun buildDayPills() {
        val container = findViewById<LinearLayout>(R.id.rowDays)
        val sizePx = (36 * resources.displayMetrics.density).toInt()
        val marginPx = (6 * resources.displayMetrics.density).toInt()

        dayLabels.forEachIndexed { index, label ->
            val pill = TextView(this).apply {
                text = label
                textSize = 14f
                gravity = android.view.Gravity.CENTER
                setTextColor(
                    ContextCompat.getColor(
                        context,
                        if (index in selectedDays) android.R.color.white else R.color.text_primary_fallback
                    )
                )
                background = ContextCompat.getDrawable(
                    context,
                    if (index in selectedDays) R.drawable.bg_day_circle_selected else R.drawable.bg_day_circle_unselected
                )
            }
            val params = LinearLayout.LayoutParams(sizePx, sizePx)
            params.marginEnd = marginPx
            pill.layoutParams = params

            pill.setOnClickListener {
                if (index in selectedDays) selectedDays.remove(index) else selectedDays.add(index)
                pill.setBackgroundResource(
                    if (index in selectedDays) R.drawable.bg_day_circle_selected else R.drawable.bg_day_circle_unselected
                )
                pill.setTextColor(
                    ContextCompat.getColor(
                        this,
                        if (index in selectedDays) android.R.color.white else R.color.text_primary_fallback
                    )
                )
            }

            dayViews.add(pill)
            container.addView(pill)
        }
    }

    private fun showTimePicker(onPicked: (String) -> Unit) {
        val cal = java.util.Calendar.getInstance()
        TimePickerDialog(
            this,
            { _, hour, minute ->
                val amPm = if (hour < 12) "AM" else "PM"
                val hour12 = if (hour % 12 == 0) 12 else hour % 12
                val formatted = String.format("%02d:%02d %s", hour12, minute, amPm)
                onPicked(formatted)
            },
            cal.get(java.util.Calendar.HOUR_OF_DAY),
            cal.get(java.util.Calendar.MINUTE),
            false
        ).show()
    }
}