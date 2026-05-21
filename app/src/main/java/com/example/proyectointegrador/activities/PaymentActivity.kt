package com.example.proyectointegrador.activities

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.proyectointegrador.R
import com.example.proyectointegrador.utils.SessionManager
import com.google.firebase.firestore.FirebaseFirestore

class PaymentActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var session: SessionManager
    private var walkId   = ""
    private var walkerId = ""
    private var ownerId  = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        session = SessionManager(this)
        walkId  = intent.getStringExtra("walk_id") ?: ""

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Método de pago"
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Toast.makeText(this@PaymentActivity,
                    "Por favor selecciona un método de pago",
                    Toast.LENGTH_SHORT).show()
            }
        })

        val tvAmount    = findViewById<TextView>(R.id.tvAmount)
        val tvDuration  = findViewById<TextView>(R.id.tvDuration)
        val btnCash     = findViewById<LinearLayout>(R.id.btnCash)
        val btnTransfer = findViewById<LinearLayout>(R.id.btnTransfer)

        db.collection("solicitudes").document(walkId)
            .get()
            .addOnSuccessListener { doc ->
                val cost         = doc.getDouble("cost") ?: 0.0
                val duration     = doc.getLong("durationMinutes") ?: 0L
                val startTime    = doc.getLong("startTime") ?: 0L
                val endTime      = doc.getLong("endTime") ?: 0L
                val savedPayment = doc.getString("paymentMethod") ?: ""
                walkerId         = doc.getString("walkerId") ?: ""
                ownerId          = doc.getString("ownerId") ?: ""

                val realMinutes = if (startTime > 0 && endTime > 0)
                    ((endTime - startTime) / 1000 / 60) else duration

                tvAmount.text   = "$${String.format("%.0f", cost)}"
                tvDuration.text = "Duración: $realMinutes minutos"

                when (savedPayment) {
                    "cash"     -> { btnCash.alpha = 1f;   btnTransfer.alpha = 0.5f }
                    "transfer" -> { btnCash.alpha = 0.5f; btnTransfer.alpha = 1f   }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar datos", Toast.LENGTH_SHORT).show()
            }

        btnCash.setOnClickListener     { confirmPayment("cash") }
        btnTransfer.setOnClickListener { confirmPayment("transfer") }
    }

    private fun confirmPayment(method: String) {
        db.collection("solicitudes").document(walkId)
            .update("paymentMethod", method)
            .addOnSuccessListener {
                val msg = if (method == "cash")
                    "✓ Pago en efectivo registrado"
                else "✓ Transferencia registrada"
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

                // FIX: pasar owner_id y walker_id para que el dueño califique al paseador
                val intent = Intent(this, RatingActivity::class.java)
                intent.putExtra("walk_id",   walkId)
                intent.putExtra("walker_id", walkerId)
                intent.putExtra("owner_id",  ownerId)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al registrar pago", Toast.LENGTH_SHORT).show()
            }
    }
}