package com.example.proyectointegrador.activities

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectointegrador.R
import com.example.proyectointegrador.adapters.WalkHistoryAdapter
import com.example.proyectointegrador.models.WalkRequest
import com.example.proyectointegrador.utils.SessionManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class WalkHistoryActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_walk_history)

        // ── Toolbar con botón back ──────────────────────────
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Historial de paseos"

        session = SessionManager(this)

        val rvHistory   = findViewById<RecyclerView>(R.id.rvHistory)
        val layoutEmpty = findViewById<LinearLayout>(R.id.layoutEmpty)
        val tvTotalWalks   = findViewById<TextView>(R.id.tvTotalWalks)
        val tvTotalSpent   = findViewById<TextView>(R.id.tvTotalSpent)
        val tvTotalMinutes = findViewById<TextView>(R.id.tvTotalMinutes)

        rvHistory.layoutManager = LinearLayoutManager(this)

        db.collection("solicitudes")
            .whereEqualTo("ownerId", session.getUserId())
            .whereEqualTo("status", "finished")
            .orderBy("endTime", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val history = documents.map { doc ->
                    WalkRequest(
                        id              = doc.id,
                        dogName         = doc.getString("dogName") ?: "",
                        dogBreed        = doc.getString("dogBreed") ?: "",
                        durationMinutes = (doc.getLong("durationMinutes") ?: 0L).toInt(),
                        cost            = doc.getDouble("cost") ?: 0.0,
                        status          = doc.getString("status") ?: "",
                        startTime       = doc.getLong("startTime") ?: 0L,
                        endTime         = doc.getLong("endTime") ?: 0L,
                        paymentMethod   = doc.getString("paymentMethod") ?: ""
                    )
                }

                // Calcular estadísticas
                val totalWalks   = history.size
                val totalSpent   = history.sumOf { it.cost }
                val totalMinutes = history.sumOf { it.durationMinutes }

                tvTotalWalks.text   = totalWalks.toString()
                tvTotalSpent.text   = "$${String.format("%.0f", totalSpent)}"
                tvTotalMinutes.text = totalMinutes.toString()

                if (history.isEmpty()) {
                    layoutEmpty.visibility = View.VISIBLE
                    rvHistory.visibility   = View.GONE
                } else {
                    layoutEmpty.visibility = View.GONE
                    rvHistory.visibility   = View.VISIBLE
                    rvHistory.adapter = WalkHistoryAdapter(history)
                }
            }
            .addOnFailureListener {
                layoutEmpty.visibility = View.VISIBLE
                rvHistory.visibility   = View.GONE
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}