package com.example.smartnest.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.smartnest.R
import com.google.firebase.auth.FirebaseAuth

class DashboardActivity : AppCompatActivity() {


    private lateinit var auth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_dashboard)


        auth = FirebaseAuth.getInstance()


        val logoutButton = findViewById<Button>(R.id.btnLogout)


        logoutButton.setOnClickListener {


            // Logout Firebase user
            auth.signOut()


            // Go back to Login screen
            val intent = Intent(
                this,
                LoginActivity::class.java
            )

            startActivity(intent)


            // Remove Dashboard from back stack
            finish()

        }

    }
}