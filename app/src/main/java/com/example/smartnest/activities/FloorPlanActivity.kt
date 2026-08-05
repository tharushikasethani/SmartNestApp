package com.example.smartnest.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.smartnest.IconMapper
import com.example.smartnest.R
import com.example.smartnest.model.DeviceStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FloorPlanActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private var homeId: String? = null
    private var floorId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_floor_plan)

        homeId = intent.getStringExtra("homeId")
        floorId = intent.getStringExtra("floorId")
        val floorName = intent.getStringExtra("floorName") ?: "Floor Plan"

        findViewById<TextView>(R.id.tvFloorTitle).text = floorName
        findViewById<FrameLayout>(R.id.btnBack).setOnClickListener { finish() }
        loadFloorPlan()
    }

    private fun loadFloorPlan() {
        val uid = auth.currentUser?.uid ?: return
        if (homeId == null || floorId == null) return

        database.getReference("users").child(uid).child("homes").child(homeId!!)
            .child("floors").child(floorId!!).child("rooms")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val rooms = mutableListOf<RoomPlanData>()
                    var index = 0
                    for (roomSnap in snapshot.children) {
                        val id = roomSnap.key ?: ""
                        val name = roomSnap.child("name").getValue(String::class.java) ?: "Room"
                        val iconKey = roomSnap.child("icon").getValue(String::class.java) ?: "sofa"
                        val gr = roomSnap.child("gridRow").getValue(Int::class.java) ?: (index / 4)
                        val gc = roomSnap.child("gridCol").getValue(Int::class.java) ?: (index % 4)

                        val devices = mutableListOf<DeviceOnPlan>()
                        val devSnap = roomSnap.child("devices")
                        for (d in devSnap.children) {
                            val did = d.key ?: ""
                            val dn = d.child("deviceName").getValue(String::class.java) ?: "Device"
                            val dt = d.child("deviceType").getValue(String::class.java) ?: "light"
                            val ds = d.child("status").getValue(String::class.java) ?: "OFF"
                            val devStatus = try { DeviceStatus.valueOf(ds) } catch (_: Exception) { DeviceStatus.OFF }
                            devices.add(DeviceOnPlan(did, dn, dt, devStatus))
                        }
                        rooms.add(RoomPlanData(id, name, iconKey, gr, gc, devices))
                        index++
                    }
                    renderGrid(rooms)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun renderGrid(rooms: List<RoomPlanData>) {
        val grid = findViewById<GridLayout>(R.id.floorGrid)
        grid.removeAllViews()

        val cols = 4
        val rows = 6
        grid.columnCount = cols
        grid.rowCount = rows

        val rowSpecs = Array(rows) { r ->
            GridLayout.spec(r, 1, 1f)
        }
        val colSpecs = Array(cols) { c ->
            GridLayout.spec(c, 1, 1f)
        }

        val occupied = Array(rows) { BooleanArray(cols) }

        for (room in rooms) {
            val r = room.gridRow.coerceIn(0, rows - 1)
            val c = room.gridCol.coerceIn(0, cols - 1)

            if (occupied[r][c]) continue
            occupied[r][c] = true

            val card = layoutInflater.inflate(R.layout.item_floor_plan_room, grid, false)

            card.findViewById<ImageView>(R.id.ivRoomIcon)
                .setImageResource(IconMapper.resolve(room.iconKey))
            card.findViewById<TextView>(R.id.tvRoomName).text = room.name

            val devContainer = card.findViewById<LinearLayout>(R.id.containerRoomDevices)
            for (dev in room.devices) {
                val chip = layoutInflater.inflate(R.layout.item_plan_device_chip, devContainer, false)
                chip.findViewById<ImageView>(R.id.ivPlanDevIcon)
                    .setImageResource(IconMapper.resolve(dev.type))
                chip.findViewById<View>(R.id.dotPlanDevStatus)
                    .setBackgroundResource(dev.status.dotRes)
                chip.setOnClickListener {
                    val intent = buildDeviceIntent(dev)
                    startActivity(intent)
                }
                devContainer.addView(chip)
            }

            val lp = GridLayout.LayoutParams(rowSpecs[r], colSpecs[c])
            lp.width = 0
            lp.height = GridLayout.LayoutParams.WRAP_CONTENT
            lp.setMargins(4, 4, 4, 4)
            grid.addView(card, lp)
        }
    }

    private fun buildDeviceIntent(dev: DeviceOnPlan): Intent {
        val targetClass = when (dev.type) {
            "light", "lamp", "blinds", "tv", "ceiling_fan", "speaker",
            "refrigerator", "kitchen_oven", "washing_machine",
            "bathroom_heater", "temperature_sensor" ->
                DeviceControlActivity::class.java
            "outlet", "smart_plug" -> OutletControlActivity::class.java
            "multi_switch" -> MultiSwitchControlActivity::class.java
            "hazard_appliance", "iron" -> HazardApplianceControlActivity::class.java
            "camera", "deck_camera" -> CameraControlActivity::class.java
            else -> DeviceControlActivity::class.java
        }
        return Intent(this, targetClass).apply {
            putExtra("device_id", dev.id)
            putExtra("device_name", dev.name)
            putExtra("device_type", dev.type)
            putExtra("device_status", dev.status.text)
            putExtra("homeId", homeId)
            putExtra("floorId", floorId)
            putExtra("roomId", "")
            putExtra("roomName", "")
        }
    }

    private data class RoomPlanData(
        val id: String,
        val name: String,
        val iconKey: String,
        val gridRow: Int,
        val gridCol: Int,
        val devices: List<DeviceOnPlan>
    )

    private data class DeviceOnPlan(
        val id: String,
        val name: String,
        val type: String,
        val status: DeviceStatus
    )
}
