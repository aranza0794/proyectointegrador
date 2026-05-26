package com.example.proyectointegrador.adapters

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectointegrador.R
import com.example.proyectointegrador.models.WalkRequest
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore

class WalkRequestAdapter(
    private val requests: List<WalkRequest>,
    private val onAccept: (WalkRequest) -> Unit,
    private val onReject: (WalkRequest) -> Unit
) : RecyclerView.Adapter<WalkRequestAdapter.RequestViewHolder>() {

    private val db = FirebaseFirestore.getInstance()

    inner class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivDogPhoto:   ImageView     = itemView.findViewById(R.id.ivDogPhoto)
        val tvDogInitial: TextView      = itemView.findViewById(R.id.tvDogInitial)
        val tvDogName:    TextView      = itemView.findViewById(R.id.tvDogName)
        val tvOwnerName:  TextView      = itemView.findViewById(R.id.tvOwnerName)
        val tvDogDetails: TextView      = itemView.findViewById(R.id.tvDogDetails)
        val tvDogAllergy: TextView      = itemView.findViewById(R.id.tvDogAllergy)
        val tvDuration:   TextView      = itemView.findViewById(R.id.tvWalkDuration)
        val tvCost:       TextView      = itemView.findViewById(R.id.tvWalkCost)
        val btnAccept:    MaterialButton = itemView.findViewById(R.id.btnAccept)
        val btnReject:    MaterialButton = itemView.findViewById(R.id.btnReject)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_walk_request, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val req = requests[position]

        holder.tvDogName.text    = req.dogName
        if (req.ownedName.isNotEmpty()) {
            holder.tvOwnerName.text = "Dueño: ${req.ownedName}"
        } else if (req.ownerId.isNotEmpty()) {
            holder.tvOwnerName.text = "Dueño: cargando..."
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("usuarios").document(req.ownerId).get()
                .addOnSuccessListener { doc ->
                    holder.tvOwnerName.text = "Dueño: ${doc.getString("name") ?: "Desconocido"}"
                }
        } else {
            holder.tvOwnerName.text = "Dueño no disponible"
        }
        holder.tvDogDetails.text = "${req.dogBreed} • ${req.dogSize} • ${req.dogAge} años"
        holder.tvDuration.text   = "${req.durationMinutes} min"
        holder.tvCost.text       = "$${String.format("%.0f", req.cost)}"

        // Alergia
        if (req.dogAllergy.isNotEmpty() &&
            req.dogAllergy != "ninguna" && req.dogAllergy != "Ninguna") {
            holder.tvDogAllergy.visibility = View.VISIBLE
            holder.tvDogAllergy.text       = "⚠️ ${req.dogAllergy}"
        } else {
            holder.tvDogAllergy.visibility = View.GONE
        }

        // FIX: Cargar foto del perro desde Firestore
        holder.ivDogPhoto.visibility   = View.GONE
        holder.tvDogInitial.visibility = View.VISIBLE
        holder.tvDogInitial.text       = "🐕"

        if (req.id.isNotEmpty()) {
            db.collection("solicitudes").document(req.id).get()
                .addOnSuccessListener { solDoc ->
                    val dogId = solDoc.getString("dogId") ?: ""
                    if (dogId.isNotEmpty()) {
                        db.collection("perros").document(dogId).get()
                            .addOnSuccessListener { dogDoc ->
                                val photo = dogDoc.getString("photoBase64") ?: ""
                                if (photo.isNotEmpty()) {
                                    try {
                                        val bytes  = Base64.decode(photo, Base64.DEFAULT)
                                        val bitmap = BitmapFactory.decodeByteArray(
                                            bytes, 0, bytes.size)
                                        holder.ivDogPhoto.setImageBitmap(bitmap)
                                        holder.ivDogPhoto.visibility   = View.VISIBLE
                                        holder.tvDogInitial.visibility = View.GONE
                                    } catch (e: Exception) { /* usa emoji */ }
                                }
                            }
                    }
                }
        }

        holder.btnAccept.setOnClickListener {
            holder.btnAccept.isEnabled = false
            holder.btnReject.isEnabled = false
            holder.btnAccept.text = "..."
            onAccept(req)
        }
        holder.btnReject.setOnClickListener { onReject(req) }
    }

    override fun getItemCount() = requests.size
}