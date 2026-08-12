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
import com.example.smartnest.model.UsageRecord
import com.example.smartnest.util.UsageBarChartView
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class ReportsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    
    private lateinit var tvTotalUsage: TextView
    private lateinit var tvMostUsed: TextView
    private lateinit var usageChart: UsageBarChartView
    private lateinit var rvDeviceUsage: RecyclerView
    private lateinit var rvRecentSessions: RecyclerView
    private lateinit var tvEmptyState: TextView
    
    private val allUsageRecords = mutableListOf<UsageRecord>()
    private val activeSessions = mutableListOf<UsageRecord>()
    private var filterDays = 0 // 0 for Today, 7 for Last 7 days
    
    private var targetDeviceId: String? = null
    private var targetDeviceName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reports)

        targetDeviceId = intent.getStringExtra("deviceId")
        if (targetDeviceId.isNullOrEmpty()) targetDeviceId = null
        targetDeviceName = intent.getStringExtra("deviceName")

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        tvTotalUsage = findViewById(R.id.tvTotalUsage)
        tvMostUsed = findViewById(R.id.tvMostUsed)
        usageChart = findViewById(R.id.usageChart)
        rvDeviceUsage = findViewById(R.id.rvDeviceUsage)
        rvRecentSessions = findViewById(R.id.rvRecentSessions)
        tvEmptyState = findViewById(R.id.tvEmptyState)

        if (targetDeviceId != null) {
            findViewById<TextView>(R.id.txtTitle)?.text = targetDeviceName ?: "Device Report"
            // Hide "Most Used" card as it's redundant for single device
            findViewById<View>(R.id.tvMostUsed)?.let { view ->
                view.visibility = View.GONE
                (view.parent as? View)?.visibility = View.GONE
                (view.parent?.parent as? View)?.visibility = View.GONE
            }
            // Hide "Usage by Device" section as it's redundant
            findViewById<View>(R.id.tvDeviceUsageHeader)?.visibility = View.GONE
            rvDeviceUsage.visibility = View.GONE
        }

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        rvDeviceUsage.layoutManager = LinearLayoutManager(this)
        rvRecentSessions.layoutManager = LinearLayoutManager(this)

        val chipGroup = findViewById<ChipGroup>(R.id.chipGroupFilter)
        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            when (checkedIds.firstOrNull()) {
                R.id.chipToday -> {
                    filterDays = 0
                    updateReport()
                }
                R.id.chipLast7Days -> {
                    filterDays = 7
                    updateReport()
                }
            }
        }

        loadUsageData()
        loadActiveSessions()
    }

    private fun loadUsageData() {
        val userId = auth.currentUser?.uid ?: return
        val reportsRef = database.getReference("users").child(userId).child("usage_reports")

        reportsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allUsageRecords.clear()
                for (child in snapshot.children) {
                    val record = child.getValue(UsageRecord::class.java)
                    if (record != null) {
                        allUsageRecords.add(record)
                    }
                }
                updateReport()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadActiveSessions() {
        val userId = auth.currentUser?.uid ?: return
        val activeRef = database.getReference("users").child(userId).child("active_sessions")

        activeRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                activeSessions.clear()
                for (child in snapshot.children) {
                    val startedAt = child.child("startedAt").getValue(Long::class.java) ?: 0
                    val name = child.child("deviceName").getValue(String::class.java) ?: ""
                    val type = child.child("deviceType").getValue(String::class.java) ?: ""
                    val id = child.key ?: ""
                    
                    if (startedAt > 0) {
                        val now = System.currentTimeMillis()
                        val duration = (now - startedAt) / 1000
                        activeSessions.add(UsageRecord(
                            deviceId = id,
                            deviceName = name,
                            deviceType = type,
                            startedAt = startedAt,
                            endedAt = 0,
                            durationSeconds = duration,
                            reason = "ACTIVE"
                        ))
                    }
                }
                updateReport()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updateReport() {
        val calendar = Calendar.getInstance()
        if (filterDays > 0) {
            calendar.add(Calendar.DAY_OF_YEAR, -filterDays)
        }
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis

        var filteredRecords = allUsageRecords.filter { it.startedAt >= startTime }
        
        // Only include active sessions if their start time matches the filter
        var active = activeSessions.filter { it.startedAt >= startTime }
        
        if (targetDeviceId != null) {
            filteredRecords = filteredRecords.filter { it.deviceId == targetDeviceId }
            active = active.filter { it.deviceId == targetDeviceId }
        }
        
        val combined = (filteredRecords + active).sortedByDescending { it.startedAt }

        if (combined.isEmpty()) {
            tvEmptyState.visibility = View.VISIBLE
            tvTotalUsage.text = "0h 0m"
            tvMostUsed.text = "None"
            usageChart.data = emptyList()
            rvDeviceUsage.adapter = null
            rvRecentSessions.adapter = null
            return
        }

        tvEmptyState.visibility = View.GONE

        // Calculate Totals
        val totalSeconds = combined.sumOf { it.durationSeconds }
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        tvTotalUsage.text = String.format(Locale.getDefault(), "%dh %dm", hours, minutes)

        // Usage by Device
        val deviceGroups = combined.groupBy { it.deviceId }
        val deviceUsageList = deviceGroups.map { (_, records) ->
            val totalSec = records.sumOf { it.durationSeconds }
            val name = records.first().deviceName
            val type = records.first().deviceType
            Triple(name, totalSec, type)
        }.sortedByDescending { it.second }

        if (deviceUsageList.isNotEmpty()) {
            tvMostUsed.text = deviceUsageList.first().first
        }

        // Prepare Chart Data (Top 5 devices)
        val chartData = deviceUsageList.take(5).map { 
            Pair(it.first, it.second.toFloat() / 3600f) 
        }
        usageChart.data = chartData

        if (targetDeviceId != null) {
            // For single device, show 24h breakdown or just hide the list
            rvDeviceUsage.visibility = View.GONE
        } else {
            rvDeviceUsage.visibility = View.VISIBLE
            val deviceActivityItems = deviceUsageList.map { 
                val h = it.second / 3600
                val m = (it.second % 3600) / 60
                ActivityItem(
                    iconRes = getIconForType(it.third),
                    text = it.first,
                    time = String.format(Locale.getDefault(), "%dh %dm total", h, m)
                )
            }
            rvDeviceUsage.adapter = ActivityAdapter(deviceActivityItems)
        }

        // Recent Sessions List
        val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
        val recentItems = combined.take(10).map {
            val h = it.durationSeconds / 3600
            val m = (it.durationSeconds % 3600) / 60
            val timeStr = if (h > 0) String.format(Locale.getDefault(), "%dh %dm", h, m) else String.format(Locale.getDefault(), "%dm", m)
            val suffix = if (it.reason == "ACTIVE") " (Still ON)" else ""
            ActivityItem(
                iconRes = getIconForType(it.deviceType),
                text = "${it.deviceName} ($timeStr)$suffix",
                time = sdf.format(Date(it.startedAt))
            )
        }
        rvRecentSessions.adapter = ActivityAdapter(recentItems)
    }

    private fun getIconForType(type: String): Int {
        return when (type.lowercase()) {
            "light", "lamp" -> R.drawable.ic_lamp
            "iron" -> R.drawable.ic_iron
            "fan", "ceiling_fan" -> R.drawable.ic_fan
            "plug", "outlet" -> R.drawable.ic_plug
            else -> R.drawable.ic_reports
        }
    }
}
