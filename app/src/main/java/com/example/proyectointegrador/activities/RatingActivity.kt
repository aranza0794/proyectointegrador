package com.example.proyectointegrador.activities

import android.content.Intent
import android.os.Bundle
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectointegrador.R
import com.example.proyectointegrador.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

class RatingActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var session: SessionManager
    private var walkId = ""
    private var walkerId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rating)

        session = SessionManager(this)
        walkId = intent.getStringExtra("walk_id") ?: ""
        walkerId = intent.getStringExtra("walker_id") ?: ""

        val ratingBar = findViewById<RatingBar>(R.id.ratingBar)
        val etComment = findViewById<TextInputEditText>(R.id.etComment)
        val btnSubmit = findViewById<MaterialButton>(R.id.btnSubmitRating)
        val btnSkip = findViewById<MaterialButton>(R.id.btnSkip)

        btnSubmit.setOnClickListener {
            val stars = ratingBar.rating.toInt()
            if (stars == 0) {
                Toast.makeText(this, "Selecciona al menos 1 estrella", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val rating = hashMapOf(
                "walkRequestId" to walkId,
                "walkerId" to walkerId,
                "ownerName" to session.getUserName(),
                "stars" to stars,
                "comment" to etComment.text.toString().trim()
            )

            // Guardar calificación
            db.collection("calificaciones")
                .add(rating)
                .addOnSuccessListener {
                    // Actualizar promedio del paseador
                    updateWalkerRating(walkerId, stars)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al guardar calificación", Toast.LENGTH_SHORT).show()
                }
        }

        btnSkip.setOnClickListener { goToDashboard() }
    }

    private fun updateWalkerRating(walkerId: String, newStars: Int) {
        val walkerRef = db.collection("usuarios").document(walkerId)

        db.runTransaction { transaction ->
            val walkerDoc = transaction.get(walkerRef)
            val currentRating = walkerDoc.getDouble("rating") ?: 0.0
            val currentCount = walkerDoc.getLong("ratingCount") ?: 0L
            val newCount = currentCount + 1
            val newRating = ((currentRating * currentCount) + newStars) / newCount

            transaction.update(walkerRef, "rating", newRating)
            transaction.update(walkerRef, "ratingCount", newCount)
        }.addOnSuccessListener {
            Toast.makeText(this, "¡Gracias por tu calificación!", Toast.LENGTH_SHORT).show()
            goToDashboard()
        }
    }

    private fun goToDashboard() {
        startActivity(Intent(this, OwnerDashboardActivity::class.java))
        finish()
    }
}