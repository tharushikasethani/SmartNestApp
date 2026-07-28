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

class AddRoomActivity : AppCompatActivity() {

    private lateinit var adapter: SelectableGridAdapter
    private lateinit var etRoomName: EditText
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var homeId: String? = null
    private var floorId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_room)

        homeId = intent.getStringExtra("homeId")
        floorId = intent.getStringExtra("floorId")
        
        etRoomName = findViewById(R.id.etRoomName)
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rvRoomIcons)
        rv.layoutManager = GridLayoutManager(this, 3)

        loadRoomTypes(rv)

        findViewById<TextView>(R.id.btnSave).setOnClickListener {
            if (!::adapter.isInitialized) return@setOnClickListener

            val name = etRoomName.text.toString().trim()
            val selected = adapter.getSelected()
            val uid = auth.currentUser?.uid

            if (name.isEmpty()) {
                Toast.makeText(this, "Enter a room name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (uid == null || homeId == null || floorId == null) {
                Toast.makeText(this, "Missing required information", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val roomRef = database.getReference("users")
                .child(uid)
                .child("homes")
                .child(homeId!!)
                .child("floors")
                .child(floorId!!)
                .child("rooms")
                .push()

            val roomData = mapOf(
                "name" to name,
                "icon" to (selected?.id ?: "sofa")
            )

            roomRef.setValue(roomData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Room \"$name\" saved", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to save: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadRoomTypes(rv: RecyclerView) {
        val defaultItems = listOf(
            SelectableItem(
                "porch",
                "Porch",
                R.drawable.ic_porch
            ),

            SelectableItem(
                "living_room",
                "Living Room",
                R.drawable.ic_sofa
            ),

            SelectableItem(
                "media_room",
                "Media Room",
                R.drawable.ic_media_room
            ),

            SelectableItem(
                "primary_suite",
                "Primary Suite",
                R.drawable.ic_primary_suite
            ),

            SelectableItem(
                "bedroom",
                "Bedroom",
                R.drawable.ic_bed
            ),

            SelectableItem(
                "dining_room",
                "Dining Room",
                R.drawable.ic_dining_room
            ),

            SelectableItem(
                "kitchen",
                "Kitchen",
                R.drawable.ic_kitchen
            ),

            SelectableItem(
                "bathroom",
                "Bathroom",
                R.drawable.ic_bath
            ),

            SelectableItem(
                "laundry",
                "Laundry",
                R.drawable.ic_laundry
            ),

            SelectableItem(
                "garage",
                "Garage",
                R.drawable.ic_garage
            ),

            SelectableItem(
                "others",
                "Others",
                R.drawable.ic_grid
            )
        )

        database.getReference("roomTypes")
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