package com.example.smartnest.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartnest.R
import com.example.smartnest.adapter.ListRowAdapter
import com.example.smartnest.model.ListRowItem

class RoomListActivity : AppCompatActivity() {

    private val rooms = listOf(
        ListRowItem("room1", "Living Room", "4 Devices", R.drawable.ic_sofa),
        ListRowItem("room2", "Kitchen", "3 Devices", R.drawable.ic_kitchen),
        ListRowItem("room3", "Bedroom", "5 Devices", R.drawable.ic_bed),
        ListRowItem("room4", "Bathroom", "2 Devices", R.drawable.ic_bath),
        ListRowItem("room5", "Dining Room", "1 Device", R.drawable.ic_kitchen),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room_list)

        findViewById<android.widget.ImageView>(R.id.btnBack).setOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rvList)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = ListRowAdapter(rooms) { room ->
            val intent = Intent(this, DeviceListActivity::class.java)
            intent.putExtra("roomId", room.id)
            intent.putExtra("roomName", room.title)
            startActivity(intent)
        }

        findViewById<android.widget.TextView>(R.id.btnAdd).setOnClickListener {
            // TODO: startActivity(Intent(this, AddRoomActivity::class.java))
        }
    }
}