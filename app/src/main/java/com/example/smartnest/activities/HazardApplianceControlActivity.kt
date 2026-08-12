package com.example.smartnest.activities

import android.app.AlertDialog
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.SeekBar
import android.widget.TextView
import com.example.smartnest.R
import com.example.smartnest.util.UsageTracker
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class HazardApplianceControlActivity : BaseDeviceControlActivity() {

    private var isOn = false
    private var maxRuntimeMinutes = 30
    private var remainingSeconds = 0
    private var timerActive = false
    private var countDownTimer: CountDownTimer? = null

    private lateinit var tvStatus: TextView
    private lateinit var tvTimer: TextView
    private lateinit var tvRuntimeLabel: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var warningBanner: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hazard_appliance_control)
        setupCommonHeader("Hazard Appliance")

        tvStatus = findViewById(R.id.tvStatusValue)
        tvTimer = findViewById(R.id.tvTimerValue)
        tvRuntimeLabel = findViewById(R.id.tvRuntimeValue)
        seekBar = findViewById(R.id.seekMaxRuntime)
        warningBanner = findViewById(R.id.tvWarningBanner)

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
        findViewById<android.view.View>(R.id.btnUsageReport).setOnClickListener { openReport() }

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

    private fun forceOff(reason: String = "SAFETY_CUTOFF") {
        isOn = false
        tvStatus.text = "OFF"
        tvStatus.setTextColor(0xFFFF3B30.toInt())
        getDeviceRef()?.let {
            UsageTracker.turnOff(it, deviceId!!, deviceName, deviceType, auth.currentUser!!.uid, reason)
        }
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
                // Avoid excessive database writes, only update locally. 
                // Reliable shutoff should happen on the device/cloud side.
            }
            override fun onFinish() {
                forceOff()
            }
        }.start()
    }

    private fun updateState(on: Boolean) {
        if (on && !isOn) {
            AlertDialog.Builder(this)
                .setTitle("Confirm Enable")
                .setMessage("This device is a fire-risk appliance. It will auto-turn-off after $maxRuntimeMinutes minutes. Continue?")
                .setPositiveButton("Enable") { _, _ ->
                    isOn = true
                    getDeviceRef()?.let { UsageTracker.turnOn(it, deviceId!!, deviceName, deviceType, auth.currentUser!!.uid) }
                    startTimer()
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else if (!on && isOn) {
            forceOff("MANUAL_OFF")
        }
    }

    private fun observeDeviceData() {
        getDeviceRef()?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return

                val dbStatus = snapshot.child("status").getValue(String::class.java) ?: "OFF"
                val lastOnTimestamp = snapshot.child("last_on_timestamp").getValue(Long::class.java)

                // Passive tracking check
                UsageTracker.checkAndRecordZombieUsage(
                    getDeviceRef()!!, dbStatus, lastOnTimestamp,
                    deviceId!!, deviceName, deviceType, auth.currentUser!!.uid
                )

                isOn = dbStatus == "ON"
                tvStatus.text = dbStatus
                tvStatus.setTextColor(if (isOn) 0xFF34C759.toInt() else 0xFFFF3B30.toInt())

                maxRuntimeMinutes = snapshot.child("maxRuntimeMinutes").getValue(Int::class.java) ?: 30
                seekBar.progress = maxRuntimeMinutes
                tvRuntimeLabel.text = "$maxRuntimeMinutes min"

                val dbTimerActive = snapshot.child("timerActive").getValue(Boolean::class.java) ?: false
                val dbRemaining = snapshot.child("remainingSeconds").getValue(Int::class.java) ?: 0

                if (isOn && dbTimerActive) {
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
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        super.onDestroy()
    }
}
