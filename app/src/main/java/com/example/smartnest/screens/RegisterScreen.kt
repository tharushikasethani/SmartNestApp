package com.example.smartnest.screens


import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.example.smartnest.model.User



@Composable
fun RegisterScreen(

    navigateToLogin: () -> Unit

) {


    var name by remember {
        mutableStateOf("")
    }


    var email by remember {
        mutableStateOf("")
    }


    var password by remember {
        mutableStateOf("")
    }


    val context = LocalContext.current


    val auth = FirebaseAuth.getInstance()


    val database = FirebaseDatabase.getInstance()



    Column(

        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),

        horizontalAlignment = Alignment.CenterHorizontally,

        verticalArrangement = Arrangement.Center

    ) {


        Text(

            text = "Create Account",

            style = MaterialTheme.typography.headlineMedium

        )


        Spacer(
            modifier = Modifier.height(20.dp)
        )



        OutlinedTextField(

            value = name,

            onValueChange = {
                name = it
            },

            label = {
                Text("Name")
            },

            modifier = Modifier.fillMaxWidth()

        )



        Spacer(
            modifier = Modifier.height(10.dp)
        )



        OutlinedTextField(

            value = email,

            onValueChange = {
                email = it
            },

            label = {
                Text("Email")
            },

            modifier = Modifier.fillMaxWidth()

        )



        Spacer(
            modifier = Modifier.height(10.dp)
        )



        OutlinedTextField(

            value = password,

            onValueChange = {
                password = it
            },

            label = {
                Text("Password")
            },

            visualTransformation = PasswordVisualTransformation(),

            modifier = Modifier.fillMaxWidth()

        )



        Spacer(
            modifier = Modifier.height(20.dp)
        )



        Button(

            onClick = {


                auth.createUserWithEmailAndPassword(

                    email,

                    password

                )
                    .addOnSuccessListener {


                        val uid = auth.currentUser!!.uid



                        val user = User(

                            name = name,

                            email = email,

                        )



                        database
                            .getReference("users")
                            .child(uid)
                            .setValue(user)



                        Toast.makeText(

                            context,

                            "Registration Successful",

                            Toast.LENGTH_SHORT

                        ).show()



                        navigateToLogin()


                    }


                    .addOnFailureListener {


                        Toast.makeText(

                            context,

                            "Registration Failed",

                            Toast.LENGTH_SHORT

                        ).show()


                    }


            },


            modifier = Modifier.fillMaxWidth()

        ){

            Text("Register")

        }


    }

}