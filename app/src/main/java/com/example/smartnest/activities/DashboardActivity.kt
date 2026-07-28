package com.example.smartnest.activities

import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartnest.R
import com.example.smartnest.adapter.ActivityAdapter
import com.example.smartnest.model.ActivityItem
import com.google.firebase.auth.FirebaseAuth

class DashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private val recentActivity = listOf(
        ActivityItem(R.drawable.ic_alert, "Living Room Light turned ON", "10:30 AM"),
        ActivityItem(R.drawable.ic_warning_small, "Iron automatically turned OFF", "09:15 AM"),
        ActivityItem(R.drawable.ic_camera, "Kitchen Camera is active", "08:45 AM"),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        auth = FirebaseAuth.getInstance()

        // Personalize greeting with the signed-in user's name if you saved it,
        // otherwise falls back to the email prefix.
        val displayName = auth.currentUser?.email?.substringBefore("@") ?: "there"
        findViewById<android.widget.TextView>(R.id.txtTitle).text = "$displayName 👋"

        val rvActivity = findViewById<RecyclerView>(R.id.rvActivity)
        rvActivity.layoutManager = LinearLayoutManager(this)
        rvActivity.adapter = ActivityAdapter(recentActivity)


        findViewById<FrameLayout>(R.id.btnMenu).setOnClickListener {
            // TODO: open a drawer or settings menu
        }

        findViewById<android.widget.LinearLayout>(R.id.cardHome).setOnClickListener {
            // TODO: navigate to FloorListActivity / Home Details
        }

        findViewById<android.widget.LinearLayout>(R.id.actionFloors).setOnClickListener {
            // TODO: startActivity(Intent(this, FloorListActivity::class.java))
        }
        findViewById<android.widget.LinearLayout>(R.id.actionCameras).setOnClickListener {
            // TODO: startActivity(Intent(this, CamerasActivity::class.java))
        }
        findViewById<android.widget.LinearLayout>(R.id.actionScenes).setOnClickListener {
            // TODO: startActivity(Intent(this, ScenesActivity::class.java))
        }
        findViewById<android.widget.LinearLayout>(R.id.actionReports).setOnClickListener {
            // TODO: startActivity(Intent(this, ReportsActivity::class.java))
        }

        findViewById<android.widget.LinearLayout>(R.id.navReports).setOnClickListener {
            // TODO: startActivity(Intent(this, ReportsActivity::class.java))
        }
        findViewById<android.widget.LinearLayout>(R.id.navAlerts).setOnClickListener {
            // TODO: startActivity(Intent(this, AlertsActivity::class.java))
        }
        findViewById<android.widget.LinearLayout>(R.id.navSettings).setOnClickListener {
            // TODO: startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
}