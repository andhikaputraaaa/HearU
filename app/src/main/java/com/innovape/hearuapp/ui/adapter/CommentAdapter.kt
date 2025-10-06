package com.innovape.hearuapp.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.innovape.hearuapp.R
import com.innovape.hearuapp.data.model.Comment
import java.text.SimpleDateFormat
import java.util.*
import kotlin.text.format

class CommentAdapter(private var comments: List<Comment>) :
    RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivAvatar: ImageView = itemView.findViewById(R.id.ivCommentAvatar)
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

        // Load avatar
        val profileResourceName = c.profileImageResource
        if (!profileResourceName.isNullOrEmpty()) {
            val resId = getDrawableResourceId(holder.itemView.context, profileResourceName)
            if (resId != null) {
                com.bumptech.glide.Glide.with(holder.itemView.context)
                    .load(resId)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .circleCrop()
                    .into(holder.ivAvatar)
            } else {
                holder.ivAvatar.setImageResource(R.drawable.ic_profile_placeholder)
            }
        } else if (!c.profileImageUrl.isNullOrEmpty()) {
            com.bumptech.glide.Glide.with(holder.itemView.context)
                .load(c.profileImageUrl)
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .circleCrop()
                .into(holder.ivAvatar)
        } else {
            com.bumptech.glide.Glide.with(holder.itemView.context)
                .load(R.drawable.ic_profile_placeholder)
                .circleCrop()
                .into(holder.ivAvatar)
        }
    }

    override fun getItemCount(): Int = comments.size

    fun updateData(newList: List<Comment>) {
        comments = newList
        notifyDataSetChanged()
    }

    private fun getDrawableResourceId(context: android.content.Context, resourceName: String?): Int? {
        if (resourceName.isNullOrEmpty()) return null
        return try {
            context.resources.getIdentifier(resourceName, "drawable", context.packageName)
        } catch (e: Exception) {
            null
        }
    }
}

