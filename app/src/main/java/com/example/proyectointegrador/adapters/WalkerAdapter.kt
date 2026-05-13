package com.example.proyectointegrador.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        val tvWalkerName: TextView = itemView.findViewById(R.id.tvWalkerName)
        val tvWalkerCard: TextView = itemView.findViewById(R.id.tvWalkerCard)
        val tvWalkerPrice: TextView = itemView.findViewById(R.id.tvWalkerPrice)
        val tvRatingCount: TextView = itemView.findViewById(R.id.tvRatingCount)
        val ratingBar: RatingBar = itemView.findViewById(R.id.ratingBarWalker)
        val btnSelect: MaterialButton = itemView.findViewById(R.id.btnSelectWalker)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalkerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_walker, parent, false)
        return WalkerViewHolder(view)
    }

    override fun onBindViewHolder(holder: WalkerViewHolder, position: Int) {
        val walker = walkers[position]
        holder.tvWalkerName.text = walker.name
        holder.tvWalkerCard.text = "**** ${walker.cardNumber.takeLast(4)}"
        holder.tvWalkerPrice.text = "$${String.format("%.0f", walker.rating * 10)}"
        holder.ratingBar.rating = walker.rating
        holder.tvRatingCount.text = "(${walker.ratingCount})"
        holder.btnSelect.setOnClickListener { onSelect(walker) }
    }

    override fun getItemCount() = walkers.size
}