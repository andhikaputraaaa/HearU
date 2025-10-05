package com.innovape.hearuapp.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.innovape.hearuapp.R
import com.innovape.hearuapp.data.model.Post
import java.text.SimpleDateFormat
import java.util.*

class PostAdapter(
    private val posts: List<Post>,
    private val onLikeClick: (postId: String, liked: Boolean) -> Unit,
    private val onCommentClick: (postId: String) -> Unit
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivProfile: ImageView = itemView.findViewById(R.id.ivProfile)
        val username: TextView = itemView.findViewById(R.id.tvUsername)
        val name: TextView = itemView.findViewById(R.id.tvName)
        val content: TextView = itemView.findViewById(R.id.tvContent)
        val timestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        val ivLike: ImageView = itemView.findViewById(R.id.ivLike)
        val tvLikeCount: TextView = itemView.findViewById(R.id.tvLikeCount)
        val ivComment: ImageView = itemView.findViewById(R.id.ivComment)

        val tvCommentCount: TextView = itemView.findViewById(R.id.tvCommentCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        val currentUser = FirebaseAuth.getInstance().currentUser

        holder.username.text = if (post.isAnonymous) {
            "Anonim"
        } else {
            "@${post.username}"
        }
        holder.name.text = if (post.isAnonymous) {
            "Anonim"
        } else {
            "@${post.username}"
        }
        holder.content.text = post.content
        holder.tvLikeCount.text = post.likes.size.toString()
        holder.tvCommentCount.text = post.commentCount.toString()

        val liked = currentUser?.uid in post.likes
        holder.ivLike.setImageResource(
            if (liked) R.drawable.ic_like_filled else R.drawable.ic_like_outline
        )

        holder.ivLike.setOnClickListener {
            post.id?.let { postId ->
                onLikeClick(postId, liked)
            }
        }

        holder.ivComment.setOnClickListener {
            post.id?.let { onCommentClick(it) }
        }

        val formattedDate = post.timestamp?.toDate()?.let { date ->
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(date)
        } ?: ""

        holder.timestamp.text = formattedDate
    }

    override fun getItemCount(): Int = posts.size
}
