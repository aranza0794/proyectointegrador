package com.example.proyectointegrador.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.proyectointegrador.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private var selectedType = "owner"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // ── Toolbar con botón back ──────────────────────────
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Crear cuenta"

        val rgUserType    = findViewById<RadioGroup>(R.id.rgUserType)
        val etName        = findViewById<TextInputEditText>(R.id.etName)
        val etEmail       = findViewById<TextInputEditText>(R.id.etEmail)
        val etPhone       = findViewById<TextInputEditText>(R.id.etPhone)
        val etBirthDate   = findViewById<TextInputEditText>(R.id.etBirthDate)
        val etPassword    = findViewById<TextInputEditText>(R.id.etPassword)
        val tilCardNumber = findViewById<TextInputLayout>(R.id.tilCardNumber)
        val etCardNumber  = findViewById<TextInputEditText>(R.id.etCardNumber)
        val btnRegister   = findViewById<MaterialButton>(R.id.btnRegister)
        val tvGoLogin     = findViewById<TextView>(R.id.tvGoLogin)

        rgUserType.setOnCheckedChangeListener { _, checkedId ->
            selectedType = if (checkedId == R.id.rbWalker) "walker" else "owner"
            tilCardNumber.visibility =
                if (selectedType == "walker") View.VISIBLE else View.GONE
        }

        btnRegister.setOnClickListener {
            val name      = etName.text.toString().trim()
            val email     = etEmail.text.toString().trim()
            val phone     = etPhone.text.toString().trim()
            val birthDate = etBirthDate.text.toString().trim()
            val password  = etPassword.text.toString().trim()
            val cardNumber = etCardNumber.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || phone.isEmpty() ||
                birthDate.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Ingresa un correo válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres",
                    Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedType == "walker" && cardNumber.isEmpty()) {
                Toast.makeText(this, "Ingresa tu número de tarjeta", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnRegister.isEnabled = false
            btnRegister.text = "Registrando..."

            db.collection("usuarios")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        Toast.makeText(this, "Este correo ya está registrado",
                            Toast.LENGTH_SHORT).show()
                        btnRegister.isEnabled = true
                        btnRegister.text = "Crear cuenta"
                        return@addOnSuccessListener
                    }

                    val newUser = hashMapOf(
                        "name"        to name,
                        "email"       to email,
                        "phone"       to phone,
                        "birthDate"   to birthDate,
                        "password"    to password,
                        "userType"    to selectedType,
                        "cardNumber"  to cardNumber,
                        "isAvailable" to true,
                        "rating"      to 0.0,
                        "ratingCount" to 0
                    )

                    db.collection("usuarios")
                        .add(newUser)
                        .addOnSuccessListener { documentRef ->
                            Log.d("REGISTER", "Usuario creado: ${documentRef.id}")
                            Toast.makeText(this, "¡Registro exitoso!", Toast.LENGTH_SHORT).show()
                            if (selectedType == "owner") {
                                startActivity(Intent(this, RegisterDogActivity::class.java))
                            } else {
                                startActivity(Intent(this, LoginActivity::class.java))
                            }
                            finish()
                        }
                        .addOnFailureListener { exception ->
                            Log.e("REGISTER_ERROR", "Error al crear usuario", exception)
                            Toast.makeText(this,
                                "Error al registrar: ${exception.message}",
                                Toast.LENGTH_LONG).show()
                            btnRegister.isEnabled = true
                            btnRegister.text = "Crear cuenta"
                        }
                }
                .addOnFailureListener { exception ->
                    Log.e("REGISTER_ERROR", "Error al verificar email", exception)
                    Toast.makeText(this,
                        "Error de conexión: ${exception.message}",
                        Toast.LENGTH_LONG).show()
                    btnRegister.isEnabled = true
                    btnRegister.text = "Crear cuenta"
                }
        }

        tvGoLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}