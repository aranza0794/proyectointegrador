package com.example.proyectointegrador.activities

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectointegrador.R
import com.example.proyectointegrador.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        session = SessionManager(this)

        val tilEmail     = findViewById<TextInputLayout>(R.id.tilEmail)
        val tilPassword  = findViewById<TextInputLayout>(R.id.tilPassword)
        val etEmail      = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword   = findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin     = findViewById<MaterialButton>(R.id.btnLogin)
        val tvGoRegister = findViewById<TextView>(R.id.tvGoRegister)

        btnLogin.setOnClickListener {
            val email    = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // FIX: Validación visual con error en cada campo
            var hasError = false

            if (email.isEmpty()) {
                tilEmail.error = "Ingresa tu correo"
                hasError = true
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.error = "Correo inválido"
                hasError = true
            } else {
                tilEmail.error = null
            }

            if (password.isEmpty()) {
                tilPassword.error = "Ingresa tu contraseña"
                hasError = true
            } else if (password.length < 6) {
                tilPassword.error = "Mínimo 6 caracteres"
                hasError = true
            } else {
                tilPassword.error = null
            }

            if (hasError) return@setOnClickListener

            btnLogin.isEnabled = false
            btnLogin.text      = "Entrando..."

            db.collection("usuarios")
                .whereEqualTo("email", email)
                .whereEqualTo("password", password)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val doc      = documents.first()
                        val userType = doc.getString("userType") ?: ""
                        val userName = doc.getString("name") ?: ""
                        val userId   = doc.id

                        session.saveSession(userId, userType, userName)

                        val dest = if (userType == "owner")
                            OwnerDashboardActivity::class.java
                        else
                            WalkerDashboardActivity::class.java

                        startActivity(Intent(this, dest))
                        finish()
                    } else {
                        tilEmail.error    = null
                        tilPassword.error = "Correo o contraseña incorrectos"
                        btnLogin.isEnabled = true
                        btnLogin.text      = "Iniciar sesión"
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this,
                        "Error de conexión: ${e.message}",
                        Toast.LENGTH_LONG).show()
                    btnLogin.isEnabled = true
                    btnLogin.text      = "Iniciar sesión"
                }
        }

        tvGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}