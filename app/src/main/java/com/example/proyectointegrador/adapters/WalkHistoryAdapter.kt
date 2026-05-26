package com.example.proyectointegrador.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectointegrador.R
import com.example.proyectointegrador.models.WalkHistory
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WalkHistoryAdapter(
    private val history: List<WalkHistory>,
    private val userType: String
) : RecyclerView.Adapter<WalkHistoryAdapter.HistoryViewHolder>() {

    private val db         = FirebaseFirestore.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDogName:    TextView  = itemView.findViewById(R.id.tvDogName)
        val tvWalkerName: TextView  = itemView.findViewById(R.id.tvWalkerName)
        val tvDate:       TextView  = itemView.findViewById(R.id.tvDate)
        val tvDuration:   TextView  = itemView.findViewById(R.id.tvDuration)
        val tvCost:       TextView  = itemView.findViewById(R.id.tvCost)
        val tvStatus:     TextView  = itemView.findViewById(R.id.tvStatus)
        val ratingBar:    RatingBar = itemView.findViewById(R.id.ratingBarItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_walk_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = history[position]

        holder.tvDogName.text   = item.dogName
        holder.tvDate.text      = if (item.endTime > 0)
            dateFormat.format(Date(item.endTime)) else "—"
        holder.tvDuration.text  = "${item.durationMinutes} min"
        holder.tvCost.text      = "$${String.format("%.0f", item.cost)}"
        holder.ratingBar.rating = item.ratingStars.toFloat()

        // Estado con color y padding para que se vea el badge
        holder.tvStatus.setPadding(16, 6, 16, 6)
        when (item.status) {
            "completed" -> {
                holder.tvStatus.text = "✓ Completado"
                holder.tvStatus.setBackgroundColor(Color.parseColor("#2A9D8F"))
                holder.tvStatus.setTextColor(Color.WHITE)
            }
            "cancelled" -> {
                holder.tvStatus.text = "✗ Cancelado"
                holder.tvStatus.setBackgroundColor(Color.parseColor("#E63946"))
                holder.tvStatus.setTextColor(Color.WHITE)
            }
            else -> {
                holder.tvStatus.text = item.status
                holder.tvStatus.setBackgroundColor(Color.parseColor("#F4A261"))
                holder.tvStatus.setTextColor(Color.WHITE)
            }
        }

        // FIX: Nombre según tipo de usuario — cargar desde Firestore si no está en el modelo
        if (userType == "walker") {
            // Paseador ve el nombre del dueño
            if (item.ownedName.isNotEmpty()) {
                holder.tvWalkerName.text = "Dueño: ${item.ownedName}"
            } else if (item.ownerId.isNotEmpty()) {
                // Buscar en Firestore
                holder.tvWalkerName.text = "Cargando..."
                db.collection("usuarios").document(item.ownerId).get()
                    .addOnSuccessListener { doc ->
                        val name = doc.getString("name") ?: "Desconocido"
                        holder.tvWalkerName.text = "Dueño: $name"
                    }
                    .addOnFailureListener {
                        holder.tvWalkerName.text = "Dueño no encontrado"
                    }
            } else {
                holder.tvWalkerName.text = "Dueño no registrado"
            }
        } else {
            // Dueño ve el nombre del paseador
            if (item.walkerName.isNotEmpty()) {
                holder.tvWalkerName.text = "Paseador: ${item.walkerName}"
            } else if (item.walkerId.isNotEmpty()) {
                holder.tvWalkerName.text = "Cargando..."
                db.collection("usuarios").document(item.walkerId).get()
                    .addOnSuccessListener { doc ->
                        val name = doc.getString("name") ?: "Desconocido"
                        holder.tvWalkerName.text = "Paseador: $name"
                    }
                    .addOnFailureListener {
                        holder.tvWalkerName.text = "Paseador no encontrado"
                    }
            } else {
                holder.tvWalkerName.text = "Sin paseador asignado"
            }
        }
    }

    override fun getItemCount() = history.size
}