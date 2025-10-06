package com.innovape.hearuapp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.div
import kotlin.text.get
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

    private var originalUsername: String? = null


    // Image picker launchers
    private val profileImageLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedProfileImageUri = uri
            ivProfile.setImageURI(uri)
        }
    }

    private val bannerImageLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedBannerImageUri = uri
            ivBanner.setImageURI(uri)
        }
    }

    private val PERMISSION_REQUEST_CODE = 100

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

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                    PERMISSION_REQUEST_CODE
                )
            }
        } else {
            // Android 12 ke bawah
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permission denied. Cannot select images.", Toast.LENGTH_SHORT).show()
            }
        }
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
        profileImageLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    private fun selectBannerImage() {
        bannerImageLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    private fun loadCurrentUserData() {
        val currentUser = auth.currentUser ?: return

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
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

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
        Log.d("EditProfile", "Starting upload for user: $userId")

        val storageRoot = storage.reference
        val uploadTasks = mutableListOf<com.google.android.gms.tasks.Task<String>>()

        // Upload compressed profile image
        selectedProfileImageUri?.let { uri ->
            val path = "profile_images/$userId/profile_${System.currentTimeMillis()}.jpg"
            Log.d("EditProfile", "Uploading profile to: $path")
            val ref = storageRoot.child(path)

            // Kompres gambar
            val compressedData = compressImage(uri, 500) // Max 500KB

            val uploadTask = ref.putBytes(compressedData)
                .addOnProgressListener { taskSnapshot ->
                    val progress = (100.0 * taskSnapshot.bytesTransferred) / taskSnapshot.totalByteCount
                    Log.d("EditProfile", "Profile upload: $progress%")
                }
                .continueWithTask { task ->
                    if (!task.isSuccessful) {
                        Log.e("EditProfile", "Profile upload failed", task.exception)
                        task.exception?.let { throw it }
                    }
                    ref.downloadUrl
                }
                .continueWith { task ->
                    val url = task.result.toString()
                    Log.d("EditProfile", "Profile URL: $url")
                    url
                }
            uploadTasks.add(uploadTask)
        }

        // Upload compressed banner image
        selectedBannerImageUri?.let { uri ->
            val path = "banner_images/$userId/banner_${System.currentTimeMillis()}.jpg"
            Log.d("EditProfile", "Uploading banner to: $path")
            val ref = storageRoot.child(path)

            // Kompres gambar (banner bisa lebih besar)
            val compressedData = compressImage(uri, 1024) // Max 1MB

            val uploadTask = ref.putBytes(compressedData)
                .addOnProgressListener { taskSnapshot ->
                    val progress = (100.0 * taskSnapshot.bytesTransferred) / taskSnapshot.totalByteCount
                    Log.d("EditProfile", "Banner upload: $progress%")
                }
                .continueWithTask { task ->
                    if (!task.isSuccessful) {
                        Log.e("EditProfile", "Banner upload failed", task.exception)
                        task.exception?.let { throw it }
                    }
                    ref.downloadUrl
                }
                .continueWith { task ->
                    val url = task.result.toString()
                    Log.d("EditProfile", "Banner URL: $url")
                    url
                }
            uploadTasks.add(uploadTask)
        }

        if (uploadTasks.isEmpty()) {
            updateUserData(userId, name, username, email, password, null, null)
            return
        }

        // Wait for all uploads
        com.google.android.gms.tasks.Tasks.whenAllSuccess<String>(uploadTasks)
            .addOnSuccessListener { results ->
                Log.d("EditProfile", "All uploads successful")
                var resultIndex = 0
                var profileImageUrl: String? = null
                var bannerImageUrl: String? = null

                if (selectedProfileImageUri != null) {
                    profileImageUrl = results[resultIndex]
                    resultIndex++
                }
                if (selectedBannerImageUri != null) {
                    bannerImageUrl = results[resultIndex]
                }

                updateUserData(userId, name, username, email, password, profileImageUrl, bannerImageUrl)
            }
            .addOnFailureListener { e ->
                Log.e("EditProfile", "Upload failed", e)
                Toast.makeText(this, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Method untuk kompres gambar
    private fun compressImage(uri: Uri, maxSizeKB: Int): ByteArray {
        val inputStream = contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        var quality = 90
        var outputStream = ByteArrayOutputStream()

        // Compress sampai ukuran < maxSizeKB
        do {
            outputStream.reset()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            quality -= 10
        } while (outputStream.size() / 1024 > maxSizeKB && quality > 0)

        Log.d("EditProfile", "Compressed image size: ${outputStream.size() / 1024} KB")

        return outputStream.toByteArray()
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
                // Update posts di background
                updatePostsUserInfo(
                    userId = userId,
                    name = name,
                    username = username,
                    profileImageUrl = profileImageUrl,
                    bannerImageUrl = bannerImageUrl
                )

                // Update comments username
                updateCommentsUserData(
                    userId = userId,
                    newUsername = username,
                    oldUsername = originalUsername,
                    profileImageUrl = profileImageUrl
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

    // Ganti fungsi updateCommentsUserData lama
    private fun updateCommentsUserData(
        userId: String,
        newUsername: String,
        oldUsername: String?,
        profileImageUrl: String?
    ) {
        // Langkah 1: Query by userId
        db.collectionGroup("comments")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { byUserId ->
                if (byUserId.isEmpty && oldUsername != null && oldUsername != newUsername) {
                    // Fallback: mungkin komentar lama belum punya userId -> cari by old username
                    Log.w("EditProfile", "No comments by userId. Fallback by oldUsername=$oldUsername")
                    fallbackUpdateByOldUsername(
                        userId,
                        newUsername,
                        oldUsername,
                        profileImageUrl
                    )
                } else {
                    Log.d("EditProfile", "Comments (userId) found: ${byUserId.size()}")
                    batchUpdateComments(byUserId.documents, userId, newUsername, profileImageUrl)
                }
            }
            .addOnFailureListener {
                Log.e("EditProfile", "Query comments by userId failed: ${it.message}", it)
            }
    }

    private fun fallbackUpdateByOldUsername(
        userId: String,
        newUsername: String,
        oldUsername: String,
        profileImageUrl: String?
    ) {
        db.collectionGroup("comments")
            .whereEqualTo("username", oldUsername)
            .get()
            .addOnSuccessListener { qs ->
                Log.d("EditProfile", "Fallback comments found: ${qs.size()}")
                if (!qs.isEmpty) {
                    batchUpdateComments(qs.documents, userId, newUsername, profileImageUrl, addUserIdIfMissing = true)
                }
            }
            .addOnFailureListener {
                Log.e("EditProfile", "Fallback query failed: ${it.message}", it)
            }
    }

    private fun batchUpdateComments(
        docs: List<DocumentSnapshot>,
        userId: String,
        newUsername: String,
        profileImageUrl: String?,
        addUserIdIfMissing: Boolean = false
    ) {
        if (docs.isEmpty()) return
        val batches = mutableListOf<WriteBatch>()
        var batch = db.batch()
        var count = 0
        docs.forEach { doc ->
            val updates = mutableMapOf<String, Any>(
                "username" to newUsername
            )
            if (addUserIdIfMissing && !doc.contains("userId")) {
                updates["userId"] = userId
            }
            profileImageUrl?.let { updates["profileImageUrl"] = it }

            Log.d("EditProfile", "Updating comment: ${doc.reference.path} -> $updates")
            batch.update(doc.reference, updates)
            count++
            if (count == 450) {
                batches.add(batch)
                batch = db.batch()
                count = 0
            }
        }
        if (count > 0) batches.add(batch)
        commitCommentBatchesSequentially(batches.iterator())
    }


    private fun commitCommentBatchesSequentially(batchIterator: Iterator<WriteBatch>) {
        if (!batchIterator.hasNext()) {
            Log.d("EditProfile", "All comment batches committed")
            return
        }

        val batch = batchIterator.next()
        batch.commit()
            .addOnSuccessListener {
                Log.d("EditProfile", "Comment batch committed successfully")
                commitCommentBatchesSequentially(batchIterator)
            }
            .addOnFailureListener { e ->
                Log.e("EditProfile", "Failed committing comment batch: ${e.message}", e)
                // Tetap lanjutkan ke batch berikutnya
                commitCommentBatchesSequentially(batchIterator)
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
            .whereEqualTo("isAnonymous", false)
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
                    if (opCount == 450) {
                        batches.add(batch)
                        batch = db.batch()
                        opCount = 0
                    }
                }
                if (opCount > 0) batches.add(batch)

                commitPostBatchesSequentially(batches.iterator())
            }
            .addOnFailureListener { e ->
                Log.e("EditProfile", "Failed updating posts: ${e.message}")
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
