package com.example.proyectointegrador

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegistroActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val etNombre = findViewById<EditText>(R.id.regNombre)
        val etCorreo = findViewById<EditText>(R.id.regCorreo)
        val etPass = findViewById<EditText>(R.id.regPass)
        val etTarjeta = findViewById<EditText>(R.id.regTarjeta)
        val rgRol = findViewById<RadioGroup>(R.id.radioGroupRol)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrarFinal)

        rgRol.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbPaseador) {
                etTarjeta.visibility = View.VISIBLE
            } else {
                etTarjeta.visibility = View.GONE
                etTarjeta.setText("")
            }
        }

        btnRegistrar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val correo = etCorreo.text.toString().trim()
            val pass = etPass.text.toString().trim()
            val tarjeta = etTarjeta.text.toString().trim()

            // 1. Validar que se haya seleccionado un Rol para evitar cierres (Crash)
            val selectedRolId = rgRol.checkedRadioButtonId
            if (selectedRolId == -1) {
                Toast.makeText(this, "Por favor selecciona un rol", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val rbSeleccionado = findViewById<RadioButton>(selectedRolId)
            val rolSeleccionado = rbSeleccionado.text.toString()

            // 2. Validaciones básicas
            if (nombre.isEmpty() || correo.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Completa los campos obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass.length < 6) {
                Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 3. Proceso de Registro en Firebase
            auth.createUserWithEmailAndPassword(correo, pass)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = auth.currentUser?.uid ?: ""

                        // Usamos tu Data Class Usuario
                        val nuevoUsuario = Usuario(
                            nombre = nombre,
                            correo = correo,
                            rol = rolSeleccionado,
                            tarjeta = tarjeta
                        )

                        // GUARDAR EN FIRESTORE
                        // Es vital usar .document(uid) para que el ID sea el mismo que el de Auth
                        db.collection("Usuarios").document(uid).set(nuevoUsuario)
                            .addOnSuccessListener {
                                Toast.makeText(this, "¡Registro exitoso!", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error al guardar en BD: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    } else {
                        Toast.makeText(this, "Error de Auth: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
}