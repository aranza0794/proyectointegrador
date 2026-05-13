package com.example.proyectointegrador.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectointegrador.R
import com.example.proyectointegrador.adapters.WalkRequestAdapter
import com.example.proyectointegrador.models.WalkRequest
import com.example.proyectointegrador.utils.SessionManager
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.firestore.FirebaseFirestore

class WalkerDashboardActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_walker_dashboard)

        session = SessionManager(this)

        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        val rvRequests = findViewById<RecyclerView>(R.id.rvPendingRequests)
        val btnLogout = findViewById<ImageButton>(R.id.btnLogout)
        val switchAvailable = findViewById<SwitchMaterial>(R.id.switchAvailable)
        val tvAvailableStatus = findViewById<TextView>(R.id.tvAvailableStatus)

        tvWelcome.text = session.getUserName()
        rvRequests.layoutManager = LinearLayoutManager(this)

        // Switch disponibilidad
        switchAvailable.setOnCheckedChangeListener { _, isChecked ->
            tvAvailableStatus.text =
                if (isChecked) "Disponible para paseos" else "No disponible"
            db.collection("usuarios")
                .document(session.getUserId())
                .update("isAvailable", isChecked)
        }

        btnLogout.setOnClickListener {
            session.clearSession()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        loadPendingRequests(rvRequests)
    }

    override fun onResume() {
        super.onResume()
        // Verificar si hay paseo activo
        db.collection("solicitudes")
            .whereEqualTo("walkerId", session.getUserId())
            .whereEqualTo("status", "active")
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    startActivity(Intent(this, ActiveWalkActivity::class.java))
                }
            }
    }

    private fun loadPendingRequests(rvRequests: RecyclerView) {
        val layoutEmpty = findViewById<LinearLayout>(R.id.layoutEmpty)

        db.collection("solicitudes")
            .whereEqualTo("walkerId", session.getUserId())
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshots, error ->
                if (error != null) return@addSnapshotListener

                val requests = snapshots?.documents?.map { doc ->
                    WalkRequest(
                        id = doc.id,
                        ownerId = doc.getString("ownerId") ?: "",
                        ownedName = doc.getString("ownerName") ?: "",
                        walkerId = doc.getString("walkerId") ?: "",
                        dogName = doc.getString("dogName") ?: "",
                        dogBreed = doc.getString("dogBreed") ?: "",
                        dogSize = doc.getString("dogSize") ?: "",
                        dogAge = (doc.getLong("dogAge") ?: 0L).toInt(),
                        dogAllergy = doc.getString("dogAllergy") ?: "",
                        durationMinutes = (doc.getLong("durationMinutes") ?: 0L).toInt(),
                        cost = doc.getDouble("cost") ?: 0.0,
                        status = doc.getString("status") ?: ""
                    )
                } ?: emptyList()

                val tvRequestCount = findViewById<TextView>(R.id.tvRequestCount)
                tvRequestCount.text = requests.size.toString()

                if (requests.isEmpty()) {
                    layoutEmpty.visibility = View.VISIBLE
                    rvRequests.visibility = View.GONE
                } else {
                    layoutEmpty.visibility = View.GONE
                    rvRequests.visibility = View.VISIBLE
                    rvRequests.adapter = WalkRequestAdapter(
                        requests,
                        onAccept = { req -> respondRequest(req.id, "accepted") },
                        onReject = { req -> respondRequest(req.id, "rejected") }
                    )
                }
            }
    }

    private fun respondRequest(requestId: String, status: String) {
        db.collection("solicitudes").document(requestId)
            .update("status", status)
            .addOnSuccessListener {
                val msg = if (status == "accepted") "Paseo aceptado" else "Paseo rechazado"
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                if (status == "accepted") {
                    startActivity(Intent(this, ActiveWalkActivity::class.java))
                }
            }
    }
}