package com.example.proyectointegrador.activities

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.proyectointegrador.R
import com.example.proyectointegrador.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val session    = SessionManager(this)
        val etEmail    = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin   = findViewById<MaterialButton>(R.id.btnLogin)
        val tvRegister = findViewById<TextView>(R.id.tvGoRegister)

        btnLogin.setOnClickListener {
            val email    = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            btnLogin.text      = "Verificando..."

            db.collection("usuarios")
                .whereEqualTo("email", email)
                .whereEqualTo("password", password)
                .get()
                .addOnSuccessListener { docs ->
                    if (docs.isEmpty) {
                        Toast.makeText(this,
                            "Correo o contraseña incorrectos",
                            Toast.LENGTH_SHORT).show()
                        btnLogin.isEnabled = true
                        btnLogin.text      = "🐾  Continuar"
                        return@addOnSuccessListener
                    }

                    val doc      = docs.documents.first()
                    val userId   = doc.id
                    val userType = doc.getString("userType") ?: "owner"
                    val name     = doc.getString("name")     ?: ""

                    session.saveSession(userId, userType, name)

                    val dest = if (userType == "walker")
                        WalkerDashboardActivity::class.java
                    else OwnerDashboardActivity::class.java

                    startActivity(Intent(this, dest).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error de conexión", Toast.LENGTH_SHORT).show()
                    btnLogin.isEnabled = true
                    btnLogin.text      = "🐾  Continuar"
                }
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}