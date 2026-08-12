package com.example.smartnest.activities

import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.smartnest.DeviceImageMapper
import com.example.smartnest.IconMapper
import com.example.smartnest.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

abstract class BaseDeviceControlActivity : AppCompatActivity() {

    protected val auth = FirebaseAuth.getInstance()
    protected val database = FirebaseDatabase.getInstance()

    protected var homeId: String? = null
    protected var floorId: String? = null
    protected var roomId: String? = null
    protected var deviceId: String? = null
    protected var deviceName: String = ""
    protected var deviceType: String = ""
    protected var roomName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Make activity edge-to-edge to remove the bottom navigation bar background
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

        parseExtras()
    }

    private fun parseExtras() {
        homeId = intent.getStringExtra("homeId")
        floorId = intent.getStringExtra("floorId")
        roomId = intent.getStringExtra("roomId")
        deviceId = intent.getStringExtra("device_id")
        deviceName = intent.getStringExtra("device_name") ?: "Device"
        deviceType = intent.getStringExtra("device_type") ?: "light"
        roomName = intent.getStringExtra("roomName") ?: ""
    }

    protected fun getDeviceRef(): DatabaseReference? {
        val uid = auth.currentUser?.uid ?: return null
        if (homeId == null || floorId == null || roomId == null || deviceId == null) return null
        return database.getReference("users")
            .child(uid)
            .child("homes")
            .child(homeId!!)
            .child("floors")
            .child(floorId!!)
            .child("rooms")
            .child(roomId!!)
            .child("devices")
            .child(deviceId!!)
    }

    protected fun setupCommonHeader(title: String) {
        val ivIcon = findViewById<ImageView>(R.id.ivDeviceIcon)
        ivIcon?.setImageResource(DeviceImageMapper.resolve(deviceType))
        ivIcon?.imageTintList = null // Clear tint for realistic images

        findViewById<TextView>(R.id.tvDeviceName)?.text = deviceName
        findViewById<TextView>(R.id.tvDeviceRoom)?.text = roomName

        findViewById<FrameLayout>(R.id.btnBack)?.setOnClickListener { finish() }
    }

    protected fun openSchedule() {
        val intent = Intent(this, ScheduleActivity::class.java).apply {
            putExtra("device_name", deviceName)
            putExtra("device_id", deviceId ?: "")
            putExtra("homeId", homeId)
            putExtra("floorId", floorId)
            putExtra("roomId", roomId)
        }
        startActivity(intent)
    }

    protected fun openReport() {
        val intent = Intent(this, ReportsActivity::class.java).apply {
            putExtra("deviceName", deviceName)
            putExtra("deviceId", deviceId ?: "")
        }
        startActivity(intent)
    }
}
