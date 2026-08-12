package com.example.smartnest.activities

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.smartnest.R

class LiveViewActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var ivStream: ImageView
    private lateinit var tvStatus: TextView
    private lateinit var tvUri: TextView
    
    private val frames = intArrayOf(
        R.drawable.living_room,
        R.drawable.bedroom,
        R.drawable.porch,
        R.drawable.dining_room
    )
    private var frameIndex = 0
    private var isPlaying = false

    private val streamRunnable = object : Runnable {
        override fun run() {
            if (!isPlaying) return
            // Simulate frame updates every few seconds to look like a live feed
            frameIndex = (frameIndex + 1) % frames.size
            ivStream.setImageResource(frames[frameIndex])
            
            // Add a small delay jitter to look more realistic
            val nextDelay = (2000..5000).random().toLong()
            handler.postDelayed(this, nextDelay)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_view)

        val deviceName = intent.getStringExtra("device_name") ?: "Camera"
        val mockUri = intent.getStringExtra("stream_uri") ?: "rtsp://mock-stream.local/default"
        
        findViewById<TextView>(R.id.tvLiveTitle).text = "$deviceName — LIVE"
        findViewById<FrameLayout>(R.id.btnBack).setOnClickListener { finish() }

        ivStream = findViewById(R.id.ivLiveStream)
        tvStatus = findViewById(R.id.tvLiveStatus)
        tvUri = findViewById(R.id.tvStreamUri)

        tvUri.text = mockUri
        tvUri.visibility = View.VISIBLE

        startStreamSimulation()
    }

    private fun startStreamSimulation() {
        tvStatus.text = "Connecting to stream..."
        tvStatus.setTextColor(0xFF8B8B8D.toInt())
        
        handler.postDelayed({
            tvStatus.text = "● LIVE"
            tvStatus.setTextColor(0xFFFF3B30.toInt()) // Red for Live
            ivStream.visibility = View.VISIBLE
            ivStream.alpha = 1.0f
            ivStream.setImageResource(frames[frameIndex])
            isPlaying = true
            handler.post(streamRunnable)
        }, 1800)
    }

    override fun onDestroy() {
        isPlaying = false
        handler.removeCallbacks(streamRunnable)
        super.onDestroy()
    }
}
