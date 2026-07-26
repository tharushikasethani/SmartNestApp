package com.example.smartnest.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartnest.R
import com.example.smartnest.adapter.ListRowAdapter
import com.example.smartnest.model.ListRowItem

class FloorListActivity : AppCompatActivity() {

    private val floors = listOf(
        ListRowItem("floor1", "Ground Floor", "5 Rooms", R.drawable.ic_floors),
        ListRowItem("floor2", "First Floor", "4 Rooms", R.drawable.ic_floors),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_floor_list)

        findViewById<android.widget.ImageView>(R.id.btnBack).setOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rvList)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = ListRowAdapter(floors) { floor ->
            val intent = Intent(this, RoomListActivity::class.java)
            intent.putExtra("floorId", floor.id)
            intent.putExtra("floorName", floor.title)
            startActivity(intent)
        }

        findViewById<android.widget.TextView>(R.id.btnAdd).setOnClickListener {
            // TODO: startActivity(Intent(this, AddFloorActivity::class.java))
        }
    }
}