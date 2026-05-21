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

class EarningsAdapter(
    private val earnings: List<WalkRequest>
) : RecyclerView.Adapter<EarningsAdapter.EarningViewHolder>() {

    inner class EarningViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvEarningDogName:  TextView = itemView.findViewById(R.id.tvEarningDogName)
        val tvEarningDate:     TextView = itemView.findViewById(R.id.tvEarningDate)
        val tvEarningAmount:   TextView = itemView.findViewById(R.id.tvEarningAmount)
        val tvEarningPayment:  TextView = itemView.findViewById(R.id.tvEarningPayment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EarningViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_earning, parent, false)
        return EarningViewHolder(view)
    }

    override fun onBindViewHolder(holder: EarningViewHolder, position: Int) {
        val item = earnings[position]
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        holder.tvEarningDogName.text  = "${item.dogName} • ${item.durationMinutes} min"
        holder.tvEarningDate.text     = if (item.endTime > 0)
            dateFormat.format(Date(item.endTime)) else "—"
        holder.tvEarningAmount.text   = "+$${String.format("%.2f", item.cost)}"
        holder.tvEarningPayment.text  = when (item.paymentMethod) {
            "cash"     -> "💵 Efectivo"
            "transfer" -> "💳 Transferencia"
            else       -> "—"
        }
    }

    override fun getItemCount() = earnings.size
}