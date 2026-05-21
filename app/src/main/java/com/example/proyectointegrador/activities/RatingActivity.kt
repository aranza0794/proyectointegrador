package com.example.proyectointegrador.activities

import android.content.Intent
import android.os.Bundle
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.proyectointegrador.R
import com.example.proyectointegrador.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore

class RatingActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var session: SessionManager
    private var walkId   = ""
    private var walkerId = ""
    private var ownerId  = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rating)

        session  = SessionManager(this)
        walkId   = intent.getStringExtra("walk_id") ?: ""
        walkerId = intent.getStringExtra("walker_id") ?: ""
        ownerId  = intent.getStringExtra("owner_id") ?: ""

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Calificar paseo"
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { goToDashboard() }
        })

        val tvRatingLabel = findViewById<TextView>(R.id.tvRatingLabel)
        val ratingBar     = findViewById<RatingBar>(R.id.ratingBar)
        val etComment     = findViewById<TextInputEditText>(R.id.etComment)
        val btnSubmit     = findViewById<MaterialButton>(R.id.btnSubmitRating)
        val btnSkip       = findViewById<MaterialButton>(R.id.btnSkip)

        val userType = session.getUserType()

        // FIX: Texto según quién califica a quién
        if (userType == "owner") {
            if (walkerId.isNotEmpty()) {
                db.collection("usuarios").document(walkerId).get()
                    .addOnSuccessListener { doc ->
                        tvRatingLabel.text =
                            "¿Cómo fue el servicio de ${doc.getString("name") ?: "el paseador"}?"
                    }
            }
        } else {
            // FIX: Paseador califica al dueño
            if (ownerId.isNotEmpty()) {
                db.collection("usuarios").document(ownerId).get()
                    .addOnSuccessListener { doc ->
                        tvRatingLabel.text =
                            "¿Cómo fue el dueño ${doc.getString("name") ?: ""}?"
                    }
            }
        }

        // Verificar si ya calificó
        val ratingField = if (userType == "owner") "walkerId" else "ownerId"
        val ratingValue = if (userType == "owner") walkerId else ownerId

        db.collection("calificaciones")
            .whereEqualTo("walkRequestId", walkId)
            .whereEqualTo("ratedBy", session.getUserId())
            .get()
            .addOnSuccessListener { docs ->
                if (!docs.isEmpty) {
                    val stars = docs.first().getLong("stars") ?: 0L
                    ratingBar.rating    = stars.toFloat()
                    ratingBar.isEnabled = false
                    etComment.isEnabled = false
                    btnSubmit.isEnabled = false
                    btnSubmit.text      = "Ya calificaste este paseo"
                    tvRatingLabel.text  = "✓ Calificación enviada"
                    return@addOnSuccessListener
                }

                ratingBar.setOnRatingBarChangeListener { _, rating, _ ->
                    tvRatingLabel.text = when (rating.toInt()) {
                        1 -> "😞 Muy malo"
                        2 -> "😐 Regular"
                        3 -> "🙂 Bueno"
                        4 -> "😊 Muy bueno"
                        5 -> "🤩 ¡Excelente!"
                        else -> "Toca las estrellas"
                    }
                }

                btnSubmit.setOnClickListener {
                    val stars   = ratingBar.rating.toInt()
                    val comment = etComment.text.toString().trim()

                    if (stars == 0) {
                        Toast.makeText(this,
                            "Selecciona al menos 1 estrella",
                            Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    btnSubmit.isEnabled = false
                    btnSubmit.text      = "Enviando..."

                    // FIX: Guardar quién califica a quién
                    val ratedUserId = if (userType == "owner") walkerId else ownerId
                    val rating = hashMapOf(
                        "walkRequestId" to walkId,
                        "ratedUserId"   to ratedUserId,
                        "ratedBy"       to session.getUserId(),
                        "raterType"     to userType,
                        "ownerName"     to session.getUserName(),
                        "stars"         to stars,
                        "comment"       to comment
                    )

                    // Para compatibilidad con el perfil del paseador
                    if (userType == "owner") {
                        rating["walkerId"] = walkerId
                    }

                    db.collection("calificaciones")
                        .add(rating)
                        .addOnSuccessListener {
                            db.collection("solicitudes").document(walkId)
                                .update("ratingStars", stars)
                            updateUserRating(ratedUserId, stars)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this,
                                "Sin conexión. Intenta de nuevo.",
                                Toast.LENGTH_LONG).show()
                            btnSubmit.isEnabled = true
                            btnSubmit.text      = "Enviar calificación"
                        }
                }
            }

        btnSkip.setOnClickListener { goToDashboard() }
    }

    private fun updateUserRating(userId: String, newStars: Int) {
        val userRef = db.collection("usuarios").document(userId)
        db.runTransaction { transaction ->
            val doc           = transaction.get(userRef)
            val currentRating = doc.getDouble("rating") ?: 0.0
            val currentCount  = doc.getLong("ratingCount") ?: 0L
            val newCount      = currentCount + 1
            val newRating     = ((currentRating * currentCount) + newStars) / newCount
            transaction.update(userRef, "rating", newRating)
            transaction.update(userRef, "ratingCount", newCount)
        }.addOnSuccessListener {
            Toast.makeText(this, "¡Gracias por tu calificación!", Toast.LENGTH_SHORT).show()
            goToDashboard()
        }.addOnFailureListener {
            goToDashboard()
        }
    }

    // FIX: Después de calificar va al dashboard principal, no al historial
    private fun goToDashboard() {
        val dest = if (session.getUserType() == "owner")
            OwnerDashboardActivity::class.java
        else
            WalkerDashboardActivity::class.java

        val intent = Intent(this, dest)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }
}