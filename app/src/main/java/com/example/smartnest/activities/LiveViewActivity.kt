package com.example.smartnest.activities

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.smartnest.R

class LiveViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_view)

        val deviceName = intent.getStringExtra("device_name") ?: "Camera"
        findViewById<TextView>(R.id.tvLiveTitle).text = "$deviceName — Live View"
        findViewById<FrameLayout>(R.id.btnBack).setOnClickListener { finish() }

        val tvStatus = findViewById<TextView>(R.id.tvLiveStatus)
        Handler(Looper.getMainLooper()).postDelayed({
            tvStatus.text = "Connecting..."
            tvStatus.setTextColor(0xFF8B8B8D.toInt())
        }, 500)
    }
}
