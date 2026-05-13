package com.example.proyectointegrador.activities

import android.content.Intent
import android.os.Bundle
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectointegrador.R
import com.google.android.material.button.MaterialButton

class RequestWalkActivity : AppCompatActivity() {

    private val pricePerMinute = 0.5
    private var selectedMinutes = 30

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_request_walk)

        val seekBarDuration = findViewById<SeekBar>(R.id.seekBarDuration)
        val tvDuration = findViewById<TextView>(R.id.tvDuration)
        val tvCost = findViewById<TextView>(R.id.tvCost)
        val btnSeeWalkers = findViewById<MaterialButton>(R.id.btnSeeWalkers)

        // Inicializar valores
        updateDisplay(tvDuration, tvCost, selectedMinutes)

        seekBarDuration.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                selectedMinutes = if (progress < 15) 15 else progress
                updateDisplay(tvDuration, tvCost, selectedMinutes)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnSeeWalkers.setOnClickListener {
            val cost = selectedMinutes * pricePerMinute
            val intent = Intent(this, AvailableWalkersActivity::class.java)
            intent.putExtra("duration", selectedMinutes)
            intent.putExtra("cost", cost)
            startActivity(intent)
        }
    }

    private fun updateDisplay(tvDuration: TextView, tvCost: TextView, minutes: Int) {
        val cost = minutes * pricePerMinute
        tvDuration.text = "$minutes minutos"
        tvCost.text = "$${String.format("%.2f", cost)}"
    }
}