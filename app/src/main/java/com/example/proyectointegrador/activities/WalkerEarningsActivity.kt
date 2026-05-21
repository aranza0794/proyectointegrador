package com.example.proyectointegrador.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

// FIX: Ganancias eliminada — redirige al historial que ya tiene toda la info
class WalkerEarningsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, WalkHistoryActivity::class.java))
        finish()
    }
}