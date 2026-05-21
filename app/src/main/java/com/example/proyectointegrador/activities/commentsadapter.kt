package com.example.proyectointegrador.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectointegrador.R

data class Comment(
    val ownerName: String,
    val stars: Int,
    val comment: String
)

class CommentsAdapter(
    private val comments: List<Comment>
) : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvOwner:   TextView  = itemView.findViewById(R.id.tvCommentOwner)
        val tvComment: TextView  = itemView.findViewById(R.id.tvCommentText)
        val ratingBar: RatingBar = itemView.findViewById(R.id.ratingBarComment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val item = comments[position]
        holder.tvOwner.text   = item.ownerName
        holder.ratingBar.rating = item.stars.toFloat()

        if (item.comment.isNotEmpty()) {
            holder.tvComment.visibility = View.VISIBLE
            holder.tvComment.text       = "\"${item.comment}\""
        } else {
            holder.tvComment.visibility = View.GONE
        }
    }

    override fun getItemCount() = comments.size
}