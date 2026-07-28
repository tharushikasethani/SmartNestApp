package com.example.smartnest

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.smartnest.activities.LoginActivity

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Open Login Screen
        val intent = Intent(
            this,
            LoginActivity::class.java
        )

        startActivity(intent)

        // Close MainActivity
        finish()
    }
}