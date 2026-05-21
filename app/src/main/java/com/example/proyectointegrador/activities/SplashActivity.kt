package com.example.proyectointegrador.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectointegrador.R
import com.example.proyectointegrador.utils.SessionManager
import com.google.firebase.firestore.FirebaseFirestore

class SplashActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val session = SessionManager(this)

        if (!session.isLoggedIn()) {
            // Sin sesión → Login
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val userType = session.getUserType()
        val userId   = session.getUserId()

        if (userType == "owner") {
            // FIX: Verificar si hay paseo activo antes de ir al dashboard
            db.collection("solicitudes")
                .whereEqualTo("ownerId", userId)
                .get()
                .addOnSuccessListener { docs ->
                    val activeDoc = docs.documents.firstOrNull { doc ->
                        val s = doc.getString("status") ?: ""
                        s == "active"
                    }
                    val acceptedDoc = docs.documents.firstOrNull { doc ->
                        val s = doc.getString("status") ?: ""
                        s == "accepted" || s == "pending"
                    }

                    when {
                        activeDoc != null -> {
                            // Hay paseo activo → ir al mapa directamente
                            startActivity(Intent(this, TrackWalkActivity::class.java))
                        }
                        else -> {
                            // Sin paseo activo → dashboard normal
                            startActivity(Intent(this, OwnerDashboardActivity::class.java))
                        }
                    }
                    finish()
                }
                .addOnFailureListener {
                    // Error de red → ir al dashboard normal
                    startActivity(Intent(this, OwnerDashboardActivity::class.java))
                    finish()
                }
        } else {
            // Paseador — verificar si tiene paseo activo
            db.collection("solicitudes")
                .whereEqualTo("walkerId", userId)
                .get()
                .addOnSuccessListener { docs ->
                    val activeDoc = docs.documents.firstOrNull { doc ->
                        val s = doc.getString("status") ?: ""
                        s == "active" || s == "accepted"
                    }

                    if (activeDoc != null) {
                        // Hay paseo activo → ir directamente
                        startActivity(Intent(this, ActiveWalkActivity::class.java))
                    } else {
                        startActivity(Intent(this, WalkerDashboardActivity::class.java))
                    }
                    finish()
                }
                .addOnFailureListener {
                    startActivity(Intent(this, WalkerDashboardActivity::class.java))
                    finish()
                }
        }
    }
}