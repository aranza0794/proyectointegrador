package com.example.proyectointegrador.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectointegrador.R
import com.example.proyectointegrador.adapters.WalkRequestAdapter
import com.example.proyectointegrador.models.WalkRequest
import com.example.proyectointegrador.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.firestore.FirebaseFirestore

class WalkerDashboardActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_walker_dashboard)

        session = SessionManager(this)

        val tvWelcome         = findViewById<TextView>(R.id.tvWelcome)
        val rvRequests        = findViewById<RecyclerView>(R.id.rvPendingRequests)
        val btnLogout         = findViewById<LinearLayout>(R.id.btnLogout)
        val switchAvailable   = findViewById<SwitchMaterial>(R.id.switchAvailable)
        val tvAvailableStatus = findViewById<TextView>(R.id.tvAvailableStatus)
        val btnWalkerHistory  = findViewById<MaterialButton>(R.id.btnWalkerHistory)

        tvWelcome.text         = session.getUserName()
        tvAvailableStatus.text = "Disponible para paseos"
        rvRequests.layoutManager = LinearLayoutManager(this)

        switchAvailable.setOnCheckedChangeListener { _, isChecked ->
            tvAvailableStatus.text =
                if (isChecked) "Disponible para paseos" else "No disponible"
            db.collection("usuarios")
                .document(session.getUserId())
                .update("isAvailable", isChecked)
        }

        // FIX: Botón perfil con ícono 👤
        btnLogout.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // FIX: Solo historial, sin ganancias
        btnWalkerHistory.setOnClickListener {
            startActivity(Intent(this, WalkHistoryActivity::class.java))
        }

        loadPendingRequests(rvRequests)
    }

    override fun onResume() {
        super.onResume()
        db.collection("solicitudes")
            .whereEqualTo("walkerId", session.getUserId())
            .get()
            .addOnSuccessListener { docs ->
                val activeDoc = docs.documents.firstOrNull { doc ->
                    val s = doc.getString("status") ?: ""
                    s == "active" || s == "accepted"
                }
                if (activeDoc != null) {
                    startActivity(Intent(this, ActiveWalkActivity::class.java))
                }
            }
    }

    private fun loadPendingRequests(rvRequests: RecyclerView) {
        val layoutEmpty    = findViewById<LinearLayout>(R.id.layoutEmpty)
        val tvRequestCount = findViewById<TextView>(R.id.tvRequestCount)
        val walkerId       = session.getUserId()

        db.collection("solicitudes")
            .whereEqualTo("walkerId", walkerId)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("WALKER_DASH", "Error: ${error.message}")
                    Toast.makeText(this,
                        "Sin conexión. Verifica tu internet.",
                        Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val requests = snapshots?.documents
                    ?.filter { it.getString("status") == "pending" }
                    ?.map { doc ->
                        WalkRequest(
                            id              = doc.id,
                            ownerId         = doc.getString("ownerId") ?: "",
                            ownedName       = doc.getString("ownerName") ?: "",
                            walkerId        = doc.getString("walkerId") ?: "",
                            dogName         = doc.getString("dogName") ?: "",
                            dogBreed        = doc.getString("dogBreed") ?: "",
                            dogSize         = doc.getString("dogSize") ?: "",
                            dogAge          = (doc.getLong("dogAge") ?: 0L).toInt(),
                            dogAllergy      = doc.getString("dogAllergy") ?: "",
                            durationMinutes = (doc.getLong("durationMinutes") ?: 0L).toInt(),
                            cost            = doc.getDouble("cost") ?: 0.0,
                            status          = doc.getString("status") ?: ""
                        )
                    } ?: emptyList()

                tvRequestCount.text = requests.size.toString()

                if (requests.isEmpty()) {
                    layoutEmpty.visibility = View.VISIBLE
                    rvRequests.visibility  = View.GONE
                } else {
                    layoutEmpty.visibility = View.GONE
                    rvRequests.visibility  = View.VISIBLE
                    rvRequests.adapter = WalkRequestAdapter(
                        requests,
                        onAccept = { req -> acceptRequest(req.id) },
                        onReject = { req -> confirmReject(req) }
                    )
                }
            }
    }

    private fun acceptRequest(requestId: String) {
        db.collection("solicitudes").document(requestId)
            .update("status", "accepted")
            .addOnSuccessListener {
                Toast.makeText(this, "✓ Paseo aceptado", Toast.LENGTH_SHORT).show()
                db.collection("solicitudes")
                    .whereEqualTo("walkerId", session.getUserId())
                    .whereEqualTo("status", "pending")
                    .get()
                    .addOnSuccessListener { docs ->
                        docs.documents.forEach { doc ->
                            if (doc.id != requestId) {
                                db.collection("solicitudes").document(doc.id)
                                    .update("status", "rejected")
                            }
                        }
                    }
                startActivity(Intent(this, ActiveWalkActivity::class.java))
            }
            .addOnFailureListener {
                Toast.makeText(this,
                    "Sin conexión. Intenta de nuevo.",
                    Toast.LENGTH_SHORT).show()
            }
    }

    private fun confirmReject(req: WalkRequest) {
        AlertDialog.Builder(this)
            .setTitle("Rechazar solicitud")
            .setMessage("¿Seguro que quieres rechazar el paseo de ${req.dogName}?\nGanancia: $${String.format("%.0f", req.cost)}")
            .setPositiveButton("Sí, rechazar") { _, _ ->
                db.collection("solicitudes").document(req.id)
                    .update("status", "rejected")
                    .addOnFailureListener {
                        Toast.makeText(this,
                            "Sin conexión. Intenta de nuevo.",
                            Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}