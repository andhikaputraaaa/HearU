package com.innovape.hearuapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class EditProfilActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private lateinit var etNama: EditText
    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var ivProfile: ImageView
    private lateinit var ivBanner: ImageView
    private lateinit var btnUpdate: Button
    private lateinit var btnBack: ImageButton
    private lateinit var btnEditProfile: ImageButton
    private lateinit var btnEditBanner: ImageButton

    private var selectedProfileImageUri: Uri? = null
    private var selectedBannerImageUri: Uri? = null

    // Image picker launchers
    private val profileImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedProfileImageUri = result.data?.data
            ivProfile.setImageURI(selectedProfileImageUri)
        }
    }

    private val bannerImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedBannerImageUri = result.data?.data
            ivBanner.setImageURI(selectedBannerImageUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profil)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // Initialize views
        initViews()

        // Set click listeners
        setClickListeners()

        // Load current user data
        loadCurrentUserData()
    }

    private fun initViews() {
        etNama = findViewById(R.id.etNama)
        etUsername = findViewById(R.id.etUsername)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        ivProfile = findViewById(R.id.ivProfile)
        ivBanner = findViewById(R.id.ivBanner)
        btnUpdate = findViewById(R.id.btnUpdate)
        btnBack = findViewById(R.id.btnBack)
        btnEditProfile = findViewById(R.id.btnEditProfile)
        btnEditBanner = findViewById(R.id.btnEditBanner)
    }

    private fun setClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnEditProfile.setOnClickListener {
            selectProfileImage()
        }

        btnEditBanner.setOnClickListener {
            selectBannerImage()
        }

        btnUpdate.setOnClickListener {
            updateProfile()
        }
    }

    private fun selectProfileImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        profileImageLauncher.launch(intent)
    }

    private fun selectBannerImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        bannerImageLauncher.launch(intent)
    }

    private fun loadCurrentUserData() {
        val currentUser = auth.currentUser ?: return

        db.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    etNama.setText(document.getString("name") ?: "")
                    etUsername.setText(document.getString("username") ?: "")
                    etEmail.setText(currentUser.email ?: "")
                    // Don't populate password for security
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateProfile() {
        val currentUser = auth.currentUser ?: return

        val name = etNama.text.toString().trim()
        val username = etUsername.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (name.isEmpty() || username.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Update user data in Firestore
        val userUpdates = hashMapOf<String, Any>(
            "name" to name,
            "username" to username
        )

        db.collection("users").document(currentUser.uid)
            .update(userUpdates)
            .addOnSuccessListener {
                // Update email if changed
                if (email != currentUser.email) {
                    currentUser.updateEmail(email)
                        .addOnSuccessListener {
                            updatePasswordIfNeeded(password)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error updating email: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    updatePasswordIfNeeded(password)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error updating profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updatePasswordIfNeeded(password: String) {
        val currentUser = auth.currentUser ?: return

        if (password.isNotEmpty() && password != "********") {
            currentUser.updatePassword(password)
                .addOnSuccessListener {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error updating password: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
