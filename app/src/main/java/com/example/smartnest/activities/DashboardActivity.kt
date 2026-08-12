package com.example.smartnest.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartnest.R
import com.example.smartnest.adapter.DeviceStatusAdapter
import com.example.smartnest.model.DeviceStatus
import com.example.smartnest.model.DeviceStatusItem
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar

class DashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private val quickDevices = listOf(
        DeviceStatusItem("camera1", "CCTV Camera", R.drawable.ic_camera1, DeviceStatus.ON, "camera", "Main Gate"),
        DeviceStatusItem("light1", "Smart Lamp", R.drawable.ic_lamp, DeviceStatus.ON, "light", "Living Room"),
        DeviceStatusItem("fan1", "Ceiling Fan", R.drawable.ic_fan, DeviceStatus.OFF, "fan", "Bedroom"),
        DeviceStatusItem("heater1", "Water Heater", R.drawable.ic_bathroom_heater, DeviceStatus.OFF, "heater", "Bathroom")
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Transparent status bar with dark icons (light theme)
        window.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = Color.TRANSPARENT
            navigationBarColor = Color.TRANSPARENT
            decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        setContentView(R.layout.activity_dashboard)

        auth = FirebaseAuth.getInstance()

        // Set username from Firebase auth
        val displayName = auth.currentUser?.email?.substringBefore("@") ?: "Alex"
        val nameCapitalized = displayName.replaceFirstChar { it.uppercase() }
        findViewById<TextView>(R.id.txtTitle).text = "Hi, $nameCapitalized"

        // Smart time-of-day greeting
        val greetingText = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11  -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..20 -> "Good Evening"
            else      -> "Good Night"
        }
        findViewById<TextView>(R.id.txtWelcome).text = greetingText

        // Quick Devices RecyclerView
        val rvQuickDevices = findViewById<RecyclerView>(R.id.rvQuickDevices)
        rvQuickDevices.layoutManager = GridLayoutManager(this, 2)
        rvQuickDevices.adapter = DeviceStatusAdapter(quickDevices) { device ->
            Toast.makeText(this, "Control ${device.name}", Toast.LENGTH_SHORT).show()
        }

        // Button click handlers
        findViewById<FrameLayout>(R.id.btnMenu).setOnClickListener {
            // TODO: open a drawer or settings menu
        }

        findViewById<FrameLayout>(R.id.cardHome).setOnClickListener {
            startActivity(Intent(this, MyHomesActivity::class.java))
        }

        findViewById<android.widget.Button>(R.id.btnViewMyHome).setOnClickListener {
            startActivity(Intent(this, MyHomesActivity::class.java))
        }

        // Bottom nav click handlers
        findViewById<LinearLayout>(R.id.navReports).setOnClickListener {
            startActivity(Intent(this, ReportsActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navAlerts).setOnClickListener {
            startActivity(Intent(this, AlertsActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.navSettings).setOnClickListener {
            // TODO: startActivity(Intent(this, SettingsActivity::class.java))
            Toast.makeText(this, "Settings coming soon", Toast.LENGTH_SHORT).show()
        }

        listenForSafetyAlerts()
    }

    private fun listenForSafetyAlerts() {
        val uid = auth.currentUser?.uid ?: return
        val alertsRef = com.google.firebase.database.FirebaseDatabase.getInstance()
            .getReference("users").child(uid).child("alerts")

        // Only listen for new alerts (after current time)
        val query = alertsRef.orderByChild("timestamp").startAt(System.currentTimeMillis().toDouble())

        query.addChildEventListener(object : com.google.firebase.database.ChildEventListener {
            override fun onChildAdded(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {
                val alert = snapshot.getValue(com.example.smartnest.model.Alert::class.java)
                if (alert != null && alert.type == "SAFETY_CUTOFF" && !alert.read) {
                    showSafetyCutoffDialog(alert)
                }
            }

            override fun onChildChanged(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: com.google.firebase.database.DataSnapshot) {}
            override fun onChildMoved(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        })
    }

    private fun showSafetyCutoffDialog(alert: com.example.smartnest.model.Alert) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("⚠️ Safety Cutoff")
            .setMessage(alert.message)
            .setPositiveButton("View Alerts") { _, _ ->
                startActivity(Intent(this, AlertsActivity::class.java))
            }
            .setNegativeButton("Dismiss", null)
            .show()
    }
}