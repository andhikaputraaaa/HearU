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

        // ambil username dari collection "users"
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val username = if (isAnonymous) {
                        "Anonim"
                    } else {
                        document.getString("username") ?: "User"
                    }

                    val post = hashMapOf(
                        "userId" to userId,
                        "username" to username,
                        "content" to content,
                        "isAnonymous" to isAnonymous,
                        "timestamp" to FieldValue.serverTimestamp(),
                        "likes" to listOf<String>()
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
                } else {
                    Toast.makeText(this, "Data user tidak ditemukan", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal mengambil data user", Toast.LENGTH_SHORT).show()
            }
    }
}

