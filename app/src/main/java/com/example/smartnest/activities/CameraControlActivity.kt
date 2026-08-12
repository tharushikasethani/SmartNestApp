package com.example.smartnest.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.TextView
import com.example.smartnest.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class CameraControlActivity : BaseDeviceControlActivity() {

    private val mockImages = intArrayOf(
        R.drawable.living_room,
        R.drawable.bedroom,
        R.drawable.porch,
        R.drawable.dining_room
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

        // Set initial snapshot based on device name or type to make it feel more "fixed"
        mockIndex = (deviceName.length % mockImages.size)
        ivSnapshot.setImageResource(mockImages[mockIndex])
        ivSnapshot.scaleType = ImageView.ScaleType.CENTER_CROP

        fun updateTimestamp() {
            val now = java.text.SimpleDateFormat("MMM dd, yyyy  hh:mm:ss a", java.util.Locale.getDefault())
                .format(java.util.Date())
            tvLastUpdated.text = "Snapshot: $now"
        }
        updateTimestamp()

        btnRefresh.setOnClickListener {
            btnRefresh.isEnabled = false
            btnRefresh.text = "Capturing snapshot..."
            Handler(Looper.getMainLooper()).postDelayed({
                mockIndex = (mockIndex + 1) % mockImages.size
                ivSnapshot.setImageResource(mockImages[mockIndex])
                updateTimestamp()
                btnRefresh.text = "Refresh Snapshot"
                btnRefresh.isEnabled = true
            }, 1200)
        }

        btnLiveView.setOnClickListener {
            val intent = Intent(this, LiveViewActivity::class.java)
            intent.putExtra("device_name", deviceName)
            // Mock a URI stream by passing a dummy URL
            intent.putExtra("stream_uri", "rtsp://mock-camera-stream.local/live/${deviceName.lowercase().replace(" ", "_")}")
            startActivity(intent)
        }

        findViewById<TextView>(R.id.btnUsageReport).setOnClickListener {
            openReport()
        }

        findViewById<android.view.View>(R.id.btnOn).setOnClickListener {
            getDeviceRef()?.let { ref ->
                com.example.smartnest.util.UsageTracker.turnOn(ref, deviceId!!, deviceName, deviceType, auth.currentUser!!.uid)
            }
        }

        findViewById<android.view.View>(R.id.btnOff).setOnClickListener {
            getDeviceRef()?.let { ref ->
                com.example.smartnest.util.UsageTracker.turnOff(ref, deviceId!!, deviceName, deviceType, auth.currentUser!!.uid)
            }
        }

        getDeviceRef()?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val s = snapshot.child("status").getValue(String::class.java) ?: "ON"
                tvStatus.text = s
                tvStatus.setTextColor(if (s == "ON") 0xFF34C759.toInt() else 0xFFFF3B30.toInt())
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
