package com.example.smartnest

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.smartnest.ui.theme.SmartNestTheme

import com.google.firebase.database.FirebaseDatabase


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

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->

                    Greeting(
                        name = "Smart Nest",
                        modifier = Modifier.padding(innerPadding)
                    )

                }

            }

        }
    }
}


@Composable
fun Greeting(
    name: String,
    modifier: Modifier = Modifier
) {

    Text(
        text = "Hello $name!",
        modifier = modifier
    )

}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {

    SmartNestTheme {

        Greeting("Smart Nest")

    }

}