package com.example.smartnest.activities

import android.app.AlertDialog
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.smartnest.R
import com.example.smartnest.model.DeviceStatus
import com.example.smartnest.utils.ScheduleValidator
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class HazardApplianceControlActivity : BaseDeviceControlActivity() {

    private var currentStatus: DeviceStatus = DeviceStatus.OFF
    private var maxRuntimeMinutes = 30
    private var remainingSeconds = 0
    private var timerActive = false
    private var countDownTimer: CountDownTimer? = null

    private lateinit var tvStatus: TextView
    private lateinit var tvTimer: TextView
    private lateinit var tvRuntimeLabel: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var warningBanner: TextView
    private var cardSchedule: android.view.View? = null
    private var tvScheduleTime: TextView? = null
    private var tvScheduleDays: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hazard_appliance_control)
        setupCommonHeader("Hazard Appliance")

        tvStatus = findViewById(R.id.tvStatusValue)
        tvTimer = findViewById(R.id.tvTimerValue)
        tvRuntimeLabel = findViewById(R.id.tvRuntimeValue)
        seekBar = findViewById(R.id.seekMaxRuntime)
        warningBanner = findViewById(R.id.tvWarningBanner)
        
        cardSchedule = findViewById(R.id.cardCurrentSchedule)
        tvScheduleTime = findViewById(R.id.tvActiveScheduleTime)
        tvScheduleDays = findViewById(R.id.tvActiveScheduleDays)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                val mins = if (progress < 5) 5 else progress
                tvRuntimeLabel.text = "$mins min"
                maxRuntimeMinutes = mins
                if (fromUser) {
                    getDeviceRef()?.child("maxRuntimeMinutes")?.setValue(maxRuntimeMinutes)
                }
            }
            override fun onStartTrackingTouch(bar: SeekBar?) {}
            override fun onStopTrackingTouch(bar: SeekBar?) {}
        })

        findViewById<android.view.View>(R.id.btnOn).setOnClickListener { updateState(true) }
        findViewById<android.view.View>(R.id.btnOff).setOnClickListener { updateState(false) }
        findViewById<android.view.View>(R.id.btnSchedule).setOnClickListener { openSchedule() }

        observeDeviceData()
    }

    private fun updateTimerDisplay() {
        val min = remainingSeconds / 60
        val sec = remainingSeconds % 60
        tvTimer.text = String.format("%02d:%02d", min, sec)
    }

    private fun stopTimer() {
        countDownTimer?.cancel()
        countDownTimer = null
        timerActive = false
        remainingSeconds = 0
        updateTimerDisplay()
        getDeviceRef()?.child("timerActive")?.setValue(false)
        getDeviceRef()?.child("remainingSeconds")?.setValue(0)
        warningBanner.visibility = android.view.View.GONE
    }

    private fun forceOff() {
        currentStatus = DeviceStatus.OFF
        tvStatus.text = "OFF"
        tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_off_gray))
        getDeviceRef()?.child("status")?.setValue("OFF")
        stopTimer()
    }

    private fun startTimer() {
        remainingSeconds = maxRuntimeMinutes * 60
        timerActive = true
        updateTimerDisplay()
        getDeviceRef()?.child("timerActive")?.setValue(true)
        getDeviceRef()?.child("remainingSeconds")?.setValue(remainingSeconds)
        getDeviceRef()?.child("maxRuntimeMinutes")?.setValue(maxRuntimeMinutes)
        warningBanner.visibility = android.view.View.VISIBLE
        runCountDown()
    }

    private fun runCountDown() {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer((remainingSeconds * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingSeconds = (millisUntilFinished / 1000).toInt()
                updateTimerDisplay()
            }
            override fun onFinish() {
                forceOff()
            }
        }.start()
    }

    private fun updateState(on: Boolean) {
        if (on && currentStatus != DeviceStatus.ON) {
            AlertDialog.Builder(this)
                .setTitle("Confirm Enable")
                .setMessage("This device is a fire-risk appliance. It will auto-turn-off after $maxRuntimeMinutes minutes. Continue?")
                .setPositiveButton("Enable") { _, _ ->
                    currentStatus = DeviceStatus.ON
                    getDeviceRef()?.child("status")?.setValue("ON")
                    startTimer()
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else if (!on && currentStatus == DeviceStatus.ON) {
            forceOff()
        }
    }

    private fun observeDeviceData() {
        getDeviceRef()?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return

                val statusStr = snapshot.child("status").getValue(String::class.java) ?: "OFF"
                currentStatus = try { DeviceStatus.valueOf(statusStr) } catch (_: Exception) { DeviceStatus.OFF }
                
                tvStatus.text = currentStatus.text
                tvStatus.setTextColor(ContextCompat.getColor(this@HazardApplianceControlActivity, currentStatus.textColorRes))

                maxRuntimeMinutes = snapshot.child("maxRuntimeMinutes").getValue(Int::class.java) ?: 30
                seekBar.progress = maxRuntimeMinutes
                tvRuntimeLabel.text = "$maxRuntimeMinutes min"

                val dbTimerActive = snapshot.child("timerActive").getValue(Boolean::class.java) ?: false
                val dbRemaining = snapshot.child("remainingSeconds").getValue(Int::class.java) ?: 0

                if (currentStatus == DeviceStatus.ON && dbTimerActive) {
                    warningBanner.visibility = android.view.View.VISIBLE
                    if (!timerActive || Math.abs(dbRemaining - remainingSeconds) > 5) {
                        remainingSeconds = dbRemaining
                        timerActive = true
                        runCountDown()
                    }
                } else {
                    timerActive = false
                    countDownTimer?.cancel()
                    warningBanner.visibility = android.view.View.GONE
                    remainingSeconds = 0
                    updateTimerDisplay()
                }

                // Handle Schedule Display
                val schedule = snapshot.child("schedule")
                if (schedule.exists() && (schedule.child("enabled").getValue(Boolean::class.java) ?: false)) {
                    val start = schedule.child("startTime").getValue(String::class.java) ?: ""
                    val end = schedule.child("endTime").getValue(String::class.java) ?: ""
                    val daysRaw = schedule.child("days").getValue(String::class.java) ?: ""

                    if (start.isNotEmpty() && end.isNotEmpty()) {
                        cardSchedule?.visibility = android.view.View.VISIBLE
                        tvScheduleTime?.text = "$start - $end"

                        val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
                        val daysText = daysRaw.split(",")
                            .filter { it.isNotEmpty() }
                            .mapNotNull { it.trim().toIntOrNull() }
                            .sorted()
                            .joinToString(", ") { dayLabels[it] }

                        tvScheduleDays?.text = if (daysText.isNotEmpty()) daysText else "Once"

                        // AUTOMATION: Check if device should be forced ON/OFF
                        val shouldBeOn = ScheduleValidator.isDeviceShouldBeOn(start, end, daysRaw)
                        if (shouldBeOn && currentStatus == DeviceStatus.OFF) {
                            getDeviceRef()?.child("status")?.setValue("ON")
                        }
                    } else {
                        cardSchedule?.visibility = android.view.View.GONE
                    }
                } else {
                    cardSchedule?.visibility = android.view.View.GONE
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        super.onDestroy()
    }
}
