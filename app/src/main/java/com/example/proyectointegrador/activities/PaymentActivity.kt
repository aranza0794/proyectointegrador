package com.example.proyectointegrador.activities

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.proyectointegrador.R
import com.example.proyectointegrador.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.firestore.FirebaseFirestore

class PaymentActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var session: SessionManager
    private var walkId   = ""
    private var walkerId = ""
    private var ownerId  = ""
    private var selectedPayment = "cash"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        session  = SessionManager(this)
        walkId   = intent.getStringExtra("walk_id")   ?: ""
        walkerId = intent.getStringExtra("walker_id") ?: ""
        ownerId  = intent.getStringExtra("owner_id")  ?: session.getUserId()

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Pago del paseo"

        // Bloquear back
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Toast.makeText(this@PaymentActivity,
                    "Por favor confirma el pago primero", Toast.LENGTH_SHORT).show()
            }
        })

        val tvTotal      = findViewById<TextView>(R.id.tvTotal)
        val tvDuration   = findViewById<TextView>(R.id.tvDuration)
        val tvWalkerName = findViewById<TextView>(R.id.tvWalkerName)
        val btnCash      = findViewById<MaterialCardView>(R.id.btnCash)
        val btnTransfer  = findViewById<MaterialCardView>(R.id.btnTransfer)
        val btnConfirm   = findViewById<MaterialButton>(R.id.btnConfirm)

        if (walkId.isEmpty()) {
            Toast.makeText(this, "Error: paseo no encontrado", Toast.LENGTH_SHORT).show()
            goToRating()
            return
        }

        // Cargar datos de la solicitud
        db.collection("solicitudes").document(walkId).get()
            .addOnSuccessListener { doc ->
                val cost          = doc.getDouble("cost")           ?: 0.0
                val duration      = doc.getLong("durationMinutes")  ?: 0L
                val startTime     = doc.getLong("startTime")        ?: 0L
                val endTime       = doc.getLong("endTime")          ?: 0L
                val walkerIdDoc   = doc.getString("walkerId")       ?: walkerId
                val savedPayment  = doc.getString("paymentMethod")  ?: "cash"

                if (walkerIdDoc.isNotEmpty()) walkerId = walkerIdDoc

                val realMinutes = if (startTime > 0 && endTime > 0)
                    ((endTime - startTime) / 1000 / 60) else duration

                tvTotal.text    = "$${String.format("%.0f", cost)}"
                tvDuration.text = "$realMinutes minutos"

                // Cargar nombre del paseador
                if (walkerId.isNotEmpty()) {
                    db.collection("usuarios").document(walkerId).get()
                        .addOnSuccessListener { w ->
                            tvWalkerName.text = w.getString("name") ?: "—"
                        }
                }

                // Selección de pago
                selectedPayment = savedPayment
                updatePaymentUI(btnCash, btnTransfer, selectedPayment)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar datos", Toast.LENGTH_SHORT).show()
            }

        btnCash.setOnClickListener {
            selectedPayment = "cash"
            updatePaymentUI(btnCash, btnTransfer, "cash")
        }
        btnTransfer.setOnClickListener {
            selectedPayment = "transfer"
            updatePaymentUI(btnCash, btnTransfer, "transfer")
        }

        btnConfirm.setOnClickListener {
            btnConfirm.isEnabled = false
            btnConfirm.text      = "Procesando..."

            db.collection("solicitudes").document(walkId)
                .update("paymentMethod", selectedPayment)
                .addOnSuccessListener {
                    Toast.makeText(this, "✓ Pago confirmado", Toast.LENGTH_SHORT).show()
                    goToRating()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al confirmar", Toast.LENGTH_SHORT).show()
                    btnConfirm.isEnabled = true
                    btnConfirm.text      = "✓  Confirmar pago"
                }
        }
    }

    private fun updatePaymentUI(btnCash: MaterialCardView, btnTransfer: MaterialCardView, method: String) {
        if (method == "cash") {
            btnCash.setCardBackgroundColor(android.graphics.Color.parseColor("#E0F5F3"))
            btnCash.strokeColor = android.graphics.Color.parseColor("#2A9D8F")
            btnCash.strokeWidth = 6
            btnTransfer.setCardBackgroundColor(android.graphics.Color.WHITE)
            btnTransfer.strokeColor = android.graphics.Color.parseColor("#E0E0E0")
            btnTransfer.strokeWidth = 3
        } else {
            btnTransfer.setCardBackgroundColor(android.graphics.Color.parseColor("#E0F5F3"))
            btnTransfer.strokeColor = android.graphics.Color.parseColor("#2A9D8F")
            btnTransfer.strokeWidth = 6
            btnCash.setCardBackgroundColor(android.graphics.Color.WHITE)
            btnCash.strokeColor = android.graphics.Color.parseColor("#E0E0E0")
            btnCash.strokeWidth = 3
        }
    }

    private fun goToRating() {
        startActivity(Intent(this, RatingActivity::class.java).apply {
            putExtra("walk_id",   walkId)
            putExtra("walker_id", walkerId)
            putExtra("owner_id",  ownerId)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        })
        finish()
    }
}