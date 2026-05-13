package com.example.proyectointegrador.activities

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectointegrador.R
import com.example.proyectointegrador.utils.SessionManager
import com.google.firebase.firestore.FirebaseFirestore

class TrackWalkActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_track_walk)

        session = SessionManager(this)

        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val tvWalkerName = findViewById<TextView>(R.id.tvWalkerName)
        val tvDogInfo = findViewById<TextView>(R.id.tvDogInfo)
        val tvDuration = findViewById<TextView>(R.id.tvDuration)

        // Escuchar cambios en tiempo real con Firestore
        db.collection("solicitudes")
            .whereEqualTo("ownerId", session.getUserId())
            .whereIn("status", listOf("pending", "accepted", "active", "finished"))
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null) return@addSnapshotListener

                if (!snapshots.isEmpty) {
                    val doc = snapshots.documents.first()
                    val status = doc.getString("status") ?: ""
                    val walkerId = doc.getString("walkerId") ?: ""
                    val dogName = doc.getString("dogName") ?: ""
                    val duration = doc.getLong("durationMinutes") ?: 0L
                    val walkId = doc.id

                    tvDogInfo.text = "$dogName • ${duration} min"

                    // Obtener nombre del paseador
                    if (walkerId.isNotEmpty()) {
                        db.collection("usuarios").document(walkerId)
                            .get()
                            .addOnSuccessListener { walkerDoc ->
                                tvWalkerName.text =
                                    "Paseador: ${walkerDoc.getString("name") ?: ""}"
                            }
                    }

                    when (status) {
                        "pending" -> tvStatus.text = "⏳ Esperando que el paseador acepte..."
                        "accepted" -> tvStatus.text = "✅ Paseador en camino"
                        "active" -> tvStatus.text = "🐕 Paseo en curso"
                        "finished" -> {
                            tvStatus.text = "🎉 ¡Paseo finalizado!"
                            val intent = Intent(this, PaymentActivity::class.java)
                            intent.putExtra("walk_id", walkId)
                            startActivity(intent)
                            finish()
                        }
                    }
                }
            }
    }
}