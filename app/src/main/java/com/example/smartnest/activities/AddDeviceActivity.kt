package com.example.smartnest.activities

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartnest.IconMapper
import com.example.smartnest.R
import com.example.smartnest.adapter.SelectableGridAdapter
import com.example.smartnest.model.SelectableItem
import com.example.smartnest.model.TypeCatalogItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AddDeviceActivity : AppCompatActivity() {

    private lateinit var adapter: SelectableGridAdapter
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private var homeId: String? = null
    private var floorId: String? = null
    private var roomId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_device)

        homeId = intent.getStringExtra("homeId")
        floorId = intent.getStringExtra("floorId")
        roomId = intent.getStringExtra("roomId")

        val etDeviceName = findViewById<EditText>(R.id.etDeviceName)
        findViewById<android.widget.FrameLayout>(R.id.btnBack).setOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rvDeviceTypes)
        rv.layoutManager = GridLayoutManager(this, 3)

        loadDeviceTypes(rv)

        findViewById<TextView>(R.id.btnSave).setOnClickListener {
            if (!::adapter.isInitialized) {
                Toast.makeText(this, "Device types still loading...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val name = etDeviceName?.text?.toString()?.trim() ?: ""
            val selected = adapter.getSelected()
            val uid = auth.currentUser?.uid
            
            if (name.isEmpty()) {
                Toast.makeText(this, "Enter a device name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selected == null) {
                Toast.makeText(this, "Select a device type", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (uid == null || homeId == null || floorId == null || roomId == null) {
                Toast.makeText(this, "Missing required info", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val deviceRef = database.getReference("users")
                .child(uid)
                .child("homes")
                .child(homeId!!)
                .child("floors")
                .child(floorId!!)
                .child("rooms")
                .child(roomId!!)
                .child("devices")
                .push()

            val deviceData = mapOf(
                "deviceName" to name,
                "deviceType" to selected.id,
                "status" to "OFF",
                "createdAt" to System.currentTimeMillis().toString()
            )

            deviceRef.setValue(deviceData)
                .addOnSuccessListener {
                    Toast.makeText(this, "$name added", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadDeviceTypes(rv: RecyclerView) {
        val defaultItems = listOf(

            SelectableItem(
                "light",
                "Light",
                R.drawable.ic_light
            ),

            SelectableItem(
                "blinds",
                "Blinds",
                R.drawable.ic_blinds1
            ),

            SelectableItem(
                "tv",
                "TV",
                R.drawable.ic_tv1
            ),

            SelectableItem(
                "deck_camera",
                "Deck Camera",
                R.drawable.ic_camera
            ),

            SelectableItem(
                "refrigerator",
                "Refrigerator",
                R.drawable.ic_refrigerator1
            ),

            SelectableItem(
                "kitchen_oven",
                "Kitchen Oven",
                R.drawable.ic_oven11
            ),

            SelectableItem(
                "ceiling_fan",
                "Ceiling Fan",
                R.drawable.ic_fan1
            ),

            SelectableItem(
                "smart_plug",
                "Smart Plug",
                R.drawable.ic_plug1
            ),

            SelectableItem(
                "temperature_sensor",
                "Air Conditioner",
                R.drawable.ic_temperature_sensor1
            ),

            SelectableItem(
                "speaker",
                "Smart Speaker",
                R.drawable.ic_speaker1
            ),

            SelectableItem(
                "door_lock",
                "Door Lock",
                R.drawable.ic_lock1
            ),

            SelectableItem(
                "washing_machine",
                "Washing Machine",
                R.drawable.ic_washing_machine1
            ),

            SelectableItem(
                "iron",
                "Iron",
                R.drawable.ic_iron1
            ),
            SelectableItem(
                "outlet",
                "Outlet",
                R.drawable.ic_plug1
            ),
            SelectableItem(
                "multi_switch",
                "Multi Switch",
                R.drawable.ic_switch_multi1
            ),
            SelectableItem(
                "hazard_appliance",
                "Hazard Appliance",
                R.drawable.ic_iron1
            ),
            SelectableItem(
                "camera",
                "Security Camera",
                R.drawable.ic_camera
            ),
            SelectableItem(
                "bathroom_heater",
                "Heater",
                R.drawable.ic_bathroom_heater1
            ),

            SelectableItem(
                "others",
                "Others",
                R.drawable.ic_grid
            )
        )

        database.getReference("deviceTypes")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val items = mutableListOf<SelectableItem>()
                    for (child in snapshot.children) {
                        val id = child.key ?: continue
                        val catalogItem = child.getValue(TypeCatalogItem::class.java) ?: continue
                        if (catalogItem.label.isNotEmpty()) {
                            items.add(
                                SelectableItem(
                                    id = id,
                                    label = catalogItem.label,
                                    iconRes = IconMapper.resolve(catalogItem.icon)
                                )
                            )
                        }
                    }

                    val finalItems = if (items.isNotEmpty()) items else defaultItems
                    adapter = SelectableGridAdapter(finalItems, showLabel = true, selectedIndex = 0) { _, _ -> }
                    rv.adapter = adapter
                }

                override fun onCancelled(error: DatabaseError) {
                    adapter = SelectableGridAdapter(defaultItems, showLabel = true, selectedIndex = 0) { _, _ -> }
                    rv.adapter = adapter
                }
            })
    }
}