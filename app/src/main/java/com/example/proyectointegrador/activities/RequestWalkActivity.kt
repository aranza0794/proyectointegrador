package com.example.proyectointegrador.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.proyectointegrador.R
import com.example.proyectointegrador.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.firestore.FirebaseFirestore

class RequestWalkActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var session: SessionManager

    // FIX: precio mínimo $30 por 30 minutos
    private val minMinutes   = 30
    private val maxMinutes   = 120
    private val pricePerMin  = 1.0  // $30 / 30min = $1 por minuto
    private var selectedMinutes = 30
    private var selectedPayment = "cash"

    // Perro seleccionado
    private var selectedDogId      = ""
    private var selectedDogName    = ""
    private var selectedDogBreed   = ""
    private var selectedDogSize    = ""
    private var selectedDogAge     = 0
    private var selectedDogAllergy = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_request_walk)

        session = SessionManager(this)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Solicitar paseo"

        val cardDogInfo      = findViewById<MaterialCardView>(R.id.cardDogInfo)
        val tvDogName        = findViewById<TextView>(R.id.tvDogName)
        val tvDogBreed       = findViewById<TextView>(R.id.tvDogBreed)
        val chipGroupDogs    = findViewById<ChipGroup>(R.id.chipGroupDogs)
        val tvPrice          = findViewById<TextView>(R.id.tvPrice)
        val tvCustomDuration = findViewById<TextView>(R.id.tvCustomDuration)
        val btn30            = findViewById<com.google.android.material.button.MaterialButton>(R.id.btn30)
        val btn45            = findViewById<com.google.android.material.button.MaterialButton>(R.id.btn45)
        val btn60            = findViewById<com.google.android.material.button.MaterialButton>(R.id.btn60)
        val btn90            = findViewById<com.google.android.material.button.MaterialButton>(R.id.btn90)
        val btnCash          = findViewById<com.google.android.material.button.MaterialButton>(R.id.chipCash)
        val btnTransfer      = findViewById<com.google.android.material.button.MaterialButton>(R.id.chipTransfer)
        val btnSeeWalkers    = findViewById<MaterialButton>(R.id.btnRequestWalk)

        // FIX: Cargar TODOS los perros del dueño
        loadDogs(cardDogInfo, tvDogName, tvDogBreed, chipGroupDogs)

        // FIX: Botones de tiempo predefinidos
        val timeButtons = listOf(btn30, btn45, btn60, btn90)
        val timeMins    = listOf(30, 45, 60, 90)

        fun selectTime(minutes: Int) {
            selectedMinutes = minutes
            val cost = minutes * pricePerMin
            tvPrice.text = "$$cost"
            tvCustomDuration.text = "$minutes minutos seleccionados · $$cost MXN"
            timeButtons.forEachIndexed { i, btn ->
                if (timeMins[i] == minutes) {
                    btn.backgroundTintList = android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#2A9D8F"))
                    btn.setTextColor(android.graphics.Color.WHITE)
                } else {
                    btn.backgroundTintList = android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#F0F4F4"))
                    btn.setTextColor(android.graphics.Color.parseColor("#666666"))
                }
            }
        }

        btn30.setOnClickListener { selectTime(30) }
        btn45.setOnClickListener { selectTime(45) }
        btn60.setOnClickListener { selectTime(60) }
        btn90.setOnClickListener { selectTime(90) }
        selectTime(30)

        // Método de pago
        fun selectPayment(method: String) {
            selectedPayment = method
            if (method == "cash") {
                btnCash.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#2A9D8F"))
                btnCash.setTextColor(android.graphics.Color.WHITE)
                btnTransfer.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#F0F4F4"))
                btnTransfer.setTextColor(android.graphics.Color.parseColor("#666666"))
            } else {
                btnTransfer.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#2A9D8F"))
                btnTransfer.setTextColor(android.graphics.Color.WHITE)
                btnCash.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#F0F4F4"))
                btnCash.setTextColor(android.graphics.Color.parseColor("#666666"))
            }
        }
        btnCash.setOnClickListener { selectPayment("cash") }
        btnTransfer.setOnClickListener { selectPayment("transfer") }
        selectPayment("cash")

        btnSeeWalkers.setOnClickListener {
            if (selectedDogId.isEmpty()) {
                Toast.makeText(this, "Selecciona a tu perro", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val cost = selectedMinutes * pricePerMin
            val intent = Intent(this, AvailableWalkersActivity::class.java)
            intent.putExtra("duration",      selectedMinutes)
            intent.putExtra("cost",          cost)
            intent.putExtra("paymentMethod", selectedPayment)
            intent.putExtra("dogId",         selectedDogId)
            intent.putExtra("dogName",       selectedDogName)
            intent.putExtra("dogBreed",      selectedDogBreed)
            intent.putExtra("dogSize",       selectedDogSize)
            intent.putExtra("dogAge",        selectedDogAge)
            intent.putExtra("dogAllergy",    selectedDogAllergy)
            startActivity(intent)
        }
    }

    private fun loadDogs(
        cardDogInfo: MaterialCardView,
        tvDogName: TextView,
        tvDogBreed: TextView,
        chipGroupDogs: ChipGroup
    ) {
        // FIX: Solo cargar perros del dueño actual, sin fallback global
        val ownerId = session.getUserId()
        if (ownerId.isEmpty()) {
            Toast.makeText(this, "Error de sesión", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("perros")
            .whereEqualTo("ownerId", ownerId)
            .get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) {
                    // El dueño no tiene perros registrados
                    cardDogInfo.visibility   = View.GONE
                    chipGroupDogs.visibility = View.GONE
                    Toast.makeText(this,
                        "Primero registra a tu perro 🐕",
                        Toast.LENGTH_LONG).show()
                } else {
                    setupDogSelector(
                        docs.documents, cardDogInfo,
                        tvDogName, tvDogBreed, chipGroupDogs
                    )
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar perros", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupDogSelector(
        dogs: List<com.google.firebase.firestore.DocumentSnapshot>,
        cardDogInfo: MaterialCardView,
        tvDogName: TextView,
        tvDogBreed: TextView,
        chipGroupDogs: ChipGroup
    ) {
        // FIX: Siempre mostrar chips sin importar cuántos perros haya
        cardDogInfo.visibility   = View.VISIBLE
        chipGroupDogs.visibility = View.VISIBLE
        chipGroupDogs.removeAllViews()
        chipGroupDogs.isSingleSelection   = true
        chipGroupDogs.isSelectionRequired = true

        dogs.forEachIndexed { index, dog ->
            val name = dog.getString("name") ?: "Perro ${index + 1}"
            val chip = com.google.android.material.chip.Chip(this).apply {
                text        = "🐕 $name"
                isCheckable = true
                textSize    = 14f
                chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#E8F0FB"))
                setTextColor(android.graphics.Color.parseColor("#1B3A6B"))
                chipStrokeColor = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#1B3A6B"))
                chipStrokeWidth = 1.5f
                isChecked = (index == 0)
            }
            chip.setOnCheckedChangeListener { _, checked ->
                if (checked) selectDog(dog, tvDogName, tvDogBreed, cardDogInfo)
            }
            chipGroupDogs.addView(chip)
        }
        // Seleccionar primero por defecto
        selectDog(dogs.first(), tvDogName, tvDogBreed, cardDogInfo)
    }

    private fun selectDog(
        dog: com.google.firebase.firestore.DocumentSnapshot,
        tvDogName: TextView,
        tvDogBreed: TextView,
        cardDogInfo: MaterialCardView
    ) {
        selectedDogId      = dog.id
        selectedDogName    = dog.getString("name") ?: ""
        selectedDogBreed   = dog.getString("breed") ?: ""
        selectedDogSize    = dog.getString("size") ?: ""
        selectedDogAge     = (dog.getLong("age") ?: 0L).toInt()
        selectedDogAllergy = dog.getString("allergy") ?: ""

        tvDogName.text         = selectedDogName
        tvDogBreed.text        = "$selectedDogBreed • $selectedDogSize • $selectedDogAge años"
        cardDogInfo.visibility = View.VISIBLE
    }

    private fun updateDisplay(tvDuration: TextView, tvCost: TextView, minutes: Int) {
        val cost = minutes * pricePerMin
        tvDuration.text = "$minutes minutos"
        tvCost.text     = "$${String.format("%.0f", cost)}"
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}