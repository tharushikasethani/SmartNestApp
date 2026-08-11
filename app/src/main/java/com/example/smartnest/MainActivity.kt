package com.example.smartnest

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.smartnest.activities.SplashActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Open Splash Screen first
        val intent = Intent(
            this,
            SplashActivity::class.java
        )

        startActivity(intent)

        // Close MainActivity
        finish()
    }
}