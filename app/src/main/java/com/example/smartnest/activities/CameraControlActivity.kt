package com.example.smartnest.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.TextView
import com.example.smartnest.R

class CameraControlActivity : BaseDeviceControlActivity() {

    private val mockImages = intArrayOf(
        R.drawable.ic_camera,
        R.drawable.ic_camera,
        R.drawable.ic_camera
    )
    private var mockIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_control)
        setupCommonHeader("Camera View")

        val ivSnapshot = findViewById<ImageView>(R.id.ivSnapshot)
        val tvLastUpdated = findViewById<TextView>(R.id.tvLastUpdated)
        val tvStatus = findViewById<TextView>(R.id.tvStatusValue)
        val btnRefresh = findViewById<TextView>(R.id.btnRefresh)
        val btnLiveView = findViewById<TextView>(R.id.btnLiveView)

        ivSnapshot.setImageResource(R.drawable.ic_camera)

        fun updateTimestamp() {
            val now = java.text.SimpleDateFormat("MMM dd, yyyy  hh:mm a", java.util.Locale.getDefault())
                .format(java.util.Date())
            tvLastUpdated.text = "Last updated: $now"
        }
        updateTimestamp()

        btnRefresh.setOnClickListener {
            btnRefresh.isEnabled = false
            btnRefresh.text = "Refreshing..."
            Handler(Looper.getMainLooper()).postDelayed({
                mockIndex = (mockIndex + 1) % mockImages.size
                ivSnapshot.setImageResource(mockImages[mockIndex])
                updateTimestamp()
                btnRefresh.text = "Refresh Snapshot"
                btnRefresh.isEnabled = true
            }, 1500)
        }

        btnLiveView.setOnClickListener {
            val intent = Intent(this, LiveViewActivity::class.java)
            intent.putExtra("device_name", deviceName)
            startActivity(intent)
        }

        getDeviceRef()?.child("status")?.get()?.addOnSuccessListener {
            val s = it.getValue(String::class.java) ?: "ON"
            tvStatus.text = s
            tvStatus.setTextColor(if (s == "ON") 0xFF34C759.toInt() else 0xFFFF3B30.toInt())
        }
    }
}
