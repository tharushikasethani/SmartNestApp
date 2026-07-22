package com.example.smartnest

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.example.smartnest.ui.theme.SmartNestTheme
import com.example.smartnestapp.screens.SplashScreen
import com.example.smartnest.screens.LoginScreen
import com.google.firebase.database.FirebaseDatabase
import com.example.smartnest.screens.DashboardScreen
import com.example.smartnest.screens.RegisterScreen


class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        // Firebase connection test
        val database = FirebaseDatabase.getInstance()

        val reference = database.getReference("test")


        reference.setValue("Firebase Connected Successfully")
            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "Firebase Connected",
                    Toast.LENGTH_SHORT
                ).show()

            }
            .addOnFailureListener {

                Toast.makeText(
                    this,
                    "Firebase Connection Failed",
                    Toast.LENGTH_SHORT
                ).show()

            }



        enableEdgeToEdge()


        setContent {


            SmartNestTheme {


                var currentScreen by remember {

                    mutableStateOf("splash")

                }



                when(currentScreen){


                    "splash" -> {


                        SplashScreen(

                            onNavigateToLogin = {

                                currentScreen = "login"

                            }

                        )


                    }



                    "login" -> {


                        LoginScreen(

                            onLoginSuccess = {


                                currentScreen = "dashboard"


                            },


                            navigateToRegister = {


                                currentScreen = "register"


                            }

                        )


                    }
                    "dashboard" -> {


                        DashboardScreen()


                    }

                    "register" -> {


                        RegisterScreen(

                            navigateToLogin = {

                                currentScreen = "login"

                            }

                        )


                    }


                }


            }

        }

    }

}