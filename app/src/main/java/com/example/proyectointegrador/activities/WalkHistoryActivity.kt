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

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        session = SessionManager(this)

        val userType = session.getUserType()
        supportActionBar?.title = if (userType == "owner") "Mis paseos" else "Mi historial"

        val rvHistory      = findViewById<RecyclerView>(R.id.rvHistory)
        val layoutEmpty    = findViewById<LinearLayout>(R.id.layoutEmpty)
        val tvTotalWalks   = findViewById<TextView>(R.id.tvTotalWalks)
        val tvTotalSpent   = findViewById<TextView>(R.id.tvTotalSpent)
        val tvTotalMinutes = findViewById<TextView>(R.id.tvTotalMinutes)

        // Cambiar labels según tipo de usuario
        val tvSpentLabel = layoutOf(tvTotalSpent)
        if (userType == "walker") {
            // Para paseador "Gastado" → "Ganado"
            findSiblingLabel(tvTotalSpent, "Gastado", "Ganado")
        }

        rvHistory.layoutManager = LinearLayoutManager(this)

        // FIX: Query según tipo de usuario
        val queryField = if (userType == "owner") "ownerId" else "walkerId"

        db.collection("solicitudes")
            .whereEqualTo(queryField, session.getUserId())
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
                        paymentMethod   = doc.getString("paymentMethod") ?: "",
                        walkerId        = doc.getString("walkerId") ?: "",
                        ownedName       = doc.getString("ownerName") ?: ""
                    )
                }

                tvTotalWalks.text   = history.size.toString()
                tvTotalSpent.text   = "$${String.format("%.0f", history.sumOf { it.cost })}"
                tvTotalMinutes.text = history.sumOf { it.durationMinutes }.toString()

                if (history.isEmpty()) {
                    layoutEmpty.visibility = View.VISIBLE
                    rvHistory.visibility   = View.GONE
                } else {
                    layoutEmpty.visibility = View.GONE
                    rvHistory.visibility   = View.VISIBLE
                    // FIX: pasar userType para mostrar info correcta
                    rvHistory.adapter = WalkHistoryAdapter(history, db, userType)
                }
            }
            .addOnFailureListener {
                layoutEmpty.visibility = View.VISIBLE
                rvHistory.visibility   = View.GONE
            }
    }

    private fun layoutOf(view: View) = view.parent as? android.view.ViewGroup

    private fun findSiblingLabel(view: TextView, oldText: String, newText: String) {
        val parent = view.parent as? android.view.ViewGroup ?: return
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (child is TextView && child.text == oldText) {
                child.text = newText
                break
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}