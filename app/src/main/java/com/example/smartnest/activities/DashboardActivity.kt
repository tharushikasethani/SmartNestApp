package com.example.smartnest.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartnest.R
import com.example.smartnest.adapter.ActivityAdapter
import com.example.smartnest.model.ActivityItem
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar

class DashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private val recentActivity = listOf(
        ActivityItem(R.drawable.ic_lamp,    "Living Room Light turned ON",     "10:30 AM"),
        ActivityItem(R.drawable.ic_warning_small, "Iron automatically turned OFF", "09:15 AM"),
        ActivityItem(R.drawable.ic_camera,  "Kitchen Camera is active",        "08:45 AM"),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Transparent status bar with dark icons (light theme)
        window.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = Color.TRANSPARENT
            decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR   // dark icons for light bg
        }

        setContentView(R.layout.activity_dashboard)

        auth = FirebaseAuth.getInstance()

        // Set username from Firebase auth
        val displayName = auth.currentUser?.email?.substringBefore("@") ?: "there"
        val nameCapitalized = displayName.replaceFirstChar { it.uppercase() }
        findViewById<android.widget.TextView>(R.id.txtTitle).text = "$nameCapitalized"

        // Smart time-of-day greeting
        val greetingText = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11  -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..20 -> "Good Evening"
            else      -> "Good Night"
        }
        findViewById<android.widget.TextView>(R.id.txtWelcome).text = greetingText

        // Recent activity RecyclerView
        val rvActivity = findViewById<RecyclerView>(R.id.rvActivity)
        rvActivity.layoutManager = LinearLayoutManager(this)
        rvActivity.adapter = ActivityAdapter(recentActivity)

        // Button click handlers
        findViewById<FrameLayout>(R.id.btnMenu).setOnClickListener {
            // TODO: open a drawer or settings menu
        }

        findViewById<android.widget.LinearLayout>(R.id.cardHome).setOnClickListener {
            startActivity(Intent(this, MyHomesActivity::class.java))
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

        // Bottom nav click handlers
        // navHome is current screen, no navigation needed
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