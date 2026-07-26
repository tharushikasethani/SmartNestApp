package com.example.smartnest.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartnest.R
import com.example.smartnest.adapter.ListRowAdapter
import com.example.smartnest.model.ListRowItem

class MyHomesActivity : AppCompatActivity() {

    private val homes = listOf(
        ListRowItem("home1", "Family House", "2 Floors • 18 Devices", R.drawable.ic_house_family),
        ListRowItem("home2", "Apartment", "1 Floor • 6 Devices", R.drawable.ic_apartment),
        ListRowItem("home3", "Apartment", "1 Floor • 6 Devices", R.drawable.ic_apartment),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_homes)

        findViewById<android.widget.ImageView>(R.id.btnBack).setOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rvList)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = ListRowAdapter(homes) { home ->
            val intent = Intent(this, FloorListActivity::class.java)
            intent.putExtra("homeId", home.id)
            intent.putExtra("homeName", home.title)
            startActivity(intent)
        }

        findViewById<android.view.View>(R.id.btnAdd).setOnClickListener {
            // TODO: startActivity(Intent(this, CreateHomeActivity::class.java))
            android.widget.Toast.makeText(this, "Add Home functionality coming soon", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}