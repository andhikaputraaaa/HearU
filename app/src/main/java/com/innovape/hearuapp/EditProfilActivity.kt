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
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import com.google.firebase.storage.FirebaseStorage
import kotlin.toString

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

        // Show loading indicator
        Toast.makeText(this, "Loading profile...", Toast.LENGTH_SHORT).show()

        db.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Load text data
                    etNama.setText(document.getString("name") ?: "")
                    etUsername.setText(document.getString("username") ?: "")
                    etEmail.setText(currentUser.email ?: "")
                    
                    // Load profile image
                    val profileImageUrl = document.getString("profileImageUrl")
                    if (!profileImageUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(profileImageUrl)
                            .placeholder(R.drawable.ic_profile_placeholder) // Tambahkan placeholder
                            .error(R.drawable.ic_profile_placeholder)
                            .circleCrop() // Optional: membuat gambar bulat
                            .into(ivProfile)
                    }
                    
                    // Load banner image
                    val bannerImageUrl = document.getString("bannerImageUrl")
                    if (!bannerImageUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(bannerImageUrl)
                            .placeholder(R.drawable.ic_banner_placeholder) // Tambahkan placeholder
                            .error(R.drawable.ic_banner_placeholder)
                            .centerCrop()
                            .into(ivBanner)
                    }
                    
                    // Set placeholder for password
                    etPassword.hint = "Leave empty to keep current password"
                } else {
                    Toast.makeText(this, "Profile data not found", Toast.LENGTH_SHORT).show()
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

        // Jika ada gambar baru, upload dulu
        if (selectedProfileImageUri != null || selectedBannerImageUri != null) {
            uploadImages(
                userId = currentUser.uid,
                name = name,
                username = username,
                email = email,
                password = password
            )
        } else {
            updateUserData(currentUser.uid, name, username, email, password, null, null)
        }
    }

    private fun uploadImages(
        userId: String,
        name: String,
        username: String,
        email: String,
        password: String
    ) {
        val storageRoot = storage.reference
        val tasks = mutableListOf<com.google.android.gms.tasks.Task<android.net.Uri>>()
        var profileImageUrl: String? = null
        var bannerImageUrl: String? = null

        // Profile
        selectedProfileImageUri?.let { uri ->
            val ref = storageRoot.child("profile_images/$userId/profile_${System.currentTimeMillis()}.jpg")
            val task = ref.putFile(uri)
                .continueWithTask { putTask ->
                    if (!putTask.isSuccessful) throw putTask.exception ?: Exception("Upload profile failed")
                    ref.downloadUrl
                }.addOnSuccessListener { download ->
                    profileImageUrl = download.toString()
                }.addOnFailureListener { e ->
                    Toast.makeText(this, "Upload profile fail: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            tasks.add(task)
        }

        // Banner
        selectedBannerImageUri?.let { uri ->
            val ref = storageRoot.child("banner_images/$userId/banner_${System.currentTimeMillis()}.jpg")
            val task = ref.putFile(uri)
                .continueWithTask { putTask ->
                    if (!putTask.isSuccessful) throw putTask.exception ?: Exception("Upload banner failed")
                    ref.downloadUrl
                }.addOnSuccessListener { download ->
                    bannerImageUrl = download.toString()
                }.addOnFailureListener { e ->
                    Toast.makeText(this, "Upload banner fail: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            tasks.add(task)
        }

        if (tasks.isEmpty()) {
            updateUserData(userId, name, username, email, password, null, null)
            return
        }

        com.google.android.gms.tasks.Tasks.whenAllComplete(tasks)
            .addOnSuccessListener {
                // Jika salah satu gagal, variabel mungkin null -> tetap lanjut tanpa gambar itu
                updateUserData(
                    userId = userId,
                    name = name,
                    username = username,
                    email = email,
                    password = password,
                    profileImageUrl = profileImageUrl,
                    bannerImageUrl = bannerImageUrl
                )
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Image upload error: ${e.message}", Toast.LENGTH_SHORT).show()
                // Tetap update data text tanpa gambar baru
                updateUserData(
                    userId, name, username, email, password,
                    profileImageUrl, bannerImageUrl
                )
            }
    }

    private fun updateUserData(userId: String, name: String, username: String, email: String, password: String, 
                               profileImageUrl: String?, bannerImageUrl: String?) {
        val userUpdates = hashMapOf<String, Any>(
            "name" to name,
            "username" to username
        )

        profileImageUrl?.let { userUpdates["profileImageUrl"] = it }
        bannerImageUrl?.let { userUpdates["bannerImageUrl"] = it }

        db.collection("users").document(userId)
            .update(userUpdates)
            .addOnSuccessListener {
                // Propagate perubahan ke semua posting user
                updatePostsUserInfo(
                    userId = userId,
                    name = name,
                    username = username,
                    profileImageUrl = profileImageUrl,
                    bannerImageUrl = bannerImageUrl
                )

                val currentUser = auth.currentUser
                if (email != currentUser?.email) {
                    currentUser?.updateEmail(email)
                        ?.addOnSuccessListener {
                            updatePasswordIfNeeded(password)
                        }
                        ?.addOnFailureListener { e ->
                            Toast.makeText(this, "Error updating email: ${e.message}", Toast.LENGTH_SHORT).show()
                            updatePasswordIfNeeded(password)
                        }
                } else {
                    updatePasswordIfNeeded(password)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error updating profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updatePostsUserInfo(
        userId: String,
        name: String,
        username: String,
        profileImageUrl: String?,
        bannerImageUrl: String?
    ) {
        db.collection("posts")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) return@addOnSuccessListener

                val docs = querySnapshot.documents
                val batches = mutableListOf<WriteBatch>()
                var batch = db.batch()
                var opCount = 0

                docs.forEach { doc ->
                    val ref = doc.reference
                    val updates = mutableMapOf<String, Any>(
                        "name" to name,
                        "username" to username
                    )
                    profileImageUrl?.let { updates["profileImageUrl"] = it }
                    bannerImageUrl?.let { updates["bannerImageUrl"] = it }

                    batch.update(ref, updates)
                    opCount++
                    if (opCount == 450) { // safeguard (limit 500/ batch)
                        batches.add(batch)
                        batch = db.batch()
                        opCount = 0
                    }
                }
                if (opCount > 0) batches.add(batch)

                commitPostBatchesSequentially(batches.iterator())
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed updating posts: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun commitPostBatchesSequentially(iterator: Iterator<WriteBatch>) {
        if (!iterator.hasNext()) return
        val batch = iterator.next()
        batch.commit()
            .addOnSuccessListener {
                commitPostBatchesSequentially(iterator)
            }
            .addOnFailureListener {
                // Silent fail; user profile tetap terupdate
            }
    }

    private fun updatePasswordIfNeeded(password: String) {
        val currentUser = auth.currentUser ?: return

        if (password.isNotEmpty() && password != "********") {
            currentUser.updatePassword(password)
                .addOnSuccessListener {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error updating password: ${e.message}", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
        } else {
            Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        }
    }
}
