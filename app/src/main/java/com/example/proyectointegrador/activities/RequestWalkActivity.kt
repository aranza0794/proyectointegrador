package com.example.proyectointegrador.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
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

        val cardNoDog        = findViewById<MaterialCardView>(R.id.cardNoDog)
        val cardDogInfo      = findViewById<MaterialCardView>(R.id.cardDogInfo)
        val tvDogName        = findViewById<TextView>(R.id.tvDogName)
        val tvDogBreed       = findViewById<TextView>(R.id.tvDogBreed)
        val chipGroupDogs    = findViewById<ChipGroup>(R.id.chipGroupDogs)
        val seekBarDuration  = findViewById<SeekBar>(R.id.seekBarDuration)
        val tvDuration       = findViewById<TextView>(R.id.tvDuration)
        val tvCost           = findViewById<TextView>(R.id.tvCost)
        val chipGroupPayment = findViewById<ChipGroup>(R.id.chipGroupPayment)
        val btnSeeWalkers    = findViewById<MaterialButton>(R.id.btnSeeWalkers)

        // Inicializar display
        updateDisplay(tvDuration, tvCost, selectedMinutes)

        // FIX: Cargar TODOS los perros del dueño
        loadDogs(cardNoDog, cardDogInfo, tvDogName, tvDogBreed, chipGroupDogs)

        // SeekBar con mínimo 30 minutos
        seekBarDuration.max      = maxMinutes - minMinutes
        seekBarDuration.progress = 0
        seekBarDuration.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                selectedMinutes = minMinutes + progress
                updateDisplay(tvDuration, tvCost, selectedMinutes)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Método de pago
        chipGroupPayment.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedPayment = when {
                checkedIds.contains(R.id.chipCash)     -> "cash"
                checkedIds.contains(R.id.chipTransfer) -> "transfer"
                else -> "cash"
            }
        }

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
        cardNoDog: MaterialCardView,
        cardDogInfo: MaterialCardView,
        tvDogName: TextView,
        tvDogBreed: TextView,
        chipGroupDogs: ChipGroup
    ) {
        db.collection("perros")
            .whereEqualTo("ownerId", session.getUserId())
            .get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) {
                    // Buscar sin filtro por si ownerId está vacío
                    db.collection("perros").get()
                        .addOnSuccessListener { allDogs ->
                            if (allDogs.isEmpty) {
                                cardNoDog.visibility   = View.VISIBLE
                                cardDogInfo.visibility = View.GONE
                                chipGroupDogs.visibility = View.GONE
                            } else {
                                setupDogSelector(
                                    allDogs.documents, cardNoDog, cardDogInfo,
                                    tvDogName, tvDogBreed, chipGroupDogs
                                )
                            }
                        }
                } else {
                    setupDogSelector(
                        docs.documents, cardNoDog, cardDogInfo,
                        tvDogName, tvDogBreed, chipGroupDogs
                    )
                }
            }
    }

    private fun setupDogSelector(
        dogs: List<com.google.firebase.firestore.DocumentSnapshot>,
        cardNoDog: MaterialCardView,
        cardDogInfo: MaterialCardView,
        tvDogName: TextView,
        tvDogBreed: TextView,
        chipGroupDogs: ChipGroup
    ) {
        cardNoDog.visibility = View.GONE

        if (dogs.size == 1) {
            // Solo un perro — seleccionarlo automáticamente
            val dog = dogs.first()
            selectDog(dog, tvDogName, tvDogBreed, cardDogInfo)
            chipGroupDogs.visibility = View.GONE
        } else {
            // Varios perros — mostrar chips para elegir
            cardDogInfo.visibility   = View.VISIBLE
            chipGroupDogs.visibility = View.VISIBLE

            dogs.forEach { dog ->
                val name = dog.getString("name") ?: ""
                val chip = Chip(this).apply {
                    text       = "🐕 $name"
                    isCheckable = true
                    textSize   = 14f
                }
                chip.setOnClickListener {
                    selectDog(dog, tvDogName, tvDogBreed, cardDogInfo)
                }
                chipGroupDogs.addView(chip)
            }

            // Seleccionar el primero por defecto
            selectDog(dogs.first(), tvDogName, tvDogBreed, cardDogInfo)
            (chipGroupDogs.getChildAt(0) as? Chip)?.isChecked = true
        }
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