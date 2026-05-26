package com.example.proyectointegrador.adapters

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectointegrador.R
import com.example.proyectointegrador.models.Walker
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore

class WalkerAdapter(
    private val walkers: List<Walker>,
    private val onSelect: (Walker) -> Unit
) : RecyclerView.Adapter<WalkerAdapter.WalkerViewHolder>() {

    private val db = FirebaseFirestore.getInstance()

    inner class WalkerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivWalkerPhoto:       ImageView     = itemView.findViewById(R.id.ivWalkerPhoto)
        val tvWalkerInitial:     TextView      = itemView.findViewById(R.id.tvWalkerInitial)
        val tvWalkerName:        TextView      = itemView.findViewById(R.id.tvWalkerName)
        val ratingBarWalker:     RatingBar     = itemView.findViewById(R.id.ratingBarWalker)
        val tvWalkerRating:      TextView      = itemView.findViewById(R.id.tvWalkerRating)
        val tvWalkerRatingCount: TextView      = itemView.findViewById(R.id.tvWalkerRatingCount)
        val tvWalkerCard:        TextView      = itemView.findViewById(R.id.tvWalkerCard)
        val tvWalkerPrice:       TextView      = itemView.findViewById(R.id.tvWalkerPrice)
        val btnSelectWalker:     MaterialButton = itemView.findViewById(R.id.btnSelectWalker)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalkerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_walker, parent, false)
        return WalkerViewHolder(view)
    }

    override fun onBindViewHolder(holder: WalkerViewHolder, position: Int) {
        val walker = walkers[position]

        holder.tvWalkerName.text = walker.name

        // FIX: Inicial por defecto
        holder.tvWalkerInitial.text  = walker.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        holder.ivWalkerPhoto.visibility  = View.GONE
        holder.tvWalkerInitial.visibility = View.VISIBLE

        // FIX: Cargar foto Base64 desde Firestore
        db.collection("usuarios").document(walker.id).get()
            .addOnSuccessListener { doc ->
                val photo = doc.getString("photoBase64") ?: ""
                if (photo.isNotEmpty()) {
                    try {
                        val bytes  = Base64.decode(photo, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        holder.ivWalkerPhoto.setImageBitmap(bitmap)
                        holder.ivWalkerPhoto.visibility   = View.VISIBLE
                        holder.tvWalkerInitial.visibility = View.GONE
                    } catch (e: Exception) {
                        holder.ivWalkerPhoto.visibility   = View.GONE
                        holder.tvWalkerInitial.visibility = View.VISIBLE
                    }
                }
            }

        // Calificación
        holder.ratingBarWalker.rating    = walker.rating
        holder.tvWalkerRating.text       = String.format("%.1f", walker.rating)
        holder.tvWalkerRatingCount.text  = if (walker.ratingCount > 0)
            "(${walker.ratingCount})" else "(sin reseñas)"

        // FIX: Últimos 4 dígitos de tarjeta
        val card = walker.cardNumber.replace("-", "")
        holder.tvWalkerCard.text = if (card.length >= 4)
            "**** **** **** ${card.takeLast(4)}"
        else "No registrada"

        holder.tvWalkerPrice.text = "$1"

        holder.btnSelectWalker.setOnClickListener {
            // FIX: deshabilitar botón inmediatamente para evitar doble selección
            holder.btnSelectWalker.isEnabled = false
            holder.btnSelectWalker.text = "Enviando..."
            onSelect(walker)
        }
    }

    override fun getItemCount() = walkers.size
}