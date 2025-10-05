package com.innovape.hearuapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.innovape.hearuapp.data.model.Post
import com.innovape.hearuapp.ui.adapter.PostAdapter
import kotlin.collections.remove
import kotlin.text.clear
import kotlin.text.get
import android.widget.TextView
import androidx.core.widget.NestedScrollView

class ProfileActivity : AppCompatActivity(), Navbar.OnNavigationClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private val postList = mutableListOf<Post>()
    private lateinit var auth: FirebaseAuth
    private var userListener: ListenerRegistration? = null
    private var ivProfile: ImageView? = null
    private var ivBanner: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()

        ivProfile = findViewById<ImageView?>(R.id.iv_profile)
        ivBanner  = findViewById<ImageView?>(R.id.iv_background)

        loadUserProfile()

        val bottomNavFragment = supportFragmentManager.findFragmentById(R.id.bottom_nav_container) as Navbar
        bottomNavFragment.setOnNavigationClickListener(this)

        // Settings button click listener
        val settingsButton = findViewById<ImageView>(R.id.iv_settings)
        settingsButton.setOnClickListener {
            val intent = Intent(this, EditProfilActivity::class.java)
            startActivity(intent)
        }

        recyclerView = findViewById(R.id.recyclerViewPosts)
        recyclerView.layoutManager = LinearLayoutManager(this)
        postAdapter = PostAdapter(postList,
            onLikeClick = { postId, liked -> toggleLike(postId, liked) },
            onCommentClick = { postId ->
                val i = Intent(this, DetailPostActivity::class.java)
                i.putExtra("postId", postId)
                startActivity(i)
            }
        )
        recyclerView.adapter = postAdapter

        val scrollView = findViewById<NestedScrollView>(R.id.scroll_content)
        val headerTitle = findViewById<TextView>(R.id.tv_header)

        headerTitle.setOnClickListener {
            scrollView.post {
                scrollView.smoothScrollTo(0, 0)
            }
        }

        loadUserPosts()
    }

    override fun onDestroy() {
        super.onDestroy()
        userListener?.remove()
    }

    // Ganti fungsi lama loadUserProfile dengan versi realtime di bawah
    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }
        val db = FirebaseFirestore.getInstance()
        val userId = currentUser.uid
        userListener?.remove()
        userListener = db.collection("users").document(userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("ProfileActivity", "User listen failed", e)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val username = snapshot.getString("username") ?: "unknown"
                    val name = snapshot.getString("name") ?: username
                    findViewById<TextView>(R.id.tv_username).text = name
                    findViewById<TextView>(R.id.tv_handle).text = "@$username"
                    val profileUrl = snapshot.getString("profileImageUrl")
                    val bannerUrl = snapshot.getString("bannerImageUrl")
                    ivProfile?.let { iv ->
                        if (!profileUrl.isNullOrEmpty()) {
                            Glide.with(this)
                                .load(profileUrl)
                                .placeholder(R.drawable.ic_profile_placeholder)
                                .error(R.drawable.ic_profile_placeholder)
                                .circleCrop()
                                .into(iv)
                        } else {
                            iv.setImageResource(R.drawable.ic_profile_placeholder)
                        }
                    }
                    ivBanner?.let { iv ->
                        if (!bannerUrl.isNullOrEmpty()) {
                            Glide.with(this)
                                .load(bannerUrl)
                                .placeholder(R.drawable.ic_banner_placeholder)
                                .error(R.drawable.ic_banner_placeholder)
                                .centerCrop()
                                .into(iv)
                        } else {
                            iv.setImageResource(R.drawable.ic_banner_placeholder)
                        }
                    }
                } else {
                    Log.w("ProfileActivity", "User doc missing")
                }
            }
    }

    private fun loadUserPosts() {
        val currentUser = auth.currentUser ?: return
        val db = FirebaseFirestore.getInstance()
        // Jika ingin juga menampilkan posting anonim milik user, hapus filter isAnonymous:
        db.collection("posts")
            .whereEqualTo("userId", currentUser.uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("ProfileActivity", "Listen failed.", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    postList.clear()
                    snapshot.documents.forEach { doc ->
                        doc.toObject(Post::class.java)?.let { post ->
                            post.id = doc.id
                            postList.add(post)
                        }
                    }
                    postAdapter.notifyDataSetChanged()
                }
            }
    }

    private fun toggleLike(postId: String, liked: Boolean) {
        val currentUser = auth.currentUser ?: return
        val db = FirebaseFirestore.getInstance()
        val postRef = db.collection("posts").document(postId)

        // Find the post in local list and update it immediately
        val postIndex = postList.indexOfFirst { it.id == postId }
        if (postIndex != -1) {
            val post = postList[postIndex]
            if (liked) {
                post.likes.remove(currentUser.uid)
            } else {
                post.likes.add(currentUser.uid)
            }
            postAdapter.notifyItemChanged(postIndex)
        }

        // Update Firestore
        db.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            val likes = snapshot.get("likes") as? MutableList<String> ?: mutableListOf()

            if (liked) {
                likes.remove(currentUser.uid)
            } else {
                likes.add(currentUser.uid)
            }

            transaction.update(postRef, "likes", likes)
        }.addOnFailureListener { exception ->
            // Revert local changes if Firestore update fails
            if (postIndex != -1) {
                val post = postList[postIndex]
                if (liked) {
                    post.likes.add(currentUser.uid)
                } else {
                    post.likes.remove(currentUser.uid)
                }
                postAdapter.notifyItemChanged(postIndex)
            }
            Log.e("ProfileActivity", "Error updating like", exception)
        }
    }

    override fun onHomeClick() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
    }

    override fun onEditClick() {
        val intent = Intent(this, PostingActivity::class.java)
        startActivity(intent)
    }

    override fun onProfileClick() {
        val bottomNavFragment = supportFragmentManager.findFragmentById(R.id.bottom_nav_container) as Navbar
        bottomNavFragment.setActiveItem(2)
    }

    override fun onResume() {
        super.onResume()
        // Reload posts when returning to this activity
        loadUserPosts()
        loadUserProfile()
    }
}