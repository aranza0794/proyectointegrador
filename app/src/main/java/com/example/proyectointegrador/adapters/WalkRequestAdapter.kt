package com.example.proyectointegrador.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectointegrador.R
import com.example.proyectointegrador.models.WalkRequest
import com.google.android.material.button.MaterialButton

class WalkRequestAdapter(
    private val requests: List<WalkRequest>,
    private val onAccept: (WalkRequest) -> Unit,
    private val onReject: (WalkRequest) -> Unit
) : RecyclerView.Adapter<WalkRequestAdapter.RequestViewHolder>() {

    inner class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvOwnerName: TextView = itemView.findViewById(R.id.tvOwnerName)
        val tvDogName: TextView = itemView.findViewById(R.id.tvDogName)
        val tvDogDetails: TextView = itemView.findViewById(R.id.tvDogDetails)
        val tvDogAllergy: TextView = itemView.findViewById(R.id.tvDogAllergy)
        val tvWalkDuration: TextView = itemView.findViewById(R.id.tvWalkDuration)
        val tvWalkCost: TextView = itemView.findViewById(R.id.tvWalkCost)
        val btnAccept: MaterialButton = itemView.findViewById(R.id.btnAccept)
        val btnReject: MaterialButton = itemView.findViewById(R.id.btnReject)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_walk_request, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val req = requests[position]
        holder.tvOwnerName.text = "Dueño: ${req.ownedName}"
        holder.tvDogName.text = req.dogName
        holder.tvDogDetails.text = "${req.dogBreed} • ${req.dogSize} • ${req.dogAge} años"
        holder.tvWalkDuration.text = "${req.durationMinutes} min"
        holder.tvWalkCost.text = "$${String.format("%.2f", req.cost)}"

        if (req.dogAllergy.isNotEmpty()) {
            holder.tvDogAllergy.visibility = View.VISIBLE
            holder.tvDogAllergy.text = "⚠️ ${req.dogAllergy}"
        } else {
            holder.tvDogAllergy.visibility = View.GONE
        }

        holder.btnAccept.setOnClickListener { onAccept(req) }
        holder.btnReject.setOnClickListener { onReject(req) }
    }

    override fun getItemCount() = requests.size
}