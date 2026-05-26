package com.example.proyectointegrador.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
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

    // FIX: flag para evitar selección doble
    private var isRequestInProgress = false

    private val db = FirebaseFirestore.getInstance()
    private lateinit var session: SessionManager
    private var duration      = 30
    private var cost          = 0.0
    private var paymentMethod = "cash"
    private var dogId         = ""
    private var dogName       = ""
    private var dogBreed      = ""
    private var dogSize       = ""
    private var dogAge        = 0
    private var dogAllergy    = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_available_walkers)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Paseadores disponibles"

        session       = SessionManager(this)
        duration      = intent.getIntExtra("duration", 30)
        cost          = intent.getDoubleExtra("cost", 0.0)
        paymentMethod = intent.getStringExtra("paymentMethod") ?: "cash"
        dogId         = intent.getStringExtra("dogId") ?: ""
        dogName       = intent.getStringExtra("dogName") ?: ""
        dogBreed      = intent.getStringExtra("dogBreed") ?: ""
        dogSize       = intent.getStringExtra("dogSize") ?: ""
        dogAge        = intent.getIntExtra("dogAge", 0)
        dogAllergy    = intent.getStringExtra("dogAllergy") ?: ""

        val rvWalkers         = findViewById<RecyclerView>(R.id.rvWalkers)
        val layoutEmpty       = findViewById<LinearLayout>(R.id.layoutEmpty)
        val progressBar       = findViewById<ProgressBar>(R.id.progressBar)

        rvWalkers.layoutManager = LinearLayoutManager(this)

        // Mostrar loading
        progressBar.visibility = View.VISIBLE
        rvWalkers.visibility   = View.GONE
        layoutEmpty.visibility = View.GONE

        db.collection("usuarios")
            .whereEqualTo("userType", "walker")
            .whereEqualTo("isAvailable", true)
            .get()
            .addOnSuccessListener { documents ->
                progressBar.visibility = View.GONE

                // FIX: Filtrar al dueño actual por si acaso tiene cuenta de paseador
                val walkers = documents
                    .filter { doc -> doc.id != session.getUserId() }
                    .map { doc ->
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
                progressBar.visibility = View.GONE
                // FIX: Manejo de error de red
                layoutEmpty.visibility = View.VISIBLE
                rvWalkers.visibility   = View.GONE
                Toast.makeText(this,
                    "Sin conexión. Verifica tu internet.",
                    Toast.LENGTH_LONG).show()
                Log.e("WALKERS", "Error: ${exception.message}")
            }
    }

    private fun confirmRequest(walker: Walker) {
        // FIX: si ya hay una solicitud en proceso, ignorar nuevo click
        if (isRequestInProgress) return
        isRequestInProgress = true

        val paymentText = if (paymentMethod == "cash") "Efectivo" else "Transferencia"
        AlertDialog.Builder(this)
            .setTitle("Confirmar solicitud")
            .setMessage("¿Enviar solicitud a ${walker.name}?\n\nPerro: $dogName\nDuración: $duration min\nPago: $paymentText")
            .setPositiveButton("Sí") { _, _ -> sendRequest(walker) }
            .setNegativeButton("Cancelar") { _, _ ->
                // FIX: si cancela, permitir volver a seleccionar
                isRequestInProgress = false
            }
            .setOnCancelListener {
                isRequestInProgress = false
            }
            .show()
    }

    private fun sendRequest(walker: Walker) {
        db.collection("usuarios")
            .document(session.getUserId())
            .get()
            .addOnSuccessListener { ownerDoc ->
                val ownerName = ownerDoc.getString("name") ?: ""

                val request = hashMapOf(
                    "ownerId"         to session.getUserId(),
                    "ownerName"       to ownerName,
                    "walkerId"        to walker.id,
                    "dogId"           to dogId,
                    "dogName"         to dogName,
                    "dogBreed"        to dogBreed,
                    "dogSize"         to dogSize,
                    "dogAge"          to dogAge,
                    "dogAllergy"      to dogAllergy,
                    "durationMinutes" to duration,
                    "cost"            to cost,
                    "status"          to "pending",
                    "paymentMethod"   to paymentMethod,
                    "startTime"       to 0L,
                    "endTime"         to 0L,
                    "ratingStars"     to 0
                )

                db.collection("solicitudes")
                    .add(request)
                    .addOnSuccessListener {
                        Toast.makeText(this,
                            "✓ Solicitud enviada a ${walker.name}",
                            Toast.LENGTH_LONG).show()
                        val intent = Intent(this, OwnerDashboardActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this,
                            "Sin conexión. Intenta de nuevo.",
                            Toast.LENGTH_LONG).show()
                        Log.e("SEND_REQUEST", "Error: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this,
                    "Sin conexión. Intenta de nuevo.",
                    Toast.LENGTH_LONG).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}