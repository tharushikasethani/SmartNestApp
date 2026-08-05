package com.example.smartnest.activities

import android.app.AlertDialog
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.smartnest.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class HazardApplianceControlActivity : BaseDeviceControlActivity() {

    private var isOn = false
    private var maxRuntimeMinutes = 30
    private var remainingSeconds = 0
    private var timerActive = false
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hazard_appliance_control)
        setupCommonHeader("Hazard Appliance")

        val tvStatus = findViewById<TextView>(R.id.tvStatusValue)
        val tvTimer = findViewById<TextView>(R.id.tvTimerValue)
        val tvRuntimeLabel = findViewById<TextView>(R.id.tvRuntimeValue)
        val seekBar = findViewById<SeekBar>(R.id.seekMaxRuntime)
        val warningBanner = findViewById<TextView>(R.id.tvWarningBanner)

        fun updateTimerDisplay() {
            val min = remainingSeconds / 60
            val sec = remainingSeconds % 60
            tvTimer.text = String.format("%02d:%02d", min, sec)
        }

        fun stopTimer() {
            countDownTimer?.cancel()
            countDownTimer = null
            timerActive = false
            remainingSeconds = 0
            updateTimerDisplay()
            getDeviceRef()?.child("timerActive")?.setValue(false)
            getDeviceRef()?.child("remainingSeconds")?.setValue(0)
            warningBanner.visibility = android.view.View.GONE
        }

        fun forceOff() {
            isOn = false
            tvStatus.text = "OFF"
            tvStatus.setTextColor(0xFFFF3B30.toInt())
            getDeviceRef()?.child("status")?.setValue("OFF")
            stopTimer()
        }

        fun startTimer() {
            remainingSeconds = maxRuntimeMinutes * 60
            timerActive = true
            updateTimerDisplay()
            getDeviceRef()?.child("timerActive")?.setValue(true)
            getDeviceRef()?.child("remainingSeconds")?.setValue(remainingSeconds)
            getDeviceRef()?.child("maxRuntimeMinutes")?.setValue(maxRuntimeMinutes)
            warningBanner.visibility = android.view.View.VISIBLE

            countDownTimer?.cancel()
            countDownTimer = object : CountDownTimer((remainingSeconds * 1000).toLong(), 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    remainingSeconds = (millisUntilFinished / 1000).toInt()
                    updateTimerDisplay()
                    getDeviceRef()?.child("remainingSeconds")?.setValue(remainingSeconds)
                }
                override fun onFinish() {
                    forceOff()
                }
            }.start()
        }

        fun updateState(on: Boolean) {
            if (on && !isOn) {
                AlertDialog.Builder(this)
                    .setTitle("Confirm Enable")
                    .setMessage("This device is a fire-risk appliance. " +
                            "It will auto-turn-off after $maxRuntimeMinutes minutes. Continue?")
                    .setPositiveButton("Enable") { _, _ ->
                        isOn = true
                        tvStatus.text = "ON"
                        tvStatus.setTextColor(0xFF34C759.toInt())
                        getDeviceRef()?.child("status")?.setValue("ON")
                        startTimer()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else if (!on && isOn) {
                isOn = false
                tvStatus.text = "OFF"
                tvStatus.setTextColor(0xFFFF3B30.toInt())
                getDeviceRef()?.child("status")?.setValue("OFF")
                stopTimer()
            }
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                val mins = if (progress < 5) 5 else progress
                tvRuntimeLabel.text = "$mins min"
                maxRuntimeMinutes = mins
            }
            override fun onStartTrackingTouch(bar: SeekBar?) {}
            override fun onStopTrackingTouch(bar: SeekBar?) {}
        })

        findViewById<android.view.View>(R.id.btnOn).setOnClickListener { updateState(true) }
        findViewById<android.view.View>(R.id.btnOff).setOnClickListener { updateState(false) }
        findViewById<android.view.View>(R.id.btnSchedule).setOnClickListener { openSchedule() }

        getDeviceRef()?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                maxRuntimeMinutes = snapshot.child("maxRuntimeMinutes").getValue(Int::class.java) ?: 30
                remainingSeconds = snapshot.child("remainingSeconds").getValue(Int::class.java) ?: 0
                timerActive = snapshot.child("timerActive").getValue(Boolean::class.java) ?: false
                val status = snapshot.child("status").getValue(String::class.java) ?: "OFF"
                isOn = status == "ON"
                tvStatus.text = status
                tvStatus.setTextColor(if (isOn) 0xFF34C759.toInt() else 0xFFFF3B30.toInt())

                seekBar.progress = maxRuntimeMinutes
                tvRuntimeLabel.text = "$maxRuntimeMinutes min"

                if (isOn && timerActive && remainingSeconds > 0) {
                    warningBanner.visibility = android.view.View.VISIBLE
                    updateTimerDisplay()
                    countDownTimer?.cancel()
                    countDownTimer = object : CountDownTimer((remainingSeconds * 1000).toLong(), 1000) {
                        override fun onTick(millisUntilFinished: Long) {
                            remainingSeconds = (millisUntilFinished / 1000).toInt()
                            updateTimerDisplay()
                            getDeviceRef()?.child("remainingSeconds")?.setValue(remainingSeconds)
                        }
                        override fun onFinish() {
                            forceOff()
                        }
                    }.start()
                } else if (isOn && !timerActive) {
                    startTimer()
                } else {
                    warningBanner.visibility = android.view.View.GONE
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
