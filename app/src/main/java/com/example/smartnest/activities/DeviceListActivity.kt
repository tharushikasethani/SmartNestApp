package com.example.smartnest.activities

import android.os.Bundle
import android.content.Intent
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartnest.IconMapper
import com.example.smartnest.R
import com.example.smartnest.adapter.DeviceStatusAdapter
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_list)

        homeId = intent.getStringExtra("homeId")
        floorId = intent.getStringExtra("floorId")
        roomId = intent.getStringExtra("roomId")
        val roomName = intent.getStringExtra("roomName") ?: "My Devices"

        findViewById<android.widget.TextView>(R.id.txtTitle)?.text = roomName
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rvList)
        rv.layoutManager = GridLayoutManager(this, 2)
        adapter = DeviceStatusAdapter(devicesList) { device ->
            Toast.makeText(this, "Open ${device.name} control", Toast.LENGTH_SHORT).show()
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
                        val status = deviceSnapshot.child("status").getValue(String::class.java) ?: "OFF"
                        
                        devicesList.add(
                            DeviceStatusItem(
                                id = id,
                                name = name,
                                iconRes = IconMapper.resolve(type),
                                statusText = status,
                                isActive = status == "ON"
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