package com.example.proyectointegrador.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.proyectointegrador.R
import com.example.proyectointegrador.utils.SessionManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore

class ActiveWalkActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var session: SessionManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var currentWalkId = ""
    private var currentOwnerId = ""

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_active_walk)

        session = SessionManager(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val tvStatus         = findViewById<TextView>(R.id.tvStatus)
        val tvDogInfo        = findViewById<TextView>(R.id.tvDogInfo)
        val tvDogAge         = findViewById<TextView>(R.id.tvDogAge)
        val tvDogAllergy     = findViewById<TextView>(R.id.tvDogAllergy)
        val layoutAllergy    = findViewById<LinearLayout>(R.id.layoutAllergy)
        val tvWalkCost       = findViewById<TextView>(R.id.tvWalkCost)
        val tvLocationStatus = findViewById<TextView>(R.id.tvLocationStatus)
        val btnStartWalk     = findViewById<MaterialButton>(R.id.btnStartWalk)
        val btnFinishWalk    = findViewById<MaterialButton>(R.id.btnFinishWalk)

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
                tvLocationStatus.text = "📍 Ubicación compartida"
            }
        }

        db.collection("solicitudes")
            .whereEqualTo("walkerId", session.getUserId())
            .whereIn("status", listOf("accepted", "active"))
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null || snapshots.isEmpty) return@addSnapshotListener

                val doc        = snapshots.documents.first()
                currentWalkId  = doc.id
                currentOwnerId = doc.getString("ownerId") ?: ""
                val status     = doc.getString("status") ?: ""
                val dogName    = doc.getString("dogName") ?: ""
                val dogBreed   = doc.getString("dogBreed") ?: ""
                val dogSize    = doc.getString("dogSize") ?: ""
                val dogAge     = doc.getLong("dogAge") ?: 0L
                val dogAllergy = doc.getString("dogAllergy") ?: ""
                val duration   = doc.getLong("durationMinutes") ?: 0L
                val cost       = doc.getDouble("cost") ?: 0.0

                tvDogInfo.text  = "$dogName • $dogBreed • $dogSize"
                tvDogAge.text   = "Edad: $dogAge años"
                tvWalkCost.text = "$${String.format("%.0f", cost)} • ${duration} min"

                if (dogAllergy.isNotEmpty() &&
                    dogAllergy != "ninguna" && dogAllergy != "Ninguna") {
                    layoutAllergy.visibility = View.VISIBLE
                    tvDogAllergy.text        = dogAllergy
                } else {
                    layoutAllergy.visibility = View.GONE
                }

                when (status) {
                    "accepted" -> {
                        tvStatus.text           = "¡Listo para iniciar el paseo!"
                        btnStartWalk.isEnabled  = true
                        btnFinishWalk.isEnabled = false
                        tvLocationStatus.text   = "📍 GPS listo"
                    }
                    "active" -> {
                        tvStatus.text           = "🐕 Paseo en curso"
                        btnStartWalk.isEnabled  = false
                        btnFinishWalk.isEnabled = true
                        startLocationUpdates()
                    }
                }
            }

        btnStartWalk.setOnClickListener {
            if (currentWalkId.isEmpty()) return@setOnClickListener
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST)
                return@setOnClickListener
            }
            db.collection("solicitudes").document(currentWalkId)
                .update(mapOf(
                    "status"    to "active",
                    "startTime" to System.currentTimeMillis()
                ))
                .addOnSuccessListener {
                    Toast.makeText(this, "¡Paseo iniciado!", Toast.LENGTH_SHORT).show()
                    startLocationUpdates()
                }
        }

        btnFinishWalk.setOnClickListener {
            if (currentWalkId.isEmpty()) return@setOnClickListener
            stopLocationUpdates()
            db.collection("solicitudes").document(currentWalkId)
                .update(mapOf(
                    "status"  to "finished",
                    "endTime" to System.currentTimeMillis()
                ))
                .addOnSuccessListener {
                    rejectOtherPendingRequests()
                    Toast.makeText(this,
                        "✓ Paseo finalizado",
                        Toast.LENGTH_SHORT).show()

                    // FIX: Paseador va a calificar al dueño
                    val intent = Intent(this, RatingActivity::class.java)
                    intent.putExtra("walk_id",   currentWalkId)
                    intent.putExtra("owner_id",  currentOwnerId)
                    intent.putExtra("walker_id", session.getUserId())
                    startActivity(intent)
                    finish()
                }
        }
    }

    private fun rejectOtherPendingRequests() {
        db.collection("solicitudes")
            .whereEqualTo("walkerId", session.getUserId())
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { docs ->
                docs.documents.forEach { doc ->
                    db.collection("solicitudes").document(doc.id)
                        .update("status", "rejected")
                }
            }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 5000L
        ).setMinUpdateIntervalMillis(3000L).build()
        fusedLocationClient.requestLocationUpdates(
            locationRequest, locationCallback, Looper.getMainLooper()
        )
    }

    private fun stopLocationUpdates() {
        if (::fusedLocationClient.isInitialized)
            fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            findViewById<MaterialButton>(R.id.btnStartWalk).performClick()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }
}