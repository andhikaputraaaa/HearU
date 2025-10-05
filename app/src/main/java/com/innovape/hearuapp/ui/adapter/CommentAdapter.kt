package com.innovape.hearuapp.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.innovape.hearuapp.R
import com.innovape.hearuapp.data.model.Comment
import java.text.SimpleDateFormat
import java.util.*

class CommentAdapter(private var comments: List<Comment>) :
    RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUsername: TextView = itemView.findViewById(R.id.tvCommentUsername)
        val tvContent: TextView = itemView.findViewById(R.id.tvCommentContent)
        val tvTime: TextView = itemView.findViewById(R.id.tvCommentTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val c = comments[position]
        holder.tvUsername.text = if (c.username.isBlank()) "Anonim" else "@${c.username}"
        holder.tvContent.text = c.content

        val formatted = c.timestamp?.toDate()?.let { d ->
            SimpleDateFormat("HH.mm · dd MMM yyyy", Locale.getDefault()).format(d)
        } ?: ""
        holder.tvTime.text = formatted
    }

    override fun getItemCount(): Int = comments.size

    fun updateData(newList: List<Comment>) {
        comments = newList
        notifyDataSetChanged()
    }
}
