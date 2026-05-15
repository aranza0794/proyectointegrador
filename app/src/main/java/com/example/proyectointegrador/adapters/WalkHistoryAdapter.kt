package com.example.proyectointegrador.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectointegrador.R
import com.example.proyectointegrador.models.WalkRequest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WalkHistoryAdapter(
    private val history: List<WalkRequest>
) : RecyclerView.Adapter<WalkHistoryAdapter.HistoryViewHolder>() {

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Estos IDs deben existir en item_walk_history.xml
        val tvDogName:  TextView = itemView.findViewById(R.id.tvDogName)
        val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        val tvCost:     TextView = itemView.findViewById(R.id.tvCost)
        // tvDate y tvPayment — si tu XML los tiene con otro nombre, cámbialos aquí
        val tvDate:     TextView? = itemView.findViewById(R.id.tvDate)
        val tvPayment:  TextView? = itemView.findViewById(R.id.tvPayment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_walk_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = history[position]
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        holder.tvDogName.text  = "${item.dogName} • ${item.dogBreed}"
        holder.tvDuration.text = "${item.durationMinutes} min"
        holder.tvCost.text     = "$${String.format("%.2f", item.cost)}"

        holder.tvDate?.text = if (item.endTime > 0)
            dateFormat.format(Date(item.endTime)) else "—"

        holder.tvPayment?.text = when (item.paymentMethod) {
            "cash"     -> "💵 Efectivo"
            "transfer" -> "💳 Transferencia"
            else       -> "—"
        }
    }

    override fun getItemCount() = history.size
}