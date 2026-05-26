package com.example.proyectointegrador.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectointegrador.R
import com.example.proyectointegrador.adapters.WalkRequestAdapter
import com.example.proyectointegrador.models.WalkRequest
import com.example.proyectointegrador.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class WalkerDashboardActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var session: SessionManager
    private var requestsListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_walker_dashboard)

        session = SessionManager(this)

        if (session.getUserId().isEmpty()) {
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
            return
        }

        val tvWelcome        = findViewById<TextView>(R.id.tvWelcome)
        val switchAvailable  = findViewById<SwitchMaterial>(R.id.switchAvailable)
        val tvAvailableStatus= findViewById<TextView>(R.id.tvAvailableStatus)
        val btnWalkerHistory = findViewById<MaterialButton>(R.id.btnWalkerHistory)
        val btnLogout        = findViewById<LinearLayout>(R.id.btnLogout)
        val rvRequests       = findViewById<RecyclerView>(R.id.rvPendingRequests)
        val tvRequestCount   = findViewById<TextView>(R.id.tvRequestCount)
        val layoutEmpty      = findViewById<LinearLayout>(R.id.layoutEmpty)

        tvWelcome.text = session.getUserName()

        db.collection("usuarios").document(session.getUserId()).get()
            .addOnSuccessListener { doc ->
                tvWelcome.text = doc.getString("name") ?: session.getUserName()
                val isAvailable = doc.getBoolean("isAvailable") ?: true
                switchAvailable.isChecked = isAvailable
                tvAvailableStatus.text = if (isAvailable)
                    "Disponible para paseos" else "No disponible"
            }

        switchAvailable.setOnCheckedChangeListener { _, isChecked ->
            tvAvailableStatus.text = if (isChecked) "Disponible para paseos" else "No disponible"
            db.collection("usuarios").document(session.getUserId())
                .update("isAvailable", isChecked)
        }

        rvRequests.layoutManager = LinearLayoutManager(this)

        btnWalkerHistory.setOnClickListener {
            startActivity(Intent(this, WalkHistoryActivity::class.java))
        }

        // FIX: btnLogout es LinearLayout — lleva al perfil
        btnLogout.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // Verificar si hay paseo activo
        db.collection("solicitudes")
            .whereEqualTo("walkerId", session.getUserId())
            .whereIn("status", listOf("accepted", "active"))
            .get()
            .addOnSuccessListener { docs ->
                if (!docs.isEmpty) {
                    startActivity(Intent(this, ActiveWalkActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    })
                }
            }

        loadPendingRequests(rvRequests, tvRequestCount, layoutEmpty)
    }

    private fun loadPendingRequests(
        rvRequests: RecyclerView,
        tvRequestCount: TextView,
        layoutEmpty: LinearLayout
    ) {
        requestsListener?.remove()
        requestsListener = db.collection("solicitudes")
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null) return@addSnapshotListener

                val requests = snapshots.documents
                    .filter { doc ->
                        // Solo mostrar solicitudes sin paseador asignado o con este paseador
                        val wId = doc.getString("walkerId") ?: ""
                        wId.isEmpty() || wId == session.getUserId()
                    }
                    .map { doc ->
                        WalkRequest(
                            id              = doc.id,
                            ownerId         = doc.getString("ownerId")          ?: "",
                            ownedName       = doc.getString("ownerName")        ?: "",
                            dogId           = doc.getString("dogId")            ?: "",
                            dogName         = doc.getString("dogName")          ?: "",
                            dogBreed        = doc.getString("dogBreed")         ?: "",
                            dogSize         = doc.getString("dogSize")          ?: "",
                            dogAge          = (doc.getLong("dogAge")            ?: 0L).toInt(),
                            dogAllergy      = doc.getString("dogAllergy")       ?: "",
                            durationMinutes = (doc.getLong("durationMinutes")   ?: 0L).toInt(),
                            cost            = doc.getDouble("cost")             ?: 0.0,
                            paymentMethod   = doc.getString("paymentMethod")    ?: ""
                        )
                    }

                tvRequestCount.text = requests.size.toString()

                if (requests.isEmpty()) {
                    layoutEmpty.visibility  = View.VISIBLE
                    rvRequests.visibility   = View.GONE
                } else {
                    layoutEmpty.visibility  = View.GONE
                    rvRequests.visibility   = View.VISIBLE
                    rvRequests.adapter      = WalkRequestAdapter(
                        requests,
                        onAccept = { req -> acceptRequest(req) },
                        onReject = { req -> confirmReject(req) }
                    )
                }
            }
    }

    private fun acceptRequest(req: WalkRequest) {
        db.collection("solicitudes").document(req.id)
            .update(mapOf(
                "status"     to "accepted",
                "walkerId"   to session.getUserId(),
                "walkerName" to session.getUserName()
            ))
            .addOnSuccessListener {
                Toast.makeText(this, "✓ Paseo aceptado", Toast.LENGTH_SHORT).show()
                // Rechazar las demás solicitudes pendientes de este paseador
                db.collection("solicitudes")
                    .whereEqualTo("walkerId", session.getUserId())
                    .whereEqualTo("status", "pending")
                    .get()
                    .addOnSuccessListener { docs ->
                        docs.documents.forEach { doc ->
                            if (doc.id != req.id)
                                db.collection("solicitudes").document(doc.id)
                                    .update("status", "rejected")
                        }
                    }
                startActivity(Intent(this, ActiveWalkActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                })
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al aceptar", Toast.LENGTH_SHORT).show()
            }
    }

    private fun confirmReject(req: WalkRequest) {
        AlertDialog.Builder(this)
            .setTitle("Rechazar solicitud")
            .setMessage("¿Seguro que quieres rechazar el paseo de ${req.dogName}?")
            .setPositiveButton("Sí, rechazar") { _, _ ->
                db.collection("solicitudes").document(req.id)
                    .update("status", "rejected")
                    .addOnSuccessListener {
                        Toast.makeText(this, "Solicitud rechazada", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        requestsListener?.remove()
    }
}