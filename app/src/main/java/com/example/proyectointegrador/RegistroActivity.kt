package com.example.proyectointegrador


import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegistroActivity : AppCompatActivity() {

    // Declaramos las herramientas de Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        // Inicializamos Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Referencias a los componentes del XML
        val etNombre = findViewById<EditText>(R.id.regNombre)
        val etCorreo = findViewById<EditText>(R.id.regCorreo)
        val etPass = findViewById<EditText>(R.id.regPass)
        val etTarjeta = findViewById<EditText>(R.id.regTarjeta)
        val rgRol = findViewById<RadioGroup>(R.id.radioGroupRol)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrarFinal)

        // Lógica para mostrar/ocultar el campo de tarjeta según el rol
        rgRol.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbPaseador) {
                etTarjeta.visibility = View.VISIBLE
            } else {
                etTarjeta.visibility = View.GONE
                etTarjeta.setText("") // Limpiamos si cambia a Dueño
            }
        }

        btnRegistrar.setOnClickListener {
            val nombre = etNombre.text.toString()
            val correo = etCorreo.text.toString()
            val pass = etPass.text.toString()
            val tarjeta = etTarjeta.text.toString()

            // Obtenemos qué RadioButton está marcado
            val selectedRolId = rgRol.checkedRadioButtonId
            val rbSeleccionado = findViewById<RadioButton>(selectedRolId)

            val rolSeleccionado = rbSeleccionado.text.toString()

            if (nombre.isNotEmpty() && correo.isNotEmpty() && pass.length >= 6) {


                // ... dentro de btnRegistrar.setOnClickListener ...
                auth.createUserWithEmailAndPassword(correo, pass)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val uid = auth.currentUser?.uid ?: ""

                            // Creamos el objeto usando tu data class Usuario
                            val nuevoUsuario = Usuario(
                                nombre = etNombre.text.toString(),
                                correo = etCorreo.text.toString(),
                                rol = rolSeleccionado,
                                tarjeta = etTarjeta.text.toString()
                            )

                            // GUARDAR EN FIRESTORE
                            db.collection("Usuarios").document(uid).set(nuevoUsuario)
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        this,
                                        "¡Cuenta creada y datos guardados!",
                                        Toast.LENGTH_SHORT).show()
                                    // Ir a la pantalla principal o Login
                                    startActivity(Intent(this, MainActivity::class.java))
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        this,
                                        "Error al guardar datos: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        } else {
                            Toast.makeText(
                                this,
                                "Error en Auth: ${task.exception?.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
            }
        }
    }
}
