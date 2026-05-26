package com.example.proyectointegrador.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import com.example.proyectointegrador.R
import com.example.proyectointegrador.utils.SessionManager
import com.google.android.gms.location.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ActiveWalkActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var session: SessionManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var currentWalkId  = ""
    private var currentOwnerId = ""
    private var walkListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_active_walk)

        session = SessionManager(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val toolbar       = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Paseo activo"

        // IDs que SÍ existen en activity_active_walk.xml
        val tvDogName     = findViewById<TextView>(R.id.tvDogName)
        val tvDogDetails  = findViewById<TextView>(R.id.tvDogDetails)
        val tvAllergyAlert= findViewById<MaterialCardView>(R.id.tvAllergyAlert)
        val tvAllergyText = findViewById<TextView>(R.id.tvAllergyText)
        val tvWalkerName  = findViewById<TextView>(R.id.tvWalkerName)
        val tvDuration    = findViewById<TextView>(R.id.tvDuration)
        val tvElapsed     = findViewById<TextView>(R.id.tvElapsed)
        val progressWalk  = findViewById<android.widget.ProgressBar>(R.id.progressWalk)
        val btnEndWalk    = findViewById<MaterialButton>(R.id.btnEndWalk)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                if (currentWalkId.isNotEmpty()) {
                    db.collection("solicitudes").document(currentWalkId)
                        .update(mapOf(
                            "walkerLat" to location.latitude,
                            "walkerLng" to location.longitude
                        ))
                }
            }
        }

        // Escuchar solicitud activa del paseador
        walkListener = db.collection("solicitudes")
            .whereEqualTo("walkerId", session.getUserId())
            .whereIn("status", listOf("accepted", "active"))
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null || snapshots.isEmpty) return@addSnapshotListener

                val doc        = snapshots.documents.first()
                currentWalkId  = doc.id
                currentOwnerId = doc.getString("ownerId") ?: ""

                val dogName    = doc.getString("dogName")    ?: ""
                val dogBreed   = doc.getString("dogBreed")   ?: ""
                val dogSize    = doc.getString("dogSize")    ?: ""
                val dogAge     = doc.getLong("dogAge")       ?: 0L
                val dogAllergy = doc.getString("dogAllergy") ?: ""
                val duration   = doc.getLong("durationMinutes") ?: 0L
                val cost       = doc.getDouble("cost")       ?: 0.0
                val status     = doc.getString("status")     ?: ""
                val walkerName = doc.getString("walkerName") ?: session.getUserName()
                val startTime  = doc.getLong("startTime")    ?: 0L

                // Llenar vistas con IDs correctos
                tvDogName.text    = dogName
                tvDogDetails.text = "$dogBreed • $dogSize • $dogAge años • $${String.format("%.0f", cost)}"
                tvWalkerName.text = walkerName
                tvDuration.text   = "${duration} min"

                // Progreso
                if (startTime > 0 && duration > 0) {
                    val elapsed = ((System.currentTimeMillis() - startTime) / 1000 / 60)
                    val progress = ((elapsed.toFloat() / duration.toFloat()) * 100).toInt()
                        .coerceIn(0, 100)
                    tvElapsed.text   = "$elapsed min"
                    progressWalk.progress = progress
                }

                // Alergia
                if (dogAllergy.isNotEmpty() &&
                    dogAllergy.lowercase() != "ninguna" &&
                    dogAllergy.lowercase() != "none") {
                    tvAllergyAlert.visibility = View.VISIBLE
                    tvAllergyText.text        = dogAllergy
                } else {
                    tvAllergyAlert.visibility = View.GONE
                }

                // Botón según estado
                if (status == "accepted") {
                    btnEndWalk.text = "🐕 Iniciar paseo"
                    btnEndWalk.backgroundTintList =
                        android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.parseColor("#2A9D8F"))
                } else {
                    btnEndWalk.text = "🏁 Finalizar paseo"
                    btnEndWalk.backgroundTintList =
                        android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.parseColor("#E63946"))
                    startLocationUpdates()
                }
            }

        btnEndWalk.setOnClickListener {
            if (currentWalkId.isEmpty()) return@setOnClickListener

            db.collection("solicitudes").document(currentWalkId).get()
                .addOnSuccessListener { doc ->
                    when (doc.getString("status")) {
                        "accepted" -> {
                            // Iniciar paseo
                            db.collection("solicitudes").document(currentWalkId)
                                .update(mapOf(
                                    "status"    to "active",
                                    "startTime" to System.currentTimeMillis()
                                ))
                                .addOnSuccessListener {
                                    Toast.makeText(this, "🐕 ¡Paseo iniciado!", Toast.LENGTH_SHORT).show()
                                    startLocationUpdates()
                                }
                        }
                        "active" -> {
                            AlertDialog.Builder(this)
                                .setTitle("Finalizar paseo")
                                .setMessage("¿Confirmas que el paseo ha terminado?")
                                .setPositiveButton("Sí, finalizar") { _, _ -> finishWalk() }
                                .setNegativeButton("Cancelar", null)
                                .show()
                        }
                    }
                }
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
            return
        }
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 5000L).build()
        fusedLocationClient.requestLocationUpdates(request, locationCallback, mainLooper)
    }

    private fun finishWalk() {
        db.collection("solicitudes").document(currentWalkId)
            .update(mapOf(
                "status"  to "completed",
                "endTime" to System.currentTimeMillis()
            ))
            .addOnSuccessListener {
                stopLocationUpdates()
                Toast.makeText(this, "✓ Paseo finalizado", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, RatingActivity::class.java).apply {
                    putExtra("walk_id",   currentWalkId)
                    putExtra("owner_id",  currentOwnerId)
                    putExtra("walker_id", session.getUserId())
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                })
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al finalizar", Toast.LENGTH_SHORT).show()
            }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onDestroy() {
        super.onDestroy()
        walkListener?.remove()
        stopLocationUpdates()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}