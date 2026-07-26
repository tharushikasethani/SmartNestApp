package com.example.smartnest.activities

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartnest.R
import com.example.smartnest.adapter.DeviceStatusAdapter
import com.example.smartnest.model.DeviceStatusItem

class DeviceListActivity : AppCompatActivity() {

    private val devices = listOf(
        DeviceStatusItem("light1", "Light", R.drawable.ic_lamp, "ON", true),
        DeviceStatusItem("fan1", "Fan", R.drawable.ic_fan, "OFF", false),
        DeviceStatusItem("tv1", "TV", R.drawable.ic_tv, "ON", true),
        DeviceStatusItem("camera1", "Camera", R.drawable.ic_camera, "ONLINE", true),
        DeviceStatusItem("plug1", "Smart Plug", R.drawable.ic_plug, "OFF", false),
        DeviceStatusItem("purifier1", "Air Purifier", R.drawable.ic_purifier, "ON", true),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_list)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rvList)
        rv.layoutManager = GridLayoutManager(this, 2)
        rv.adapter = DeviceStatusAdapter(devices) { device ->
            Toast.makeText(this, "Open ${device.name} control", Toast.LENGTH_SHORT).show()
            // TODO: startActivity(Intent(this, DeviceControlActivity::class.java))
        }

        findViewById<View>(R.id.btnAdd).setOnClickListener {
            Toast.makeText(this, "Add new device functionality coming soon", Toast.LENGTH_SHORT).show()
        }
    }
}