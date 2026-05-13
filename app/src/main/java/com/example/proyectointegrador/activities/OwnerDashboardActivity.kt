package com.example.proyectointegrador.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectointegrador.R
import com.example.proyectointegrador.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.LinearLayout

class OwnerDashboardActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_owner_dashboard)

        session = SessionManager(this)

        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        val btnRequestWalk = findViewById<MaterialButton>(R.id.btnRequestWalk)
        val btnMyDog = findViewById<LinearLayout>(R.id.btnMyDog)
        val btnHistory = findViewById<LinearLayout>(R.id.btnHistory)
        val btnLogout = findViewById<ImageButton>(R.id.btnLogout)

        tvWelcome.text = session.getUserName()

        btnRequestWalk.setOnClickListener {
            startActivity(Intent(this, RequestWalkActivity::class.java))
        }

        btnMyDog.setOnClickListener {
            startActivity(Intent(this, RegisterDogActivity::class.java))
        }

        btnHistory.setOnClickListener {
            startActivity(Intent(this, WalkHistoryActivity::class.java))
        }

        btnLogout.setOnClickListener {
            session.clearSession()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    // Al volver a la pantalla, verificar si hay paseo activo
    override fun onResume() {
        super.onResume()
        checkActiveWalk()
    }

    private fun checkActiveWalk() {
        db.collection("solicitudes")
            .whereEqualTo("ownerId", session.getUserId())
            .whereEqualTo("status", "active")
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    startActivity(Intent(this, TrackWalkActivity::class.java))
                }
            }
    }
}