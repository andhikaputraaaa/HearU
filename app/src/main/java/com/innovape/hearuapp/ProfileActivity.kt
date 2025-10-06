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
import kotlin.collections.remove

class ProfileActivity : AppCompatActivity(), Navbar.OnNavigationClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private val postList = mutableListOf<Post>()
    private lateinit var auth: FirebaseAuth
    private var userListener: ListenerRegistration? = null
    private var postsListener: ListenerRegistration? = null
    private var ivProfile: ImageView? = null
    private var ivBanner: ImageView? = null

    companion object {
        private const val EDIT_PROFILE_REQUEST = 100
    }


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
            startActivityForResult(intent, EDIT_PROFILE_REQUEST)
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
        postsListener?.remove()
    }

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

                    // Load profile image
                    val profileResourceName = snapshot.getString("profileImageResource")
                    val profileUrl = snapshot.getString("profileImageUrl")

                    ivProfile?.let { iv ->
                        if (!profileResourceName.isNullOrEmpty()) {
                            val resId = getDrawableResourceId(profileResourceName)
                            if (resId != null) {
                                Glide.with(this)
                                    .load(resId)
                                    .placeholder(R.drawable.ic_photo_profile)
                                    .error(R.drawable.ic_photo_profile)
                                    .circleCrop()
                                    .into(iv)
                            } else {
                                Glide.with(this)
                                    .load(R.drawable.ic_photo_profile)
                                    .circleCrop()
                                    .into(iv)
                            }
                        } else if (!profileUrl.isNullOrEmpty()) {
                            Glide.with(this)
                                .load(profileUrl)
                                .placeholder(R.drawable.ic_photo_profile)
                                .error(R.drawable.ic_photo_profile)
                                .circleCrop()
                                .into(iv)
                        } else {
                            Glide.with(this)
                                .load(R.drawable.ic_photo_profile)
                                .circleCrop()
                                .into(iv)
                        }
                    }

                    // Load banner image
                    val bannerResourceName = snapshot.getString("bannerImageResource")
                    val bannerUrl = snapshot.getString("bannerImageUrl")

                    ivBanner?.let { iv ->
                        if (!bannerResourceName.isNullOrEmpty()) {
                            val resId = getDrawableResourceId(bannerResourceName)
                            if (resId != null) {
                                Glide.with(this)
                                    .load(resId)
                                    .placeholder(R.drawable.ic_profile_background)
                                    .error(R.drawable.ic_profile_background)
                                    .centerCrop()
                                    .into(iv)
                            } else {
                                iv.setImageResource(R.drawable.ic_profile_background)
                            }
                        } else if (!bannerUrl.isNullOrEmpty()) {
                            Glide.with(this)
                                .load(bannerUrl)
                                .placeholder(R.drawable.ic_profile_background)
                                .error(R.drawable.ic_profile_background)
                                .centerCrop()
                                .into(iv)
                        } else {
                            iv.setImageResource(R.drawable.ic_profile_background)
                        }
                    }
                } else {
                    Log.w("ProfileActivity", "User doc missing")
                }
            }
    }


    private fun getDrawableResourceId(resourceName: String?): Int? {
        if (resourceName.isNullOrEmpty()) return null
        return try {
            resources.getIdentifier(resourceName, "drawable", packageName)
        } catch (e: Exception) {
            null
        }
    }



    private fun loadUserPosts() {
        val currentUser = auth.currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        // Hapus listener lama jika ada
        postsListener?.remove()

        postsListener = db.collection("posts")
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
        val bottomNavFragment = supportFragmentManager.findFragmentById(R.id.bottom_nav_container) as? Navbar
        bottomNavFragment?.setActiveItem(2)

        loadUserProfile()

        postsListener?.remove()
        loadUserPosts()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == EDIT_PROFILE_REQUEST && resultCode == RESULT_OK) {
            loadUserProfile()
            postsListener?.remove()
            recyclerView.postDelayed({
                loadUserPosts()
            }, 500) // Delay 1 detik
        }
    }

}