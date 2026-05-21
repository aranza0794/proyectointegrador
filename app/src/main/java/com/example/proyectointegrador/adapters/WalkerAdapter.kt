package com.example.proyectointegrador.adapters

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

class WalkerAdapter(
    private val walkers: List<Walker>,
    private val onSelect: (Walker) -> Unit
) : RecyclerView.Adapter<WalkerAdapter.WalkerViewHolder>() {

    inner class WalkerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivWalkerPhoto:      ImageView    = itemView.findViewById(R.id.ivWalkerPhoto)
        val tvWalkerInitial:    TextView     = itemView.findViewById(R.id.tvWalkerInitial)
        val tvWalkerName:       TextView     = itemView.findViewById(R.id.tvWalkerName)
        val ratingBarWalker:    RatingBar    = itemView.findViewById(R.id.ratingBarWalker)
        val tvWalkerRating:     TextView     = itemView.findViewById(R.id.tvWalkerRating)
        val tvWalkerRatingCount: TextView    = itemView.findViewById(R.id.tvWalkerRatingCount)
        val tvWalkerCard:       TextView     = itemView.findViewById(R.id.tvWalkerCard)
        val tvWalkerPrice:      TextView     = itemView.findViewById(R.id.tvWalkerPrice)
        val btnSelectWalker:    MaterialButton = itemView.findViewById(R.id.btnSelectWalker)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalkerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_walker, parent, false)
        return WalkerViewHolder(view)
    }

    override fun onBindViewHolder(holder: WalkerViewHolder, position: Int) {
        val walker = walkers[position]

        // Nombre
        holder.tvWalkerName.text = walker.name

        // FIX: Foto — por ahora muestra inicial del nombre
        // Cuando se implemente Firebase Storage se cargaría la URL aquí
        holder.ivWalkerPhoto.visibility  = View.GONE
        holder.tvWalkerInitial.text      = walker.name
            .firstOrNull()
            ?.uppercaseChar()
            ?.toString() ?: "?"

        // Calificación
        val rating = walker.rating
        holder.ratingBarWalker.rating    = rating
        holder.tvWalkerRating.text       = String.format("%.1f", rating)
        holder.tvWalkerRatingCount.text  = if (walker.ratingCount > 0)
            "(${walker.ratingCount})"
        else
            "(sin reseñas)"

        // FIX: Últimos 4 dígitos de la tarjeta
        val card = walker.cardNumber
        holder.tvWalkerCard.text = if (card.length >= 4) {
            val last4 = card.replace("-", "").takeLast(4)
            "**** **** **** $last4"
        } else {
            "No registrada"
        }

        // Precio por minuto
        holder.tvWalkerPrice.text = "$1"

        // Botón seleccionar
        holder.btnSelectWalker.setOnClickListener {
            onSelect(walker)
        }
    }

    override fun getItemCount() = walkers.size
}