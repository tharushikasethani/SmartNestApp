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

class MyHomesActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val homesList = mutableListOf<ListRowItem>()
    private lateinit var adapter: ListRowAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_homes)

        findViewById<android.widget.FrameLayout>(R.id.btnBack).setOnClickListener { finish() }

        val rv = findViewById<RecyclerView>(R.id.rvList)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = ListRowAdapter(homesList) { home ->
            val intent = Intent(this, FloorListActivity::class.java)
            intent.putExtra("homeId", home.id)
            intent.putExtra("homeName", home.title)
            startActivity(intent)
        }
        rv.adapter = adapter

        findViewById<android.view.View>(R.id.btnAdd).setOnClickListener {
            startActivity(Intent(this, AddHomeActivity::class.java))
        }

        loadHomes()
    }

    private fun loadHomes() {
        val uid = auth.currentUser?.uid ?: return
        database.getReference("users").child(uid).child("homes")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    homesList.clear()
                    for (homeSnapshot in snapshot.children) {
                        val id = homeSnapshot.key ?: ""
                        val name = homeSnapshot.child("name").getValue(String::class.java) ?: "Unnamed Home"
                        val type = homeSnapshot.child("type").getValue(String::class.java) ?: "house"
                        val address = homeSnapshot.child("address").getValue(String::class.java) ?: ""

                        // Fetching floor/device count if available, or static for now
                        val floorsCount = homeSnapshot.child("floors").childrenCount
                        val subtitle = if (address.isNotEmpty()) address else "$floorsCount Floors"
                        val bgRes = when(type) {
                            "apartment" -> R.drawable.apartment // your drawable name
                            "villa" -> R.drawable.villa
                            else -> R.drawable.home1 // default
                        }

                        homesList.add(
                            ListRowItem(id, name, subtitle, IconMapper.resolve(type), bgRes)
                        )
                    }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@MyHomesActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}