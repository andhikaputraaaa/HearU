package com.innovape.hearuapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.innovape.hearuapp.databinding.ActivityPostingBinding
import kotlin.text.get

class PostingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostingBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            onBackPressed()
        }

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding.btnPost.setOnClickListener {
            val content = binding.etPostContent.text.toString().trim()
            val isAnonymous = binding.switchAnonymous.isChecked

            if (content.isEmpty()) {
                Toast.makeText(this, "Tulis sesuatu dulu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            postStory(content, isAnonymous)
        }
    }

    private fun postStory(content: String, isAnonymous: Boolean) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Harap login dulu", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = currentUser.uid

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val post = hashMapOf(
                        "userId" to userId,
                        "username" to if (isAnonymous) "Anonim" else (document.getString("username") ?: "User"),
                        "name" to if (isAnonymous) "Anonim" else (document.getString("name") ?: ""),
                        "profileImageUrl" to if (isAnonymous) "" else (document.getString("profileImageUrl") ?: ""),
                        "profileImageResource" to if (isAnonymous) "" else (document.getString("profileImageResource") ?: ""),
                        "bannerImageUrl" to if (isAnonymous) "" else (document.getString("bannerImageUrl") ?: ""),
                        "bannerImageResource" to if (isAnonymous) "" else (document.getString("bannerImageResource") ?: ""),
                        "content" to content,
                        "isAnonymous" to isAnonymous,
                        "timestamp" to FieldValue.serverTimestamp(),
                        "likes" to listOf<String>(),
                        "commentCount" to 0
                    )

                    db.collection("posts")
                        .add(post)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Posting berhasil!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Gagal posting: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
    }



    override fun onResume() {
        super.onResume()
        val bottomNavFragment = supportFragmentManager.findFragmentById(R.id.bottom_nav_container) as? Navbar
        bottomNavFragment?.setActiveItem(1) // Edit/Posting position
    }

}

