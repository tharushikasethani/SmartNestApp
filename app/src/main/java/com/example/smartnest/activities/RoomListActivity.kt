package com.example.smartnest.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartnest.IconMapper
import com.example.smartnest.R
import com.example.smartnest.adapter.ListRowAdapter
import com.example.smartnest.model.ListRowItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class RoomListActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val roomsList = mutableListOf<ListRowItem>()
    private lateinit var adapter: ListRowAdapter
    private var homeId: String? = null
    private var floorId: String? = null
    private var floorName: String = ""

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

        setContentView(R.layout.activity_room_list)

        homeId = intent.getStringExtra("homeId")
        floorId = intent.getStringExtra("floorId")
        floorName = intent.getStringExtra("floorName") ?: "My Rooms"

        findViewById<android.widget.TextView>(R.id.txtTitle)?.text = floorName
        findViewById<android.widget.FrameLayout>(R.id.btnBack).setOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rvList)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = ListRowAdapter(roomsList) { room ->
            val intent = Intent(this, DeviceListActivity::class.java)
            intent.putExtra("homeId", homeId)
            intent.putExtra("floorId", floorId)
            intent.putExtra("roomId", room.id)
            intent.putExtra("roomName", room.title)
            startActivity(intent)
        }
        rv.adapter = adapter


        findViewById<android.view.View>(R.id.btnAdd).setOnClickListener {
            val intent = Intent(this, AddRoomActivity::class.java)
            intent.putExtra("homeId", homeId)
            intent.putExtra("floorId", floorId)
            startActivity(intent)
        }

        loadRooms()
    }

    private fun loadRooms() {
        val uid = auth.currentUser?.uid ?: return
        if (homeId == null || floorId == null) return

        database.getReference("users")
            .child(uid)
            .child("homes")
            .child(homeId!!)
            .child("floors")
            .child(floorId!!)
            .child("rooms")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    roomsList.clear()
                    for (roomSnapshot in snapshot.children) {
                        val id = roomSnapshot.key ?: ""
                        val name = roomSnapshot.child("name").getValue(String::class.java) ?: "Unnamed Room"
                        val iconKey = roomSnapshot.child("icon").getValue(String::class.java) ?: "sofa"
                        
                        val devicesCount = roomSnapshot.child("devices").childrenCount
                        val bgRes = when(iconKey.lowercase()) {
                            "Kitchen","kitchen" -> R.drawable.ic_kitchen_bg // if you have a kitchen background
                            "bedroom", "Bedroom" -> R.drawable.bedroom
                            "living_room", "sofa","Living Room" -> R.drawable.living_room
                            "bathroom","Bathroom" -> R.drawable.bathroom
                            "porch","Porch" -> R.drawable.porch
                            "Media Room","media_room" -> R.drawable.media_room
                            "Dining Room","dining_room" -> R.drawable.dining_room
                            "Laundary","laundry" -> R.drawable.laundry
                            else -> R.drawable.home1 // fallback
                        }

                        roomsList.add(
                            ListRowItem(
                                id = id,
                                title = name,
                                subtitle = "$devicesCount Devices",
                                iconRes = IconMapper.resolve(iconKey),
                                backgroundRes = bgRes
                            )
                        )
                    }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@RoomListActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}