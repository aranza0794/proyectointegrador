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

class PaymentActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var session: SessionManager
    private var walkId = ""
    private var walkerId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        session = SessionManager(this)
        walkId = intent.getStringExtra("walk_id") ?: ""

        val tvAmount = findViewById<TextView>(R.id.tvAmount)
        val tvDuration = findViewById<TextView>(R.id.tvDuration)
        val btnCash = findViewById<MaterialButton>(R.id.btnCash)
        val btnTransfer = findViewById<MaterialButton>(R.id.btnTransfer)

        // Cargar datos del paseo
        db.collection("solicitudes").document(walkId)
            .get()
            .addOnSuccessListener { doc ->
                val cost = doc.getDouble("cost") ?: 0.0
                val duration = doc.getLong("durationMinutes") ?: 0L
                val startTime = doc.getLong("startTime") ?: 0L
                val endTime = doc.getLong("endTime") ?: 0L
                walkerId = doc.getString("walkerId") ?: ""

                // Calcular tiempo real si está disponible
                val realMinutes = if (startTime > 0 && endTime > 0)
                    ((endTime - startTime) / 1000 / 60)
                else duration

                tvAmount.text = "$${String.format("%.2f", cost)}"
                tvDuration.text = "Duración: $realMinutes minutos"
            }

        btnCash.setOnClickListener { registerPayment("cash") }
        btnTransfer.setOnClickListener { registerPayment("transfer") }
    }

    private fun registerPayment(method: String) {
        db.collection("solicitudes").document(walkId)
            .update("paymentMethod", method)
            .addOnSuccessListener {
                val msg = if (method == "cash")
                    "Pago en efectivo registrado ✓"
                else
                    "Transferencia registrada ✓"
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

                val intent = Intent(this, RatingActivity::class.java)
                intent.putExtra("walk_id", walkId)
                intent.putExtra("walker_id", walkerId)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al registrar pago", Toast.LENGTH_SHORT).show()
            }
    }
}