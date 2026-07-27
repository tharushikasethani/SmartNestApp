package com.example.smartnest.activities

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
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
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

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
        database.getReference("deviceTypes")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val items = mutableListOf<SelectableItem>()
                    for (child in snapshot.children) {
                        val id = child.key ?: continue
                        val catalogItem = child.getValue(TypeCatalogItem::class.java) ?: continue
                        items.add(
                            SelectableItem(
                                id = id,
                                label = catalogItem.label,
                                iconRes = IconMapper.resolve(catalogItem.icon)
                            )
                        )
                    }

                    if (items.isEmpty()) {
                        items.addAll(listOf(
                            SelectableItem("light", "Light", R.drawable.ic_lamp),
                            SelectableItem("fan", "Fan", R.drawable.ic_fan),
                            SelectableItem("camera", "Camera", R.drawable.ic_camera)
                        ))
                    }

                    adapter = SelectableGridAdapter(items, selectedIndex = 0) { _, _ -> }
                    rv.adapter = adapter
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@AddDeviceActivity, "Failed to load: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}