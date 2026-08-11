package com.example.smartnest.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.smartnest.databinding.ActivityRegisterBinding
import com.example.smartnest.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase


class RegisterActivity : AppCompatActivity() {


    private lateinit var binding: ActivityRegisterBinding

    private lateinit var auth: FirebaseAuth

    private val database =
        FirebaseDatabase.getInstance()



    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        // Make activity edge-to-edge to remove the bottom navigation bar background
        window.apply {
            clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = android.graphics.Color.TRANSPARENT
            navigationBarColor = android.graphics.Color.TRANSPARENT
            decorView.systemUiVisibility =
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        binding =
            ActivityRegisterBinding.inflate(layoutInflater)


        setContentView(binding.root)



        auth = FirebaseAuth.getInstance()



        binding.btnBack.setOnClickListener {

            finish()

        }



        binding.btnRegister.setOnClickListener {


            val name =
                binding.etName.text.toString().trim()


            val email =
                binding.etEmail.text.toString().trim()


            val password =
                binding.etPassword.text.toString()


            val confirm =
                binding.etConfirmPassword.text.toString()



            when {


                name.isEmpty() ||
                        email.isEmpty() ||
                        password.isEmpty() -> {


                    Toast.makeText(
                        this,
                        "Please fill all fields",
                        Toast.LENGTH_SHORT
                    ).show()


                }



                password != confirm -> {


                    Toast.makeText(
                        this,
                        "Passwords do not match",
                        Toast.LENGTH_SHORT
                    ).show()


                }



                else -> {


                    auth.createUserWithEmailAndPassword(
                        email,
                        password
                    )

                        .addOnSuccessListener {


                            val uid =
                                auth.currentUser!!.uid



                            val user =
                                User(
                                    name = name,
                                    email = email
                                )



                            database
                                .getReference("users")
                                .child(uid)
                                .setValue(user)



                            Toast.makeText(
                                this,
                                "Registration successful",
                                Toast.LENGTH_SHORT
                            ).show()



                            startActivity(
                                Intent(
                                    this,
                                    LoginActivity::class.java
                                )
                            )


                            finish()


                        }



                        .addOnFailureListener {


                            Toast.makeText(
                                this,
                                "Registration failed: ${it.message}",
                                Toast.LENGTH_SHORT
                            ).show()


                        }

                }

            }

        }



        binding.tvGoLogin.setOnClickListener {

            finish()

        }


    }

}