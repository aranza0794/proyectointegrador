package com.example.proyectointegrador.activities

import android.content.Intent
import android.os.Bundle
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
    private var walkId        = ""
    private var walkerId      = ""
    private var ownerId       = ""
    private var selectedStars = 0
    private lateinit var paws: List<TextView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rating)

        session  = SessionManager(this)
        walkId   = intent.getStringExtra("walk_id")   ?: ""
        walkerId = intent.getStringExtra("walker_id") ?: ""
        ownerId  = intent.getStringExtra("owner_id")  ?: ""

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Calificar paseo"
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { goToDashboard() }
        })

        val tvRatingLabel = findViewById<TextView>(R.id.tvRatingLabel)
        val etComment     = findViewById<TextInputEditText>(R.id.etComment)
        val btnSubmit     = findViewById<MaterialButton>(R.id.btnSubmitRating)
        val btnSkip       = findViewById<MaterialButton>(R.id.btnSkip)

        paws = listOf(
            findViewById(R.id.paw1),
            findViewById(R.id.paw2),
            findViewById(R.id.paw3),
            findViewById(R.id.paw4),
            findViewById(R.id.paw5)
        )

        val userType = session.getUserType()

        // Si no tenemos walkId, ir al dashboard
        if (walkId.isEmpty()) {
            goToDashboard()
            return
        }

        // Si es dueño, califica al paseador. Si es paseador, califica al dueño
        val ratedId = if (userType == "owner") walkerId else ownerId

        // Cargar nombre de quien se califica
        if (ratedId.isNotEmpty()) {
            db.collection("usuarios").document(ratedId).get()
                .addOnSuccessListener { doc ->
                    val name = doc.getString("name") ?: ""
                    tvRatingLabel.text = if (userType == "owner")
                        "¿Cómo fue el servicio de $name? 🐾"
                    else "¿Cómo fue el dueño $name? 🐶"
                }
        }

        // Verificar si ya calificó este paseo
        db.collection("calificaciones")
            .whereEqualTo("walkRequestId", walkId)
            .whereEqualTo("ratedBy", session.getUserId())
            .get()
            .addOnSuccessListener { docs ->
                if (!docs.isEmpty) {
                    // Ya calificó
                    updatePaws((docs.first().getLong("stars") ?: 0L).toInt())
                    paws.forEach { it.isEnabled = false }
                    etComment.isEnabled = false
                    btnSubmit.isEnabled = false
                    btnSubmit.text      = "✓ Ya calificaste este paseo"
                    tvRatingLabel.text  = "✓ Calificación enviada"
                    return@addOnSuccessListener
                }

                // Configurar huellas clickeables
                paws.forEachIndexed { index, paw ->
                    paw.setOnClickListener {
                        selectedStars = index + 1
                        updatePaws(selectedStars)
                        tvRatingLabel.text = when (selectedStars) {
                            1 -> "😞 Muy malo"
                            2 -> "😐 Regular"
                            3 -> "🙂 Bueno"
                            4 -> "😊 Muy bueno"
                            5 -> "🤩 ¡Excelente!"
                            else -> "Toca las huellas para calificar"
                        }
                    }
                }

                btnSubmit.setOnClickListener {
                    if (selectedStars == 0) {
                        Toast.makeText(this,
                            "Toca las huellas para calificar 🐾",
                            Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    btnSubmit.isEnabled = false
                    btnSubmit.text      = "Enviando..."

                    val comment     = etComment.text.toString().trim()
                    val ratedUserId = if (userType == "owner") walkerId else ownerId

                    val calificacion = hashMapOf<String, Any>(
                        "walkRequestId" to walkId,
                        "ratedUserId"   to ratedUserId,
                        "ratedBy"       to session.getUserId(),
                        "raterType"     to userType,
                        "ownerName"     to session.getUserName(),
                        "stars"         to selectedStars,
                        "comment"       to comment
                    )
                    if (userType == "owner") calificacion["walkerId"] = walkerId

                    db.collection("calificaciones").add(calificacion)
                        .addOnSuccessListener {
                            db.collection("solicitudes").document(walkId)
                                .update("ratingStars", selectedStars)
                            updateUserRating(ratedUserId, selectedStars)
                        }
                        .addOnFailureListener {
                            Toast.makeText(this,
                                "Error al enviar. Intenta de nuevo.",
                                Toast.LENGTH_SHORT).show()
                            btnSubmit.isEnabled = true
                            btnSubmit.text      = "🐾  Enviar calificación"
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error de conexión", Toast.LENGTH_SHORT).show()
            }

        btnSkip.setOnClickListener { goToDashboard() }
    }

    private fun updatePaws(stars: Int) {
        paws.forEachIndexed { index, paw ->
            paw.alpha = if (index < stars) 1f else 0.25f
        }
    }

    private fun updateUserRating(userId: String, newStars: Int) {
        if (userId.isEmpty()) { goToDashboard(); return }
        val userRef = db.collection("usuarios").document(userId)
        db.runTransaction { transaction ->
            val doc           = transaction.get(userRef)
            val currentRating = doc.getDouble("rating")      ?: 0.0
            val currentCount  = doc.getLong("ratingCount")   ?: 0L
            val newCount      = currentCount + 1
            val newRating     = ((currentRating * currentCount) + newStars) / newCount
            transaction.update(userRef, "rating",      newRating)
            transaction.update(userRef, "ratingCount", newCount)
        }
            .addOnSuccessListener {
                Toast.makeText(this, "¡Gracias por calificar! 🐾", Toast.LENGTH_SHORT).show()
                goToDashboard()
            }
            .addOnFailureListener { goToDashboard() }
    }

    private fun goToDashboard() {
        val dest = if (session.getUserType() == "owner")
            OwnerDashboardActivity::class.java
        else WalkerDashboardActivity::class.java
        startActivity(Intent(this, dest).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        })
        finish()
    }
}