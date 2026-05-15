package com.example.proyectointegrador.activities

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectointegrador.R
import com.example.proyectointegrador.adapters.WalkerAdapter
import com.example.proyectointegrador.models.Walker
import com.example.proyectointegrador.utils.SessionManager
import com.google.firebase.firestore.FirebaseFirestore

class AvailableWalkersActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var session: SessionManager
    private var duration = 30
    private var cost = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_available_walkers)

        // Toolbar con botón back
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Paseadores disponibles"

        session  = SessionManager(this)
        duration = intent.getIntExtra("duration", 30)
        cost     = intent.getDoubleExtra("cost", 0.0)

        // Estos IDs existen en activity_available_walkers.xml
        val rvWalkers         = findViewById<RecyclerView>(R.id.rvWalkers)
        val layoutEmpty       = findViewById<LinearLayout>(R.id.layoutEmpty)
        val tvSummaryDuration = findViewById<TextView>(R.id.tvSummaryDuration)
        val tvSummaryCost     = findViewById<TextView>(R.id.tvSummaryCost)

        tvSummaryDuration.text = "$duration min"
        tvSummaryCost.text     = "$${"%.2f".format(cost)}"

        rvWalkers.layoutManager = LinearLayoutManager(this)

        db.collection("usuarios")
            .whereEqualTo("userType", "walker")
            .whereEqualTo("isAvailable", true)
            .get()
            .addOnSuccessListener { documents ->
                val walkers = documents.map { doc ->
                    Walker(
                        id          = doc.id,
                        name        = doc.getString("name") ?: "",
                        email       = doc.getString("email") ?: "",
                        phone       = doc.getString("phone") ?: "",
                        cardNumber  = doc.getString("cardNumber") ?: "",
                        rating      = (doc.getDouble("rating") ?: 0.0).toFloat(),
                        ratingCount = (doc.getLong("ratingCount") ?: 0L).toInt(),
                        isAvailable = doc.getBoolean("isAvailable") ?: true
                    )
                }

                if (walkers.isEmpty()) {
                    layoutEmpty.visibility = View.VISIBLE
                    rvWalkers.visibility   = View.GONE
                } else {
                    layoutEmpty.visibility = View.GONE
                    rvWalkers.visibility   = View.VISIBLE
                    rvWalkers.adapter = WalkerAdapter(walkers) { selectedWalker ->
                        confirmRequest(selectedWalker)
                    }
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    this,
                    "Error al cargar paseadores: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun confirmRequest(walker: Walker) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar solicitud")
            .setMessage("¿Enviar solicitud a ${walker.name}?")
            .setPositiveButton("Sí") { _, _ -> sendRequest(walker) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun sendRequest(walker: Walker) {
        db.collection("perros")
            .whereEqualTo("ownerId", session.getUserId())
            .get()
            .addOnSuccessListener { dogDocs ->
                if (dogDocs.isEmpty) {
                    Toast.makeText(this,
                        "Primero registra tu perro",
                        Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val dog = dogDocs.first()

                db.collection("usuarios")
                    .document(session.getUserId())
                    .get()
                    .addOnSuccessListener { ownerDoc ->
                        val ownerName = ownerDoc.getString("name") ?: ""

                        val request = hashMapOf(
                            "ownerId"         to session.getUserId(),
                            "ownerName"       to ownerName,
                            "walkerId"        to walker.id,
                            "dogId"           to dog.id,
                            "dogName"         to (dog.getString("name") ?: ""),
                            "dogBreed"        to (dog.getString("breed") ?: ""),
                            "dogSize"         to (dog.getString("size") ?: ""),
                            "dogAge"          to (dog.getLong("age") ?: 0L),
                            "dogAllergy"      to (dog.getString("allergy") ?: ""),
                            "durationMinutes" to duration,
                            "cost"            to cost,
                            "status"          to "pending",
                            "startTime"       to 0L,
                            "endTime"         to 0L,
                            "paymentMethod"   to ""
                        )

                        db.collection("solicitudes")
                            .add(request)
                            .addOnSuccessListener {
                                Toast.makeText(this,
                                    "✓ Solicitud enviada a ${walker.name}",
                                    Toast.LENGTH_LONG).show()
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this,
                                    "Error al enviar: ${e.message}",
                                    Toast.LENGTH_LONG).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this,
                            "Error: ${e.message}",
                            Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this,
                    "Error al obtener perro: ${e.message}",
                    Toast.LENGTH_LONG).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}