package com.example.smartnest.activities

import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.smartnest.DeviceImageMapper
import com.example.smartnest.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class DeviceControlActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private var isOn = false

    private var homeId: String? = null
    private var floorId: String? = null
    private var roomId: String? = null
    private var deviceId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_control)

        homeId = intent.getStringExtra("homeId")
        floorId = intent.getStringExtra("floorId")
        roomId = intent.getStringExtra("roomId")
        deviceId = intent.getStringExtra("device_id")
        val deviceName = intent.getStringExtra("device_name") ?: "Device"
        val deviceType = intent.getStringExtra("device_type") ?: "light"
        val roomName = intent.getStringExtra("roomName") ?: ""
        val initialStatus = intent.getStringExtra("device_status") ?: "OFF"

        isOn = initialStatus == "ON"

        findViewById<TextView>(R.id.tvDeviceName)?.text = deviceName
        findViewById<TextView>(R.id.tvDeviceRoom)?.text = roomName

        val ivIcon = findViewById<ImageView>(R.id.ivDeviceIcon)
        if (ivIcon != null) {
            ivIcon.setImageResource(DeviceImageMapper.resolve(deviceType))
            ivIcon.imageTintList = null // Clear tint for realistic images
        }
        
        val blinkAnim = AnimationUtils.loadAnimation(this, R.anim.blink)
        val fanRotateAnim = AnimationUtils.loadAnimation(this, R.anim.fan_rotate)

        val tvStatus = findViewById<TextView>(R.id.tvStatusValue)
        val tvBrightness = findViewById<TextView>(R.id.tvBrightnessValue)
        val seekBar = findViewById<SeekBar>(R.id.seekBrightness)
        val rowBrightness = findViewById<android.widget.LinearLayout>(R.id.rowBrightness)
        val dividerBrightness = findViewById<android.view.View>(R.id.dividerBrightness)

        val isLight = deviceType == "light" || deviceType == "lamp"
        val isFan = deviceType == "fan" || deviceType == "ceiling_fan"
        
        if (rowBrightness != null && !isLight) {
            rowBrightness.visibility = android.view.View.GONE
        }
        if (dividerBrightness != null && !isLight) {
            dividerBrightness.visibility = android.view.View.GONE
        }

        val deviceRef = deviceId?.let { id ->
            auth.currentUser?.uid?.let { uid ->
                if (homeId != null && floorId != null && roomId != null) {
                    database.getReference("users")
                        .child(uid)
                        .child("homes")
                        .child(homeId!!)
                        .child("floors")
                        .child(floorId!!)
                        .child("rooms")
                        .child(roomId!!)
                        .child("devices")
                        .child(id)
                } else null
            }
        }

        fun updateState(on: Boolean) {
            isOn = on
            tvStatus?.text = if (on) "ON" else "OFF"
            val imageRes = DeviceImageMapper.resolve(deviceType, on)
            findViewById<ImageView>(R.id.ivDeviceIcon).setImageResource(imageRes)
            tvStatus?.setTextColor(
                if (on) 0xFF34C759.toInt() else 0xFFFF3B30.toInt()
            )
            if (on && isFan) {
                ivIcon?.startAnimation(fanRotateAnim)
            } else {
                ivIcon?.clearAnimation()
                ivIcon?.alpha = 1.0f
                ivIcon?.rotation = 0f
            }
            deviceRef?.child("status")?.setValue(if (on) "ON" else "OFF")
        }

        fun loadState() {
            deviceRef?.child("status")?.get()?.addOnSuccessListener {
                val currentStatus = it.getValue(String::class.java) ?: "OFF"
                updateState(currentStatus == "ON")
            }
            if (isLight) {
                deviceRef?.child("brightness")?.get()?.addOnSuccessListener {
                    val brightness = it.getValue(Int::class.java) ?: 75
                    seekBar?.progress = brightness
                    tvBrightness?.text = "$brightness%"
                }
            }
        }

        updateState(isOn)

        seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvBrightness?.text = "$progress%"
            }
            override fun onStartTrackingTouch(bar: SeekBar?) {}
            override fun onStopTrackingTouch(bar: SeekBar?) {
                deviceRef?.child("brightness")?.setValue(seekBar.progress)
            }
        })

        findViewById<android.view.View>(R.id.btnOn)?.setOnClickListener { updateState(true) }
        findViewById<android.view.View>(R.id.btnOff)?.setOnClickListener { updateState(false) }

        findViewById<android.widget.FrameLayout>(R.id.btnBack)?.setOnClickListener { finish() }

        findViewById<android.view.View>(R.id.btnSchedule)?.setOnClickListener {
            val intent = android.content.Intent(this, ScheduleActivity::class.java)
            intent.putExtra("device_name", deviceName)
            intent.putExtra("device_id", deviceId ?: "")
            startActivity(intent)
        }

        loadState()
    }
}