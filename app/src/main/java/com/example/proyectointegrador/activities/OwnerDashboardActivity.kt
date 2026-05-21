package com.example.proyectointegrador.activities

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectointegrador.R
import com.example.proyectointegrador.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class OwnerDashboardActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private val db = FirebaseFirestore.getInstance()
    private var activeSolicitudId  = ""
    private var lastNotifiedStatus = ""
    private var walkListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_owner_dashboard)

        session = SessionManager(this)

        val tvWelcome      = findViewById<TextView>(R.id.tvWelcome)
        val btnRequestWalk = findViewById<MaterialButton>(R.id.btnRequestWalk)
        // FIX: son MaterialCardView en el nuevo XML, no LinearLayout
        val btnMyDog       = findViewById<MaterialCardView>(R.id.btnMyDog)
        val btnHistory     = findViewById<MaterialCardView>(R.id.btnHistory)
        // FIX: btnLogout es LinearLayout en el nuevo XML
        val btnLogout      = findViewById<LinearLayout>(R.id.btnLogout)

        tvWelcome.text = session.getUserName()

        db.collection("usuarios").document(session.getUserId()).get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name") ?: session.getUserName()
                tvWelcome.text = name
                session.saveSession(session.getUserId(), session.getUserType(), name)
            }

        btnMyDog.setOnClickListener {
            startActivity(Intent(this, RegisterDogActivity::class.java))
        }
        btnHistory.setOnClickListener {
            startActivity(Intent(this, WalkHistoryActivity::class.java))
        }
        btnLogout.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        btnRequestWalk.setOnClickListener {
            handleMainButton()
        }
    }

    override fun onResume() {
        super.onResume()
        startWalkStatusListener()
    }

    override fun onDestroy() {
        super.onDestroy()
        walkListener?.remove()
    }

    private fun startWalkStatusListener() {
        walkListener?.remove()
        val btnRequestWalk = findViewById<MaterialButton>(R.id.btnRequestWalk)

        walkListener = db.collection("solicitudes")
            .whereEqualTo("ownerId", session.getUserId())
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null) return@addSnapshotListener

                val activeDoc = snapshots.documents.firstOrNull { doc ->
                    val s = doc.getString("status") ?: ""
                    s == "pending" || s == "accepted" || s == "active"
                }

                if (activeDoc != null) {
                    activeSolicitudId = activeDoc.id
                    val status = activeDoc.getString("status") ?: ""

                    if (status != lastNotifiedStatus && lastNotifiedStatus.isNotEmpty()) {
                        when (status) {
                            "accepted" -> Toast.makeText(this,
                                "✅ ¡El paseador aceptó tu solicitud!",
                                Toast.LENGTH_LONG).show()
                            "active" -> Toast.makeText(this,
                                "🐕 ¡El paseo ha iniciado!",
                                Toast.LENGTH_LONG).show()
                        }
                    }
                    lastNotifiedStatus = status

                    when (status) {
                        "pending" -> {
                            btnRequestWalk.text = "⏳ Esperando paseador... (toca para cancelar)"
                            btnRequestWalk.backgroundTintList =
                                android.content.res.ColorStateList.valueOf(
                                    android.graphics.Color.parseColor("#E9C46A"))
                        }
                        "accepted" -> {
                            btnRequestWalk.text = "✅ Paseador en camino → Ver mapa"
                            btnRequestWalk.backgroundTintList =
                                android.content.res.ColorStateList.valueOf(
                                    android.graphics.Color.parseColor("#2A9D8F"))
                        }
                        "active" -> {
                            btnRequestWalk.text = "🐕 Paseo en curso → Ver mapa"
                            btnRequestWalk.backgroundTintList =
                                android.content.res.ColorStateList.valueOf(
                                    android.graphics.Color.parseColor("#2A9D8F"))
                        }
                    }
                } else {
                    activeSolicitudId  = ""
                    lastNotifiedStatus = ""
                    btnRequestWalk.text = "🐾 Solicitar paseo"
                    btnRequestWalk.backgroundTintList =
                        android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.parseColor("#2A9D8F"))
                }
            }
    }

    private fun handleMainButton() {
        if (activeSolicitudId.isEmpty()) {
            startActivity(Intent(this, RequestWalkActivity::class.java))
            return
        }

        db.collection("solicitudes").document(activeSolicitudId).get()
            .addOnSuccessListener { doc ->
                when (doc.getString("status") ?: "") {
                    "pending" -> {
                        AlertDialog.Builder(this)
                            .setTitle("Solicitud pendiente")
                            .setMessage("¿Qué deseas hacer?")
                            .setPositiveButton("Cancelar solicitud") { _, _ -> cancelRequest() }
                            .setNegativeButton("Mantener", null)
                            .show()
                    }
                    "accepted", "active" -> {
                        startActivity(Intent(this, TrackWalkActivity::class.java))
                    }
                }
            }
    }

    private fun cancelRequest() {
        db.collection("solicitudes").document(activeSolicitudId)
            .update("status", "cancelled")
            .addOnSuccessListener {
                activeSolicitudId  = ""
                lastNotifiedStatus = ""
                val btnRequestWalk = findViewById<MaterialButton>(R.id.btnRequestWalk)
                btnRequestWalk.text = "🐾 Solicitar paseo"
                btnRequestWalk.backgroundTintList =
                    android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#2A9D8F"))
                Toast.makeText(this, "Solicitud cancelada", Toast.LENGTH_SHORT).show()
            }
    }
}