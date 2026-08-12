package com.example.smartnest.activities

import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
    private var countDownTimer: CountDownTimer? = null

    private lateinit var tvStatus: TextView
    private lateinit var tvTimer: TextView
    private lateinit var tvRuntimeLabel: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var warningBanner: TextView
    private var cardSchedule: View? = null
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
                val mins = if (progress < 1) 1 else progress
                tvRuntimeLabel.text = "$mins min"
                maxRuntimeMinutes = mins
                if (fromUser) {
                    getDeviceRef()?.child("maxRuntimeMinutes")?.setValue(maxRuntimeMinutes)
                }
            }
            override fun onStartTrackingTouch(bar: SeekBar?) {}
            override fun onStopTrackingTouch(bar: SeekBar?) {}
        })

        findViewById<View>(R.id.btnOn).setOnClickListener { updateState(true) }
        findViewById<View>(R.id.btnOff).setOnClickListener { updateState(false) }
        findViewById<View>(R.id.btnSchedule).setOnClickListener { openSchedule() }
        findViewById<View>(R.id.btnUsageReport).setOnClickListener { openReport() }

        observeDeviceData()
    }

    private fun updateTimerDisplay() {
        val min = remainingSeconds / 60
        val sec = remainingSeconds % 60
        tvTimer.text = String.format("%02d:%02d", min, sec)
    }

    private fun forceOff(isAutoShutoff: Boolean = false) {
        // Stop Local UI Timer
        countDownTimer?.cancel()
        countDownTimer = null
        remainingSeconds = 0
        updateTimerDisplay()
        warningBanner.visibility = View.GONE

        // Update UI State
        currentStatus = DeviceStatus.OFF
        tvStatus.text = "OFF"
        tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_off_gray))
        
        // Update Database
        val ref = getDeviceRef()
        ref?.child("status")?.setValue("OFF")
        ref?.child("timerActive")?.setValue(false)
        ref?.child("remainingSeconds")?.setValue(0)
        
        // Track Usage
        com.example.smartnest.util.UsageTracker.turnOff(ref!!, deviceId!!, deviceName, deviceType, auth.currentUser!!.uid)

        if (isAutoShutoff) {
            showSafetyAlert()
        }
    }

    private fun showSafetyAlert() {
        if (isFinishing) return
        
        AlertDialog.Builder(this)
            .setTitle("SAFETY NOTIFICATION")
            .setMessage("The $deviceName has been automatically turned OFF after reaching its maximum safe runtime of $maxRuntimeMinutes minutes.")
            .setPositiveButton("I UNDERSTAND", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
        
        Toast.makeText(this, "Safety Shutoff Triggered", Toast.LENGTH_LONG).show()
    }

    private fun runCountDown(seconds: Int) {
        if (seconds <= 0) return
        
        countDownTimer?.cancel()
        remainingSeconds = seconds
        updateTimerDisplay()
        
        countDownTimer = object : CountDownTimer((remainingSeconds * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingSeconds = (millisUntilFinished / 1000).toInt()
                updateTimerDisplay()
                
                // Sync to DB every 15 seconds
                if (remainingSeconds > 0 && remainingSeconds % 15 == 0) {
                    getDeviceRef()?.child("remainingSeconds")?.setValue(remainingSeconds)
                }
            }
            override fun onFinish() {
                forceOff(true) // This is where the alert is triggered
            }
        }.start()
        
        warningBanner.visibility = View.VISIBLE
    }

    private fun updateState(on: Boolean) {
        if (on && currentStatus != DeviceStatus.ON) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Confirm Enable")
            builder.setMessage("This $deviceName is a high-heat appliance. It will automatically shut off after $maxRuntimeMinutes minutes. Proceed?")
            builder.setPositiveButton("Enable") { _, _ ->
                val totalSeconds = maxRuntimeMinutes * 60
                val ref = getDeviceRef()
                
                ref?.child("status")?.setValue("ON")
                ref?.child("timerActive")?.setValue(true)
                ref?.child("remainingSeconds")?.setValue(totalSeconds)
                
                com.example.smartnest.util.UsageTracker.turnOn(ref!!, deviceId!!, deviceName, deviceType, auth.currentUser!!.uid)
                
                runCountDown(totalSeconds)
            }
            builder.setNegativeButton("Cancel", null)
            val dialog = builder.create()
            dialog.show()
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(0xFFF0532D.toInt())
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(0xFF8A8A8E.toInt())
        } else if (!on && currentStatus == DeviceStatus.ON) {
            forceOff(false) // Manual turn off - no alert
        }
    }

    private fun observeDeviceData() {
        getDeviceRef()?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return

                val statusStr = snapshot.child("status").getValue(String::class.java) ?: "OFF"
                val newStatus = try { DeviceStatus.valueOf(statusStr) } catch (_: Exception) { DeviceStatus.OFF }
                
                val statusChanged = newStatus != currentStatus
                currentStatus = newStatus
                
                tvStatus.text = currentStatus.text
                tvStatus.setTextColor(ContextCompat.getColor(this@HazardApplianceControlActivity, currentStatus.textColorRes))

                // Max runtime slider sync
                val dbMaxMins = snapshot.child("maxRuntimeMinutes").getValue(Int::class.java) ?: 30
                if (!seekBar.isPressed) {
                    maxRuntimeMinutes = dbMaxMins
                    seekBar.progress = maxRuntimeMinutes
                    tvRuntimeLabel.text = "$maxRuntimeMinutes min"
                }

                val dbTimerActive = snapshot.child("timerActive").getValue(Boolean::class.java) ?: false
                val dbRemaining = snapshot.child("remainingSeconds").getValue(Int::class.java) ?: 0

                if (currentStatus == DeviceStatus.ON) {
                    // If turned ON (e.g. by schedule) but no timer is in DB, start one
                    if (!dbTimerActive) {
                        val totalSeconds = maxRuntimeMinutes * 60
                        val ref = getDeviceRef()
                        ref?.child("timerActive")?.setValue(true)
                        ref?.child("remainingSeconds")?.setValue(totalSeconds)
                        return // Let the next loop handle runCountDown
                    }

                    warningBanner.visibility = View.VISIBLE
                    // Start local timer if not running or significantly desynced
                    if (countDownTimer == null || Math.abs(remainingSeconds - dbRemaining) > 10) {
                        runCountDown(dbRemaining)
                    }
                } else {
                    // If OFF, ensure timer is killed
                    countDownTimer?.cancel()
                    countDownTimer = null
                    remainingSeconds = 0
                    updateTimerDisplay()
                    warningBanner.visibility = View.GONE
                }

                // Handle Schedule Info UI
                val schedule = snapshot.child("schedule")
                if (schedule.exists() && (schedule.child("enabled").getValue(Boolean::class.java) ?: false)) {
                    val start = schedule.child("startTime").getValue(String::class.java) ?: ""
                    val end = schedule.child("endTime").getValue(String::class.java) ?: ""
                    val daysRaw = schedule.child("days").getValue(String::class.java) ?: ""

                    if (start.isNotEmpty() && end.isNotEmpty()) {
                        cardSchedule?.visibility = View.VISIBLE
                        tvScheduleTime?.text = "$start - $end"

                        val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
                        val daysText = daysRaw.split(",").filter { it.isNotEmpty() }
                            .mapNotNull { it.trim().toIntOrNull() }.sorted()
                            .joinToString(", ") { dayLabels[it] }

                        tvScheduleDays?.text = if (daysText.isNotEmpty()) daysText else "Once"

                        // Automation: Force ON if schedule says so
                        val shouldBeOn = ScheduleValidator.isDeviceShouldBeOn(start, end, daysRaw)
                        if (shouldBeOn && currentStatus == DeviceStatus.OFF) {
                            getDeviceRef()?.child("status")?.setValue("ON")
                        }
                    }
                } else {
                    cardSchedule?.visibility = View.GONE
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
