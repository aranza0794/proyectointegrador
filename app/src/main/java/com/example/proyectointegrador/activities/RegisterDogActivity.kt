package com.example.proyectointegrador.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.proyectointegrador.R
import com.example.proyectointegrador.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore

class RegisterDogActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_dog)

        session = SessionManager(this)

        // Toolbar con botón back
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Mi perro"

        val etDogName     = findViewById<TextInputEditText>(R.id.etDogName)
        val etBreed       = findViewById<TextInputEditText>(R.id.etBreed)
        val etAge         = findViewById<TextInputEditText>(R.id.etAge)
        val etAllergy     = findViewById<TextInputEditText>(R.id.etAllergy)
        val chipGroupSize = findViewById<ChipGroup>(R.id.chipGroupSize)
        val btnSaveDog    = findViewById<MaterialButton>(R.id.btnSaveDog)

        // Verificar que la sesión tiene el userId correcto
        val ownerId = session.getUserId()
        Log.d("REGISTER_DOG", "OwnerId de sesión: $ownerId")

        if (ownerId.isEmpty()) {
            Toast.makeText(this,
                "Error de sesión. Vuelve a registrarte.",
                Toast.LENGTH_LONG).show()
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
            return
        }

        btnSaveDog.setOnClickListener {
            val dogName = etDogName.text.toString().trim()
            val breed   = etBreed.text.toString().trim()
            val age     = etAge.text.toString().trim()
            val allergy = etAllergy.text.toString().trim()

            val size = when (chipGroupSize.checkedChipId) {
                R.id.chipSmall  -> "Pequeño"
                R.id.chipMedium -> "Mediano"
                R.id.chipLarge  -> "Grande"
                else -> ""
            }

            if (dogName.isEmpty() || breed.isEmpty() || age.isEmpty() || size.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val ageInt = age.toIntOrNull()
            if (ageInt == null || ageInt <= 0) {
                Toast.makeText(this, "Ingresa una edad válida", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSaveDog.isEnabled = false
            btnSaveDog.text = "Guardando..."

            // FIX: usa session.getUserId() que ahora tiene el ID correcto
            val newDog = hashMapOf(
                "ownerId" to ownerId,   // ← ID correcto del dueño
                "name"    to dogName,
                "breed"   to breed,
                "size"    to size,
                "age"     to ageInt,
                "allergy" to allergy
            )

            Log.d("REGISTER_DOG", "Guardando perro con ownerId: $ownerId")

            db.collection("perros")
                .add(newDog)
                .addOnSuccessListener { docRef ->
                    Log.d("REGISTER_DOG", "Perro guardado: ${docRef.id}")
                    Toast.makeText(this, "¡Perro registrado!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, OwnerDashboardActivity::class.java))
                    finish()
                }
                .addOnFailureListener { exception ->
                    Log.e("REGISTER_DOG", "Error al guardar perro", exception)
                    Toast.makeText(this,
                        "Error al guardar: ${exception.message}",
                        Toast.LENGTH_LONG).show()
                    btnSaveDog.isEnabled = true
                    btnSaveDog.text = "Guardar y continuar"
                }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}