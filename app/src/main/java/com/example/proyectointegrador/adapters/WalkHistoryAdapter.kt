package com.example.proyectointegrador.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectointegrador.R
import com.example.proyectointegrador.models.WalkRequest
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WalkHistoryAdapter(
    private val history: List<WalkRequest>,
    private val db: FirebaseFirestore,
    private val userType: String = "owner"
) : RecyclerView.Adapter<WalkHistoryAdapter.HistoryViewHolder>() {

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDogName:     TextView  = itemView.findViewById(R.id.tvDogName)
        val tvDate:        TextView  = itemView.findViewById(R.id.tvDate)
        val tvDuration:    TextView  = itemView.findViewById(R.id.tvDuration)
        val tvCost:        TextView  = itemView.findViewById(R.id.tvCost)
        val tvPayment:     TextView  = itemView.findViewById(R.id.tvPayment)
        val tvWalkerName:  TextView  = itemView.findViewById(R.id.tvWalkerName)
        val ratingBarItem: RatingBar = itemView.findViewById(R.id.ratingBarItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_walk_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item       = history[position]
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        holder.tvDogName.text  = "${item.dogName} • ${item.dogBreed}"
        holder.tvDate.text     = if (item.endTime > 0)
            dateFormat.format(Date(item.endTime)) else "—"
        holder.tvDuration.text = "${item.durationMinutes} min"
        holder.tvCost.text     = "$${String.format("%.0f", item.cost)}"
        holder.tvPayment.text  = when (item.paymentMethod) {
            "cash"     -> "💵 Efectivo"
            "transfer" -> "💳 Transferencia"
            else       -> "—"
        }

        // FIX: Mostrar nombre del dueño si es paseador, nombre del paseador si es dueño
        if (userType == "walker") {
            // Paseador ve el nombre del dueño
            if (item.ownedName.isNotEmpty()) {
                holder.tvWalkerName.text = "Dueño: ${item.ownedName}"
            } else {
                holder.tvWalkerName.text = "Dueño: —"
            }
        } else {
            // Dueño ve el nombre del paseador
            if (item.walkerId.isNotEmpty()) {
                db.collection("usuarios").document(item.walkerId).get()
                    .addOnSuccessListener { doc ->
                        holder.tvWalkerName.text =
                            "Paseador: ${doc.getString("name") ?: "—"}"
                    }
            } else {
                holder.tvWalkerName.text = "Paseador: —"
            }
        }

        // Calificación dada
        db.collection("calificaciones")
            .whereEqualTo("walkRequestId", item.id)
            .whereEqualTo("ratedBy", getUserId(item))
            .get()
            .addOnSuccessListener { docs ->
                if (!docs.isEmpty) {
                    val stars = docs.first().getLong("stars") ?: 0L
                    holder.ratingBarItem.visibility = View.VISIBLE
                    holder.ratingBarItem.rating     = stars.toFloat()
                } else {
                    holder.ratingBarItem.visibility = View.GONE
                }
            }
    }

    private fun getUserId(item: WalkRequest): String {
        return if (userType == "owner") item.walkerId else item.ownedName
    }

    override fun getItemCount() = history.size
}