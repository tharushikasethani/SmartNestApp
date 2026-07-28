package com.example.smartnest.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.example.smartnest.R
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<android.widget.ImageView>(R.id.ivLogo)
        val appName = findViewById<android.widget.TextView>(R.id.tvAppName)
        val tagline = findViewById<android.widget.TextView>(R.id.tvTagline)

        val anim = AnimationUtils.loadAnimation(this, R.anim.fade_scale_in)
        logo.startAnimation(anim)
        appName.startAnimation(anim)
        tagline.startAnimation(anim)

        Handler(Looper.getMainLooper()).postDelayed({
            // Skip Login if a user session already exists
            val destination = if (FirebaseAuth.getInstance().currentUser != null) {
                DashboardActivity::class.java
            } else {
                LoginActivity::class.java
            }
            startActivity(Intent(this, destination))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 1800) // total splash duration
    }
}