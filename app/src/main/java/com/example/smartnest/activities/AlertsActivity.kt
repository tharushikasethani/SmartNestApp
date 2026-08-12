package com.example.smartnest.activities

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartnest.R
import com.example.smartnest.adapter.ActivityAdapter
import com.example.smartnest.model.ActivityItem
import com.example.smartnest.model.Alert
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class AlertsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var rvAlerts: RecyclerView
    private lateinit var tvEmptyState: TextView
    private val alertList = mutableListOf<Alert>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alerts)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        rvAlerts = findViewById(R.id.rvAlerts)
        tvEmptyState = findViewById(R.id.tvEmptyState)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        rvAlerts.layoutManager = LinearLayoutManager(this)
        
        loadAlerts()
    }

    private fun loadAlerts() {
        val uid = auth.currentUser?.uid ?: return
        val alertsRef = database.getReference("users").child(uid).child("alerts")

        alertsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                alertList.clear()
                for (child in snapshot.children) {
                    val alert = child.getValue(Alert::class.java)
                    if (alert != null) {
                        alertList.add(alert.copy(id = child.key ?: ""))
                    }
                }
                updateUI()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updateUI() {
        if (alertList.isEmpty()) {
            tvEmptyState.visibility = View.VISIBLE
            rvAlerts.visibility = View.GONE
        } else {
            tvEmptyState.visibility = View.GONE
            rvAlerts.visibility = View.VISIBLE

            val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
            val activityItems = alertList.sortedByDescending { it.timestamp }.map { alert ->
                ActivityItem(
                    iconRes = R.drawable.ic_hazard_warning,
                    text = alert.message,
                    time = sdf.format(Date(alert.timestamp))
                )
            }
            rvAlerts.adapter = ActivityAdapter(activityItems)
        }
    }
}
