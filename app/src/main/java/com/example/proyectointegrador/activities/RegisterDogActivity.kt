package com.example.proyectointegrador.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

        val etDogName = findViewById<TextInputEditText>(R.id.etDogName)
        val etBreed = findViewById<TextInputEditText>(R.id.etBreed)
        val etAge = findViewById<TextInputEditText>(R.id.etAge)
        val etAllergy = findViewById<TextInputEditText>(R.id.etAllergy)
        val chipGroupSize = findViewById<ChipGroup>(R.id.chipGroupSize)
        val btnSaveDog = findViewById<MaterialButton>(R.id.btnSaveDog)

        btnSaveDog.setOnClickListener {
            val dogName = etDogName.text.toString().trim()
            val breed = etBreed.text.toString().trim()
            val age = etAge.text.toString().trim()
            val allergy = etAllergy.text.toString().trim()

            // Obtener tamaño del chip seleccionado
            val selectedChipId = chipGroupSize.checkedChipId
            val size = when (selectedChipId) {
                R.id.chipSmall -> "Pequeño"
                R.id.chipMedium -> "Mediano"
                R.id.chipLarge -> "Grande"
                else -> ""
            }

            if (dogName.isEmpty() || breed.isEmpty() || age.isEmpty() || size.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newDog = hashMapOf(
                "ownerId" to session.getUserId(),
                "name" to dogName,
                "breed" to breed,
                "size" to size,
                "age" to age.toInt(),
                "allergy" to allergy
            )

            db.collection("perros")
                .add(newDog)
                .addOnSuccessListener {
                    Toast.makeText(this, "Perro registrado", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, OwnerDashboardActivity::class.java))
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show()
                }
        }
    }
}