package com.example.proyectointegrador.activities

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectointegrador.R
import com.example.proyectointegrador.adapters.WalkHistoryAdapter
import com.example.proyectointegrador.models.WalkRequest
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

        val rvHistory = findViewById<RecyclerView>(R.id.rvHistory)
        val layoutEmpty = findViewById<LinearLayout>(R.id.layoutEmpty)

        rvHistory.layoutManager = LinearLayoutManager(this)

        db.collection("solicitudes")
            .whereEqualTo("ownerId", session.getUserId())
            .whereEqualTo("status", "finished")
            .orderBy("endTime", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val history = documents.map { doc ->
                    WalkRequest(
                        id = doc.id,
                        dogName = doc.getString("dogName") ?: "",
                        dogBreed = doc.getString("dogBreed") ?: "",
                        durationMinutes = (doc.getLong("durationMinutes") ?: 0L).toInt(),
                        cost = doc.getDouble("cost") ?: 0.0,
                        status = doc.getString("status") ?: "",
                        startTime = doc.getLong("startTime") ?: 0L,
                        endTime = doc.getLong("endTime") ?: 0L,
                        paymentMethod = doc.getString("paymentMethod") ?: ""
                    )
                }

                if (history.isEmpty()) {
                    layoutEmpty.visibility = View.VISIBLE
                    rvHistory.visibility = View.GONE
                } else {
                    layoutEmpty.visibility = View.GONE
                    rvHistory.visibility = View.VISIBLE
                    rvHistory.adapter = WalkHistoryAdapter(history)
                }
            }
    }
}