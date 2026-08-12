package com.example.smartnest.activities

import android.os.Bundle
import android.content.Intent
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartnest.DeviceImageMapper
import com.example.smartnest.IconMapper
import com.example.smartnest.R
import com.example.smartnest.adapter.DeviceStatusAdapter
import com.example.smartnest.model.DeviceStatus
import com.example.smartnest.model.DeviceStatusItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DeviceListActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val devicesList = mutableListOf<DeviceStatusItem>()
    private lateinit var adapter: DeviceStatusAdapter
    
    private var homeId: String? = null
    private var floorId: String? = null
    private var roomId: String? = null
    private var roomName: String = "My Devices"

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

        setContentView(R.layout.activity_device_list)

        homeId = intent.getStringExtra("homeId")
        floorId = intent.getStringExtra("floorId")
        roomId = intent.getStringExtra("roomId")
        roomName = intent.getStringExtra("roomName") ?: "My Devices"

        findViewById<android.widget.TextView>(R.id.txtTitle)?.text = roomName
        findViewById<android.widget.FrameLayout>(R.id.btnBack).setOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rvList)
        rv.layoutManager = GridLayoutManager(this, 2)
        adapter = DeviceStatusAdapter(devicesList) { device ->
            val type = device.deviceType.lowercase().trim()
            val targetClass = when (type) {
                "light", "lamp", "blinds", "tv", "ceiling_fan", "speaker", "refrigerator",
                "kitchen_oven", "washing_machine", "bathroom_heater", "temperature_sensor" ->
                    DeviceControlActivity::class.java
                "outlet", "plug" ->
                    OutletControlActivity::class.java
                "multi_switch","smart_plug" ->
                    MultiSwitchControlActivity::class.java
                "hazard_appliance", "iron", "heater", "oven" ->
                    HazardApplianceControlActivity::class.java
                "camera", "deck_camera" ->
                    CameraControlActivity::class.java
                else ->
                    DeviceControlActivity::class.java
            }
            val intent = Intent(this, targetClass).apply {
                putExtra("device_id", device.id)
                putExtra("device_name", device.name)
                putExtra("device_type", device.deviceType)
                putExtra("device_status", device.status.text)
                putExtra("homeId", homeId)
                putExtra("floorId", floorId)
                putExtra("roomId", roomId)
                putExtra("roomName", roomName)
            }
            startActivity(intent)
        }
        
        adapter.onToggleClick = { item, _ ->
            val newStatus = if (item.status == DeviceStatus.ON) "OFF" else "ON"
            val uid = auth.currentUser?.uid
            if (uid != null && homeId != null && floorId != null && roomId != null) {
                database.getReference("users")
                    .child(uid)
                    .child("homes")
                    .child(homeId!!)
                    .child("floors")
                    .child(floorId!!)
                    .child("rooms")
                    .child(roomId!!)
                    .child("devices")
                    .child(item.id)
                    .child("status")
                    .setValue(newStatus)
            }
        }

        rv.adapter = adapter

        findViewById<android.view.View>(R.id.btnAdd).setOnClickListener {
            val intent = Intent(this, AddDeviceActivity::class.java)
            intent.putExtra("homeId", homeId)
            intent.putExtra("floorId", floorId)
            intent.putExtra("roomId", roomId)
            startActivity(intent)
        }

        loadDevices()
    }

    private fun loadDevices() {
        val uid = auth.currentUser?.uid ?: return
        if (homeId == null || floorId == null || roomId == null) return

        database.getReference("users")
            .child(uid)
            .child("homes")
            .child(homeId!!)
            .child("floors")
            .child(floorId!!)
            .child("rooms")
            .child(roomId!!)
            .child("devices")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    devicesList.clear()
                    for (deviceSnapshot in snapshot.children) {
                        val id = deviceSnapshot.key ?: ""
                        val name = deviceSnapshot.child("deviceName").getValue(String::class.java) ?: "Unnamed Device"
                        val type = deviceSnapshot.child("deviceType").getValue(String::class.java) ?: "light"
                        val statusStr = deviceSnapshot.child("status").getValue(String::class.java) ?: "OFF"
                        val devStatus = try { DeviceStatus.valueOf(statusStr) } catch (_: Exception) { DeviceStatus.OFF }

                        devicesList.add(
                            DeviceStatusItem(
                                id = id,
                                name = name,
                                iconRes = DeviceImageMapper.resolve(type),
                                status = devStatus,
                                deviceType = type,
                                subtitle = roomName
                            )
                        )
                    }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@DeviceListActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}