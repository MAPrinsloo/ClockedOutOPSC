package com.example.clockedout

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast


class Login : AppCompatActivity() {

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        usernameEditText = findViewById(R.id.etUsername)
        passwordEditText = findViewById(R.id.etPassword)
        loginButton = findViewById(R.id.btnLogin)

        //----------------------------------------------------------------------------------------//
        //Login button
        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()

            // Perform login validation here (e.g., check credentials)
            if ((username == "TEST") && password == "PWORD")
            {
                // If the login is successful, navigate to the hub activity
                val intent = Intent(this, Hub::class.java)
                startActivity(intent)
            }
            else
            {
                Toast.makeText(this, "Username or password is incorrect", Toast.LENGTH_SHORT).show()
                Toast.makeText(this, "Test account\r\n" +
                                                 "Username: TEST\r\n" +
                                                 "Password: PWORD", Toast.LENGTH_LONG).show()
            }
        }
    }
}
