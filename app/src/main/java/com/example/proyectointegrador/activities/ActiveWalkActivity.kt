package com.example.proyectointegrador.activities

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectointegrador.R
import com.example.proyectointegrador.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore

class ActiveWalkActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var session: SessionManager
    private var currentWalkId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_active_walk)

        session = SessionManager(this)

        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val tvDogInfo = findViewById<TextView>(R.id.tvDogInfo)
        val tvWalkCost = findViewById<TextView>(R.id.tvWalkCost)
        val btnStartWalk = findViewById<MaterialButton>(R.id.btnStartWalk)
        val btnFinishWalk = findViewById<MaterialButton>(R.id.btnFinishWalk)

        // Escuchar en tiempo real el paseo asignado
        db.collection("solicitudes")
            .whereEqualTo("walkerId", session.getUserId())
            .whereIn("status", listOf("accepted", "active"))
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null) return@addSnapshotListener

                if (!snapshots.isEmpty) {
                    val doc = snapshots.documents.first()
                    currentWalkId = doc.id
                    val status = doc.getString("status") ?: ""
                    val dogName = doc.getString("dogName") ?: ""
                    val dogBreed = doc.getString("dogBreed") ?: ""
                    val dogSize = doc.getString("dogSize") ?: ""
                    val dogAllergy = doc.getString("dogAllergy") ?: ""
                    val duration = doc.getLong("durationMinutes") ?: 0L
                    val cost = doc.getDouble("cost") ?: 0.0

                    tvDogInfo.text = "$dogName • $dogBreed • $dogSize"
                    tvWalkCost.text = "$${String.format("%.2f", cost)} • ${duration} min"

                    if (dogAllergy.isNotEmpty()) {
                        tvStatus.text = "⚠️ Alergia: $dogAllergy"
                    }

                    when (status) {
                        "accepted" -> {
                            tvStatus.text = "¡Listo para iniciar el paseo!"
                            btnStartWalk.isEnabled = true
                            btnFinishWalk.isEnabled = false
                        }
                        "active" -> {
                            tvStatus.text = "Paseo en curso 🐕"
                            btnStartWalk.isEnabled = false
                            btnFinishWalk.isEnabled = true
                        }
                    }
                }
            }

        btnStartWalk.setOnClickListener {
            if (currentWalkId.isEmpty()) {
                Toast.makeText(this, "No hay paseo asignado", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            db.collection("solicitudes").document(currentWalkId)
                .update(
                    mapOf(
                        "status" to "active",
                        "startTime" to System.currentTimeMillis()
                    )
                )
                .addOnSuccessListener {
                    Toast.makeText(this, "¡Paseo iniciado!", Toast.LENGTH_SHORT).show()
                }
        }

        btnFinishWalk.setOnClickListener {
            if (currentWalkId.isEmpty()) return@setOnClickListener
            db.collection("solicitudes").document(currentWalkId)
                .update(
                    mapOf(
                        "status" to "finished",
                        "endTime" to System.currentTimeMillis()
                    )
                )
                .addOnSuccessListener {
                    val intent = Intent(this, PaymentActivity::class.java)
                    intent.putExtra("walk_id", currentWalkId)
                    startActivity(intent)
                    finish()
                }
        }
    }
}