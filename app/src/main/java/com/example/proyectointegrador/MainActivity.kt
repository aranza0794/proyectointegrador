package com.example.proyectointegrador

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    // 1. Declaramos la variable de Firebase Auth
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 2. Inicializamos Firebase
        auth = FirebaseAuth.getInstance()

        // 3. Referenciamos los elementos del XML que pegaste
        val correo = findViewById<EditText>(R.id.loginCorreo)
        val pass = findViewById<EditText>(R.id.loginPass)
        val btnIngresar = findViewById<Button>(R.id.btnIngresar)
        val txtRegistro = findViewById<TextView>(R.id.txtIrARegistro)

        // 4. Programamos el botón de Ingresar
        btnIngresar.setOnClickListener {
            val email = correo.text.toString()
            val password = pass.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                // Función mágica de Firebase para loguear
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "¡Bienvenido!", Toast.LENGTH_SHORT).show()
                            // Aquí más adelante mandaremos al usuario a su pantalla (Dueño o Paseador)
                        } else {
                            Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Por favor, llena todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        // 5. Programamos el texto para ir a la pantalla de Registro (cuando la tengamos)
        txtRegistro.setOnClickListener {
            // Esto es como un "boleto" para viajar de una pantalla a otra
            val intent = Intent(this, RegistroActivity::class.java)
            startActivity(intent)
        }
    }
}