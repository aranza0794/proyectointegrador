package com.example.proyectointegrador.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.proyectointegrador.R
import com.example.proyectointegrador.utils.SessionManager
import com.google.firebase.firestore.FirebaseFirestore

class SplashActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        // FIX: Forzar modo claro ANTES de setContentView — evita pantalla negra
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val session = SessionManager(this)

        Handler(Looper.getMainLooper()).postDelayed({
            if (!session.isLoggedIn() || session.getUserId().isEmpty()) {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                return@postDelayed
            }

            db.collection("solicitudes")
                .whereEqualTo(
                    if (session.getUserType() == "walker") "walkerId" else "ownerId",
                    session.getUserId()
                )
                .whereIn("status", listOf("accepted", "active"))
                .get()
                .addOnSuccessListener { docs ->
                    val dest = when {
                        !docs.isEmpty && session.getUserType() == "walker" ->
                            ActiveWalkActivity::class.java
                        session.getUserType() == "walker" ->
                            WalkerDashboardActivity::class.java
                        else ->
                            OwnerDashboardActivity::class.java
                    }
                    startActivity(Intent(this, dest))
                    finish()
                }
                .addOnFailureListener {
                    val dest = if (session.getUserType() == "walker")
                        WalkerDashboardActivity::class.java
                    else OwnerDashboardActivity::class.java
                    startActivity(Intent(this, dest))
                    finish()
                }
        }, 1800)
    }
}