package com.innovape.hearuapp

import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.innovape.hearuapp.data.model.Comment
import com.innovape.hearuapp.databinding.ActivityDetailPostBinding
import com.innovape.hearuapp.ui.adapter.CommentAdapter
import java.text.SimpleDateFormat
import java.util.Locale

class DetailPostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailPostBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: CommentAdapter
    private var postId: String = ""
    private var isLiked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        postId = intent.getStringExtra("postId") ?: run {
            finish()
            return
        }

        // setup RecyclerView komentar
        adapter = CommentAdapter(emptyList())
        binding.recyclerViewComments.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewComments.adapter = adapter

        binding.btnBack.setOnClickListener { onBackPressed() }

        loadPostData()
        loadComments()

        // tombol kirim komentar
        binding.btnSendComment.setOnClickListener {
            val text = binding.etAddComment.text.toString().trim()
            if (text.isNotEmpty()) addComment(text)
        }

        // bisa kirim komentar lewat keyboard (enter)
        binding.etAddComment.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val text = binding.etAddComment.text.toString().trim()
                if (text.isNotEmpty()) addComment(text)
                true
            } else false
        }

        // tombol like
        binding.ivLike.setOnClickListener { toggleLike() }
    }

    private fun loadPostData() {
        db.collection("posts").document(postId)
            .addSnapshotListener { doc, e ->
                if (e != null) {
                    Log.w("DetailPost", "post listen failed", e)
                    return@addSnapshotListener
                }

                if (doc != null && doc.exists()) {
                    val username = doc.getString("username") ?: ""
                    val content = doc.getString("content") ?: ""
                    val isAnon = doc.getBoolean("isAnonymous") ?: false
                    val ts = doc.getTimestamp("timestamp")
                    val likes = (doc.get("likes") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    val commentCount = (doc.getLong("commentCount") ?: 0L).toInt()
                    val likeCount = likes.size

                    val currentUserId = auth.currentUser?.uid
                    isLiked = currentUserId != null && likes.contains(currentUserId)

                    // tampilkan data di UI
                    binding.tvDetailUsername.text = if (isAnon) "Anonim" else username
                    binding.tvDetailHandle.text = if (isAnon) "" else "@$username"
                    binding.tvDetailContent.text = content
                    binding.tvLikeCount.text = likeCount.toString()
                    binding.tvCommentCount.text = commentCount.toString()
                    binding.ivLike.setImageResource(
                        if (isLiked) R.drawable.ic_like_filled else R.drawable.ic_like_outline
                    )

                    val formatted = ts?.toDate()?.let {
                        val sdf = SimpleDateFormat("HH.mm · dd MMM yyyy", Locale.getDefault())
                        sdf.format(it)
                    } ?: ""
                    binding.tvDetailTimestamp.text = formatted
                }
            }
    }

    private fun toggleLike() {
        val currentUserId = auth.currentUser?.uid ?: return
        val postRef = db.collection("posts").document(postId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            val likes = (snapshot.get("likes") as? MutableList<String>) ?: mutableListOf()

            if (likes.contains(currentUserId)) {
                likes.remove(currentUserId)
                isLiked = false
            } else {
                likes.add(currentUserId)
                isLiked = true
            }

            transaction.update(postRef, "likes", likes)
        }.addOnSuccessListener {
            // update tampilan lokal langsung agar terasa responsif
            binding.ivLike.setImageResource(
                if (isLiked) R.drawable.ic_like_filled else R.drawable.ic_like_outline
            )
            animateLikeIcon()
        }.addOnFailureListener {
            Toast.makeText(this, "Gagal memperbarui like", Toast.LENGTH_SHORT).show()
        }
    }

    private fun animateLikeIcon() {
        binding.ivLike.animate().apply {
            scaleX(1.2f)
            scaleY(1.2f)
            duration = 100
            withEndAction {
                binding.ivLike.animate().scaleX(1f).scaleY(1f).duration = 100
            }
        }
    }

    private fun loadComments() {
        val db = FirebaseFirestore.getInstance()

        // Ganti get() dengan addSnapshotListener()
        db.collection("posts").document(postId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("CommentActivity", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val commentList = mutableListOf<Comment>()
                    snapshot.documents.forEach { doc ->
                        doc.toObject(Comment::class.java)?.let { comment ->
                            comment.id = doc.id
                            commentList.add(comment)
                        }
                    }
                    adapter.updateData(commentList)
                }
            }
    }


    private fun addComment(content: String) {
        val user = auth.currentUser ?: run {
            Toast.makeText(this, "Harap login dulu", Toast.LENGTH_SHORT).show()
            return
        }
        val userId = user.uid

        // ambil username user dari Firestore
        db.collection("users").document(userId).get()
            .addOnSuccessListener { userDoc ->
                val username = userDoc.getString("username") ?: user.email ?: "Anonim"
                val commentMap = hashMapOf(
                    "userId" to userId,
                    "username" to username,
                    "content" to content,
                    "timestamp" to FieldValue.serverTimestamp()
                )

                db.collection("posts").document(postId).collection("comments")
                    .add(commentMap)
                    .addOnSuccessListener {
                        // update jumlah komentar
                        db.collection("posts").document(postId)
                            .update("commentCount", FieldValue.increment(1))
                        binding.etAddComment.text?.clear()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Gagal mengirim komentar", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal mengambil data user", Toast.LENGTH_SHORT).show()
            }
    }
}
