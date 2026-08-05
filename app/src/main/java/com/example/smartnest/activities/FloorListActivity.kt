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

class FloorListActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val floorsList = mutableListOf<ListRowItem>()
    private lateinit var adapter: ListRowAdapter
    private var homeId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_floor_list)

        homeId = intent.getStringExtra("homeId")
        val homeName = intent.getStringExtra("homeName") ?: "My Floors"
        
        findViewById<android.widget.TextView>(R.id.txtTitle)?.text = homeName
        findViewById<android.widget.FrameLayout>(R.id.btnBack).setOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rvList)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = ListRowAdapter(floorsList) { floor ->
            val intent = Intent(this, RoomListActivity::class.java)
            intent.putExtra("homeId", homeId)
            intent.putExtra("floorId", floor.id)
            intent.putExtra("floorName", floor.title)
            startActivity(intent)
        }
        rv.adapter = adapter

        findViewById<android.view.View>(R.id.btnAdd).setOnClickListener {
            val intent = Intent(this, AddFloorActivity::class.java)
            intent.putExtra("homeId", homeId)
            startActivity(intent)
        }

        loadFloors()
    }

    private fun loadFloors() {
        val uid = auth.currentUser?.uid ?: return
        if (homeId == null) return

        database.getReference("users").child(uid).child("homes").child(homeId!!).child("floors")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    floorsList.clear()
                    for (floorSnapshot in snapshot.children) {
                        val id = floorSnapshot.key ?: ""
                        val name = floorSnapshot.child("name").getValue(String::class.java) ?: "Unnamed Floor"
                        val type = floorSnapshot.child("type").getValue(String::class.java) ?: "ground"
                        
                        val roomsCount = floorSnapshot.child("rooms").childrenCount

                        floorsList.add(
                            ListRowItem(
                                id = id,
                                title = name,
                                subtitle = "$roomsCount Rooms",
                                iconRes = IconMapper.resolve(type)
                            )
                        )
                    }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@FloorListActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}