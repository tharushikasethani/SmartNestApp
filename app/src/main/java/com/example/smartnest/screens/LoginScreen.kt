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



@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    navigateToRegister: () -> Unit
) {


    var email by remember {
        mutableStateOf("")
    }


    var password by remember {
        mutableStateOf("")
    }


    val context = LocalContext.current


    val auth = FirebaseAuth.getInstance()



    Column(

        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),

        horizontalAlignment = Alignment.CenterHorizontally,

        verticalArrangement = Arrangement.Center

    ) {


        Text(
            text = "SmartNest Login",
            style = MaterialTheme.typography.headlineMedium
        )



        Spacer(
            modifier = Modifier.height(30.dp)
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
            modifier = Modifier.height(15.dp)
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
            modifier = Modifier.height(25.dp)
        )



        Button(

            onClick = {


                if(email.isEmpty() || password.isEmpty()){


                    Toast.makeText(
                        context,
                        "Enter email and password",
                        Toast.LENGTH_SHORT
                    ).show()


                }
                else{


                    auth.signInWithEmailAndPassword(
                        email,
                        password
                    )
                        .addOnSuccessListener {


                            Toast.makeText(
                                context,
                                "Login Successful",
                                Toast.LENGTH_SHORT
                            ).show()


                            onLoginSuccess()


                        }
                        .addOnFailureListener {


                            Toast.makeText(
                                context,
                                "Login Failed",
                                Toast.LENGTH_SHORT
                            ).show()


                        }


                }



            },


            modifier = Modifier.fillMaxWidth()

        ){

            Text("Login")

        }



        Spacer(
            modifier = Modifier.height(15.dp)
        )



        TextButton(

            onClick = {

                navigateToRegister()

            }

        ){

            Text(
                "Don't have an account? Register"
            )

        }


    }

}