package com.example.smartnest.firebase

import com.google.firebase.database.FirebaseDatabase

object FirebaseManager {


    private val database =
        FirebaseDatabase.getInstance()


    fun testConnection(){

        val reference =
            database.getReference("test")


        reference.setValue(
            "Firebase Connected Successfully"
        )

    }

}