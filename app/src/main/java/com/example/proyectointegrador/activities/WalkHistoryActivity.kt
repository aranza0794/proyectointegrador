package com.example.proyectointegrador.activities

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectointegrador.R
import com.example.proyectointegrador.adapters.WalkHistoryAdapter
import com.example.proyectointegrador.models.WalkHistory
import com.example.proyectointegrador.utils.SessionManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class WalkHistoryActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_walk_history)

        session = SessionManager(this)

        val toolbar       = findViewById<Toolbar>(R.id.toolbar)
        val rvWalkHistory = findViewById<RecyclerView>(R.id.rvWalkHistory)
        val layoutEmpty   = findViewById<LinearLayout>(R.id.layoutEmpty)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Historial de paseos"

        rvWalkHistory.layoutManager = LinearLayoutManager(this)

        val userType = session.getUserType()
        val userId   = session.getUserId()
        val field    = if (userType == "owner") "ownerId" else "walkerId"

        db.collection("solicitudes")
            .whereEqualTo(field, userId)
            .whereEqualTo("status", "completed")
            .orderBy("endTime", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { docs ->
                val historyList = docs.documents.map { doc ->
                    WalkHistory(
                        id              = doc.id,
                        ownerId         = doc.getString("ownerId")         ?: "",
                        walkerId        = doc.getString("walkerId")        ?: "",
                        dogName         = doc.getString("dogName")         ?: "",
                        walkerName      = doc.getString("walkerName")      ?: "",
                        ownedName       = doc.getString("ownedName")       ?: "",
                        durationMinutes = (doc.getLong("durationMinutes")  ?: 0L).toInt(),
                        cost            = doc.getDouble("cost")            ?: 0.0,
                        paymentMethod   = doc.getString("paymentMethod")   ?: "",
                        status          = doc.getString("status")          ?: "",
                        ratingStars     = (doc.getLong("ratingStars")      ?: 0L).toInt(),
                        endTime         = doc.getLong("endTime")           ?: 0L
                    )
                }

                if (historyList.isEmpty()) {
                    layoutEmpty.visibility   = View.VISIBLE
                    rvWalkHistory.visibility = View.GONE
                } else {
                    layoutEmpty.visibility   = View.GONE
                    rvWalkHistory.visibility = View.VISIBLE
                    rvWalkHistory.adapter    = WalkHistoryAdapter(historyList, userType)
                }
            }
            .addOnFailureListener {
                layoutEmpty.visibility   = View.VISIBLE
                rvWalkHistory.visibility = View.GONE
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}