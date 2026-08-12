package com.example.smartnest.activities

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.smartnest.R
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val tvTitle = findViewById<android.view.View>(R.id.tvTitle)
        val btnGetStarted = findViewById<TextView>(R.id.btnGetStarted)
        val btnContainer = findViewById<android.view.View>(R.id.btnGetStartedContainer)

        // Animate the title in
        val anim = AnimationUtils.loadAnimation(this, R.anim.fade_scale_in)
        tvTitle.startAnimation(anim)

        // Determine destination based on login state
        val destination = if (FirebaseAuth.getInstance().currentUser != null) {
            DashboardActivity::class.java
        } else {
            LoginActivity::class.java
        }

        val proceed = {
            startActivity(Intent(this, destination))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }

        btnGetStarted.setOnClickListener { proceed() }
        btnContainer.setOnClickListener { proceed() }
    }
}