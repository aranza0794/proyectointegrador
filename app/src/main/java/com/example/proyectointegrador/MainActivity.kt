package com.example.proyectointegrador

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    // 1. Declaramos Firebase Auth y Firestore
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 2. Inicializamos las herramientas
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // 3. Referenciamos los elementos del XML
        val correo = findViewById<EditText>(R.id.loginCorreo)
        val pass = findViewById<EditText>(R.id.loginPass)
        val btnIngresar = findViewById<Button>(R.id.btnIngresar)
        val txtRegistro = findViewById<TextView>(R.id.txtIrARegistro)

        // 4. Lógica del botón Ingresar
        btnIngresar.setOnClickListener {
            val email = correo.text.toString()
            val password = pass.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Si el login es correcto, verificamos el ROL
                            verificarRolYRedirigir()
                        } else {
                            Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Por favor, llena todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        // 5. Ir a la pantalla de Registro
        txtRegistro.setOnClickListener {
            val intent = Intent(this, RegistroActivity::class.java)
            startActivity(intent)
        }
    }

    // FUNCIÓN CLAVE: Busca en Firestore si el usuario es Dueño o Paseador
    private fun verificarRolYRedirigir() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("Usuarios").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val rol = document.getString("rol") ?: ""

                    // CORRECCIÓN: Comparamos con la frase exacta que sale en tu Toast
                    if (rol == "Soy Dueño" || rol == "Dueño") {
                        val intent = Intent(this, ActivityDueno::class.java)
                        startActivity(intent)
                        finish()
                    } else if (rol == "Soy Paseador" || rol == "Paseador") {
                        val intent = Intent(this, ActivityPaseador::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Rol no reconocido: $rol", Toast.LENGTH_LONG).show()
                    }
                }
            }
    }
}