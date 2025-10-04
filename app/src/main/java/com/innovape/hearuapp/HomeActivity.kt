package com.innovape.hearuapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.innovape.hearuapp.data.model.Post
import com.innovape.hearuapp.ui.adapter.PostAdapter

class HomeActivity : AppCompatActivity(), Navbar.OnNavigationClickListener {
    private lateinit var recyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private val postList = mutableListOf<Post>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val bottomNavFragment = supportFragmentManager.findFragmentById(R.id.bottom_nav_container) as? Navbar
        bottomNavFragment?.setOnNavigationClickListener(this)

        recyclerView = findViewById(R.id.recyclerViewPosts)
        recyclerView.layoutManager = LinearLayoutManager(this)
        postAdapter = PostAdapter(postList)
        recyclerView.adapter = postAdapter

        loadPosts()
    }

    override fun onHomeClick() {
        val bottomNavFragment = supportFragmentManager.findFragmentById(R.id.bottom_nav_container) as? Navbar
        bottomNavFragment?.setActiveItem(0) // 0 is the position for home
    }

    override fun onEditClick() {
        val intent = Intent(this, PostingActivity::class.java)
        startActivity(intent)
    }

    override fun onProfileClick() {
//        val intent = Intent(this, ProfileActivity::class.java)
//        startActivity(intent)
    }

    private fun loadPosts() {
        val db = FirebaseFirestore.getInstance()
        db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("HomeActivity", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    postList.clear()
                    for (doc in snapshot.documents) {
                        try {
                            val post = doc.toObject(Post::class.java)
                            if (post != null) {
                                postList.add(post)
                            }
                        } catch (ex: Exception) {
                            Log.e("HomeActivity", "Error parsing post: ${doc.data}", ex)
                        }
                    }
                    postAdapter.notifyDataSetChanged()
                }
            }
        Log.d("HomeActivity", "Loaded posts: ${postList.size}")

    }


}