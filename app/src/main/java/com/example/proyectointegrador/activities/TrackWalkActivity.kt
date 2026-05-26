package com.example.proyectointegrador.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectointegrador.R
import com.example.proyectointegrador.utils.SessionManager
import com.google.android.material.chip.Chip
import com.google.firebase.firestore.FirebaseFirestore
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class TrackWalkActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var session: SessionManager
    private lateinit var mapView: MapView
    private var walkerMarker: Marker? = null
    private var currentWalkId      = ""
    private var currentStatus      = ""
    private var lastNotifiedStatus = ""
    private val mexicoCenter       = GeoPoint(23.6345, -102.5528)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = packageName
        setContentView(R.layout.activity_track_walk)
        session = SessionManager(this)

        val tvStatus     = findViewById<TextView>(R.id.tvStatus)
        val tvWalkerName = findViewById<TextView>(R.id.tvWalkerName)
        val tvDogInfo    = findViewById<TextView>(R.id.tvDogInfo)
        val chipLive     = findViewById<Chip>(R.id.chipLive)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentStatus == "active") {
                    AlertDialog.Builder(this@TrackWalkActivity)
                        .setTitle("¿Salir del seguimiento?")
                        .setMessage("El paseo sigue activo. Puedes volver desde el dashboard.")
                        .setPositiveButton("Sí, salir") { _, _ -> goToDashboard() }
                        .setNegativeButton("Quedarse", null)
                        .show()
                } else goToDashboard()
            }
        })

        mapView = findViewById(R.id.mapView)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(5.0)
        mapView.controller.setCenter(mexicoCenter)

        // Buscar solicitud activa del dueño
        db.collection("solicitudes")
            .whereEqualTo("ownerId", session.getUserId())
            .get()
            .addOnSuccessListener { docs ->
                val doc = docs.documents.firstOrNull {
                    val s = it.getString("status") ?: ""
                    s == "pending" || s == "accepted" || s == "active"
                } ?: run {
                    tvStatus.text = "No hay paseo activo"
                    return@addOnSuccessListener
                }

                currentWalkId = doc.id
                val walkerId  = doc.getString("walkerId") ?: ""
                val dogName   = doc.getString("dogName")  ?: ""
                val duration  = doc.getLong("durationMinutes") ?: 0L

                tvDogInfo.text = "$dogName • $duration min"

                if (walkerId.isNotEmpty()) {
                    db.collection("usuarios").document(walkerId).get()
                        .addOnSuccessListener { w ->
                            tvWalkerName.text = "Paseador: ${w.getString("name") ?: "—"}"
                        }
                }

                // Listener en tiempo real
                db.collection("solicitudes").document(currentWalkId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null || snapshot == null) return@addSnapshotListener

                        val status = snapshot.getString("status") ?: ""
                        // FIX: leer coordenadas del paseador
                        val lat = snapshot.getDouble("walkerLat") ?: 0.0
                        val lng = snapshot.getDouble("walkerLng") ?: 0.0
                        currentStatus = status

                        if (status != lastNotifiedStatus) {
                            when (status) {
                                "accepted" -> Toast.makeText(this,
                                    "✅ ¡El paseador aceptó!", Toast.LENGTH_LONG).show()
                                "active"   -> Toast.makeText(this,
                                    "🐕 ¡El paseo inició!", Toast.LENGTH_LONG).show()
                            }
                            lastNotifiedStatus = status
                        }

                        when (status) {
                            "pending" -> {
                                tvStatus.text       = "⏳ Esperando paseador..."
                                chipLive.visibility = View.GONE
                            }
                            "accepted" -> {
                                tvStatus.text       = "✅ Paseador en camino"
                                chipLive.visibility = View.GONE
                            }
                            "active" -> {
                                tvStatus.text       = "🐕 Paseo en curso"
                                chipLive.visibility = View.VISIBLE
                                // FIX: actualizar marcador con coordenadas reales
                                if (lat != 0.0 && lng != 0.0) {
                                    updateWalkerOnMap(lat, lng)
                                }
                            }
                            // FIX: "completed" en lugar de "finished"
                            "completed" -> {
                                tvStatus.text       = "🎉 ¡Paseo finalizado!"
                                chipLive.visibility = View.GONE
                                startActivity(Intent(this, PaymentActivity::class.java).apply {
                                    putExtra("walk_id",   currentWalkId)
                                    putExtra("walker_id", walkerId)
                                    putExtra("owner_id",  session.getUserId())
                                })
                                finish()
                            }
                            "cancelled", "rejected" -> {
                                tvStatus.text       = "❌ Solicitud cancelada"
                                chipLive.visibility = View.GONE
                            }
                        }
                    }
            }
            .addOnFailureListener {
                tvStatus.text = "⚠️ Sin conexión"
            }
    }

    private fun updateWalkerOnMap(lat: Double, lng: Double) {
        val point = GeoPoint(lat, lng)
        if (walkerMarker != null) {
            walkerMarker!!.position = point
        } else {
            walkerMarker = Marker(mapView).apply {
                position = point
                title    = "🦺 Paseador"
                snippet  = "Ubicación en tiempo real"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            mapView.overlays.add(walkerMarker)
        }
        if (mapView.zoomLevelDouble < 14.0) mapView.controller.setZoom(16.0)
        mapView.controller.animateTo(point)
        mapView.invalidate()
    }

    private fun goToDashboard() {
        startActivity(Intent(this, OwnerDashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        })
        finish()
    }

    override fun onResume()  { super.onResume();  mapView.onResume()  }
    override fun onPause()   { super.onPause();   mapView.onPause()   }
    override fun onDestroy() { super.onDestroy(); mapView.onDetach()  }
}