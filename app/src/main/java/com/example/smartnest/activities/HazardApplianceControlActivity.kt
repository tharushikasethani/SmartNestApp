package com.example.smartnest.activities

import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
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
    private var tvScheduleCountdown: TextView? = null
    
    private var activeStartTime: String = ""
    private var activeEndTime: String = ""
    private var activeDays: String = ""

    private val scheduleCountdownHandler = Handler(Looper.getMainLooper())
    private val scheduleCountdownRunnable = object : Runnable {
        override fun run() {
            updateScheduleCountdownUI()
            scheduleCountdownHandler.postDelayed(this, 1000)
        }
    }

    private fun updateScheduleCountdownUI() {
        if (activeStartTime.isEmpty() || activeEndTime.isEmpty()) {
            tvScheduleCountdown?.visibility = View.GONE
            return
        }

        val remaining = ScheduleValidator.getRemainingSeconds(activeStartTime, activeEndTime, activeDays)
        if (remaining > 0) {
            val h = remaining / 3600
            val m = (remaining % 3600) / 60
            val s = remaining % 60
            tvScheduleCountdown?.visibility = View.VISIBLE
            tvScheduleCountdown?.text = String.format("Ends in: %02d:%02d:%02d", h, m, s)
        } else {
            tvScheduleCountdown?.visibility = View.GONE
            
            // AUTOMATION: Turn off when schedule period ends
            if (currentStatus == DeviceStatus.ON) {
                val shouldBeOn = ScheduleValidator.isDeviceShouldBeOn(activeStartTime, activeEndTime, activeDays)
                if (!shouldBeOn) {
                    forceOff(false) // Turn off iron normally at end of schedule
                }
            }
        }
    }

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
        tvScheduleCountdown = findViewById(R.id.tvScheduleCountdown)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                val mins = if (progress < 1) 1 else progress
                tvRuntimeLabel.text = String.format("%d min", mins)
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

    private fun startTimerInDb(seconds: Int) {
        val ref = getDeviceRef()
        ref?.child("timerActive")?.setValue(true)
        ref?.child("remainingSeconds")?.setValue(seconds)
        ref?.child("maxRuntimeMinutes")?.setValue(maxRuntimeMinutes)
    }

    private fun stopTimerInDb() {
        val ref = getDeviceRef()
        ref?.child("timerActive")?.setValue(false)
        ref?.child("remainingSeconds")?.setValue(0)
    }

    private fun forceOff(isAutoShutoff: Boolean = false) {
        // Update Local State
        currentStatus = DeviceStatus.OFF
        tvStatus.text = "OFF"
        tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_off_gray))
        
        // Stop UI
        countDownTimer?.cancel()
        countDownTimer = null
        remainingSeconds = 0
        updateTimerDisplay()
        warningBanner.visibility = View.GONE

        // Update DB
        val ref = getDeviceRef()
        com.example.smartnest.util.UsageTracker.turnOff(ref!!, deviceId!!, deviceName, deviceType, auth.currentUser!!.uid)
        stopTimerInDb()

        if (isAutoShutoff) {
            showSafetyAlert()
        }
    }

    private fun showSafetyAlert() {
        if (isFinishing) return
        
        AlertDialog.Builder(this)
            .setTitle("SAFETY ALERT")
            .setMessage("Hazardous appliance ($deviceName) has been automatically SHUT OFF to prevent fire hazard after reaching $maxRuntimeMinutes minutes of use.")
            .setPositiveButton("I UNDERSTAND", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setCancelable(false)
            .show()
        
        Toast.makeText(this, "CRITICAL: Hazard appliance auto-shutoff!", Toast.LENGTH_LONG).show()
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
                forceOff(true)
            }
        }.start()
        
        warningBanner.visibility = View.VISIBLE
    }

    private fun updateState(on: Boolean) {
        if (on && currentStatus != DeviceStatus.ON) {
            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            builder.setTitle("Confirm Enable")
            builder.setMessage("CAUTION: This device ($deviceName) is a fire risk. It will automatically shut off after $maxRuntimeMinutes minutes. Do you want to proceed?")
            builder.setPositiveButton("Enable") { _, _ ->
                currentStatus = DeviceStatus.ON
                val totalSeconds = maxRuntimeMinutes * 60
                val ref = getDeviceRef()
                com.example.smartnest.util.UsageTracker.turnOn(ref!!, deviceId!!, deviceName, deviceType, auth.currentUser!!.uid)
                startTimerInDb(totalSeconds)
                runCountDown(totalSeconds)
            }
            builder.setNegativeButton("Cancel", null)
            val dialog = builder.create()
            dialog.show()
            
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setTextColor(0xFFF0532D.toInt())
        } else if (!on && currentStatus == DeviceStatus.ON) {
            forceOff(false)
        }
    }

    private fun observeDeviceData() {
        getDeviceRef()?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return

                val statusStr = snapshot.child("status").getValue(String::class.java) ?: "OFF"
                val newStatus = try { DeviceStatus.valueOf(statusStr) } catch (_: Exception) { DeviceStatus.OFF }
                
                currentStatus = newStatus
                
                tvStatus.text = currentStatus.text
                tvStatus.setTextColor(ContextCompat.getColor(this@HazardApplianceControlActivity, currentStatus.textColorRes))

                // Max runtime slider sync
                val dbMaxMins = snapshot.child("maxRuntimeMinutes").getValue(Int::class.java) ?: 30
                if (!seekBar.isPressed) {
                    maxRuntimeMinutes = dbMaxMins
                    seekBar.progress = maxRuntimeMinutes
                    tvRuntimeLabel.text = String.format("%d min", maxRuntimeMinutes)
                }

                val dbTimerActive = snapshot.child("timerActive").getValue(Boolean::class.java) ?: false
                val dbRemaining = snapshot.child("remainingSeconds").getValue(Int::class.java) ?: 0

                if (currentStatus == DeviceStatus.ON && dbTimerActive) {
                    warningBanner.visibility = View.VISIBLE
                    if (countDownTimer == null || Math.abs(remainingSeconds - dbRemaining) > 20) {
                        runCountDown(dbRemaining)
                    }
                } else {
                    if (currentStatus == DeviceStatus.OFF) {
                        countDownTimer?.cancel()
                        countDownTimer = null
                        remainingSeconds = 0
                        updateTimerDisplay()
                        warningBanner.visibility = View.GONE
                    }
                }

                // Handle Schedule Info UI
                val schedule = snapshot.child("schedule")
                if (schedule.exists() && (schedule.child("enabled").getValue(Boolean::class.java) ?: false)) {
                    val start = schedule.child("startTime").getValue(String::class.java) ?: ""
                    val end = schedule.child("endTime").getValue(String::class.java) ?: ""
                    val daysRaw = schedule.child("days").getValue(String::class.java) ?: ""
                    
                    activeStartTime = start
                    activeEndTime = end
                    activeDays = daysRaw

                    if (start.isNotEmpty() && end.isNotEmpty()) {
                        cardSchedule?.visibility = View.VISIBLE
                        tvScheduleTime?.text = String.format("%s - %s", start, end)

                        val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
                        val daysText = daysRaw.split(",").filter { it.isNotEmpty() }
                            .mapNotNull { it.trim().toIntOrNull() }.sorted()
                            .joinToString(", ") { dayLabels[it] }

                        tvScheduleDays?.text = if (daysText.isNotEmpty()) daysText else "Once"

                        // Automation
                        val shouldBeOn = ScheduleValidator.isDeviceShouldBeOn(start, end, daysRaw)
                        if (shouldBeOn && currentStatus == DeviceStatus.OFF) {
                            getDeviceRef()?.child("status")?.setValue("ON")
                        }
                    }
                } else {
                    cardSchedule?.visibility = View.GONE
                    activeStartTime = ""
                }
                updateScheduleCountdownUI()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    override fun onResume() {
        super.onResume()
        scheduleCountdownHandler.post(scheduleCountdownRunnable)
    }

    override fun onPause() {
        super.onPause()
        scheduleCountdownHandler.removeCallbacks(scheduleCountdownRunnable)
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        scheduleCountdownHandler.removeCallbacks(scheduleCountdownRunnable)
        super.onDestroy()
    }
}
