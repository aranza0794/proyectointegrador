package com.example.proyectointegrador.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.proyectointegrador.R
import com.example.proyectointegrador.utils.SessionManager

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val session = SessionManager(this)

        val destination = when {
            !session.isLoggedIn() -> LoginActivity::class.java
            session.getUserType() == "owner" -> OwnerDashboardActivity::class.java
            else -> WalkerDashboardActivity::class.java
        }

        startActivity(Intent(this, destination))
        finish()
    }
}