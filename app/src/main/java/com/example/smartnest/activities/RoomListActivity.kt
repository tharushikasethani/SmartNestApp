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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room_list)

        homeId = intent.getStringExtra("homeId")
        floorId = intent.getStringExtra("floorId")
        val floorName = intent.getStringExtra("floorName") ?: "My Rooms"

        findViewById<android.widget.TextView>(R.id.txtTitle)?.text = floorName
        findViewById<android.widget.ImageView>(R.id.btnBack).setOnClickListener { finish() }

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

                        roomsList.add(
                            ListRowItem(
                                id = id,
                                title = name,
                                subtitle = "$devicesCount Devices",
                                iconRes = IconMapper.resolve(iconKey)
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