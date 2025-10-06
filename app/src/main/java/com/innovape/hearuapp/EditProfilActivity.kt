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
import kotlin.text.set
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

    private var selectedAvatarResource: Int? = null

    private var selectedBannerResource: Int? = null

    private var currentProfileResourceName: String? = null
    private var currentProfileImageUrl: String? = null
    private var currentBannerResourceName: String? = null
    private var currentBannerImageUrl: String? = null

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
        etNama = findViewById(R.id.etNama)
        etUsername = findViewById(R.id.etUsername)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)  // Pastikan ini ada
        ivProfile = findViewById(R.id.ivProfile)
        ivBanner = findViewById(R.id.ivBanner)
        btnUpdate = findViewById(R.id.btnUpdate)

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

    private fun showAvatarPickerDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_select_avatar, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        var tempSelectedAvatar: Int? = null
        var tempSelectedBanner: Int? = null

        // Avatar ImageViews and Checkmarks
        val avatarMap = mapOf(
            R.id.ivAvatarAnjing to Pair(R.drawable.anjing, R.id.checkAvatarAnjing),
            R.id.ivAvatarBabi to Pair(R.drawable.babi, R.id.checkAvatarBabi),
            R.id.ivAvatarKelinci to Pair(R.drawable.kelinci, R.id.checkAvatarKelinci),
            R.id.ivAvatarKucing to Pair(R.drawable.kucing, R.id.checkAvatarKucing),
            R.id.ivAvatarMonyet to Pair(R.drawable.monyet, R.id.checkAvatarMonyet)
        )

        // Banner ImageViews and Checkmarks
        val bannerMap = mapOf(
            R.id.ivBannerDanau to Pair(R.drawable.danau, R.id.checkBannerDanau),
            R.id.ivBannerHutanMusimSemi to Pair(R.drawable.hutan_musim_semi, R.id.checkBannerHutanMusimSemi),
            R.id.ivBannerPantai to Pair(R.drawable.pantai, R.id.checkBannerPantai),
            R.id.ivBannerPegununganSunset to Pair(R.drawable.pegunungan_sunset, R.id.checkBannerPegununganSunset),
            R.id.ivBannerRumahPadangHijau to Pair(R.drawable.rumah_di_padang_hijau, R.id.checkBannerRumahPadangHijau)
        )

        // Setup avatar clicks
        avatarMap.forEach { (imageViewId, pair) ->
            val imageView = dialogView.findViewById<ImageView>(imageViewId)
            val drawable = pair.first

            imageView.setOnClickListener {
                tempSelectedAvatar = drawable
                // Hide all checks
                avatarMap.values.forEach { p ->
                    dialogView.findViewById<ImageView>(p.second).visibility = android.view.View.GONE
                }
                // Show selected check
                dialogView.findViewById<ImageView>(pair.second).visibility = android.view.View.VISIBLE
            }
        }

        // Setup banner clicks
        bannerMap.forEach { (imageViewId, pair) ->
            val imageView = dialogView.findViewById<ImageView>(imageViewId)
            val drawable = pair.first

            imageView.setOnClickListener {
                tempSelectedBanner = drawable
                // Hide all checks
                bannerMap.values.forEach { p ->
                    dialogView.findViewById<ImageView>(p.second).visibility = android.view.View.GONE
                }
                // Show selected check
                dialogView.findViewById<ImageView>(pair.second).visibility = android.view.View.VISIBLE
            }
        }

        // Set current selections
        selectedAvatarResource?.let { currentAvatar ->
            avatarMap.forEach { (_, pair) ->
                if (pair.first == currentAvatar) {
                    dialogView.findViewById<ImageView>(pair.second).visibility = android.view.View.VISIBLE
                    tempSelectedAvatar = currentAvatar
                }
            }
        }

        selectedBannerResource?.let { currentBanner ->
            bannerMap.forEach { (_, pair) ->
                if (pair.first == currentBanner) {
                    dialogView.findViewById<ImageView>(pair.second).visibility = android.view.View.VISIBLE
                    tempSelectedBanner = currentBanner
                }
            }
        }

        dialogView.findViewById<Button>(R.id.btnSelectAvatar).setOnClickListener {
            tempSelectedAvatar?.let { selectAvatar(it) }
            tempSelectedBanner?.let { selectBanner(it) }
            dialog.dismiss()
        }

        dialog.show()
    }


    private fun selectBanner(bannerResource: Int) {
        selectedBannerResource = bannerResource
        selectedBannerImageUri = null

        Glide.with(this)
            .load(bannerResource)
            .centerCrop()
            .into(ivBanner)
    }

    private fun selectBannerImage() {
        showAvatarPickerDialog()
    }

    private fun selectAvatar(avatarResource: Int) {
        selectedAvatarResource = avatarResource
        selectedProfileImageUri = null

        Glide.with(this)
            .load(avatarResource)
            .circleCrop()
            .into(ivProfile)
    }

    private fun selectProfileImage() {
        showAvatarPickerDialog()
    }

    private fun getDrawableResourceName(resourceId: Int): String {
        return resources.getResourceEntryName(resourceId)
    }

    private fun getDrawableResourceId(resourceName: String?): Int? {
        if (resourceName.isNullOrEmpty()) return null
        return try {
            resources.getIdentifier(resourceName, "drawable", packageName)
        } catch (e: Exception) {
            null
        }
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
                    originalUsername = document.getString("username")

                    // Simpan data profile yang sudah ada
                    currentProfileResourceName = document.getString("profileImageResource")
                    currentProfileImageUrl = document.getString("profileImageUrl")
                    currentBannerResourceName = document.getString("bannerImageResource")
                    currentBannerImageUrl = document.getString("bannerImageUrl")

                    // Load profile image
                    if (!currentProfileResourceName.isNullOrEmpty()) {
                        val resId = getDrawableResourceId(currentProfileResourceName)
                        if (resId != null) {
                            Glide.with(this)
                                .load(resId)
                                .placeholder(R.drawable.ic_profile_placeholder)
                                .error(R.drawable.ic_profile_placeholder)
                                .circleCrop()
                                .into(ivProfile)
                        }
                    } else if (!currentProfileImageUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(currentProfileImageUrl)
                            .placeholder(R.drawable.ic_profile_placeholder)
                            .error(R.drawable.ic_profile_placeholder)
                            .circleCrop()
                            .into(ivProfile)
                    } else {
                        ivProfile.setImageResource(R.drawable.ic_profile_placeholder)
                    }

                    // Load banner image
                    if (!currentBannerResourceName.isNullOrEmpty()) {
                        val resId = getDrawableResourceId(currentBannerResourceName)
                        if (resId != null) {
                            Glide.with(this)
                                .load(resId)
                                .placeholder(R.drawable.ic_banner_placeholder)
                                .error(R.drawable.ic_banner_placeholder)
                                .centerCrop()
                                .into(ivBanner)
                        }
                    } else if (!currentBannerImageUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(currentBannerImageUrl)
                            .placeholder(R.drawable.ic_banner_placeholder)
                            .error(R.drawable.ic_banner_placeholder)
                            .centerCrop()
                            .into(ivBanner)
                    } else {
                        ivBanner.setImageResource(R.drawable.ic_banner_placeholder)
                    }

                    etPassword.hint = "Leave empty to keep current password"
                }
            }
    }


    private fun updateProfile() {
        val name = etNama.text.toString().trim()
        val username = etUsername.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (name.isEmpty() || username.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Nama, username, dan email tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        // Cek apakah ada perubahan password
        val passwordToUpdate = if (password.isEmpty()) null else password

        if (selectedProfileImageUri != null || selectedBannerImageUri != null) {
//            uploadImagesToStorage(name, username, email, passwordToUpdate)
        } else {
            val profileResourceName = selectedAvatarResource?.let {
                getDrawableResourceName(it)
            } ?: currentProfileResourceName

            val bannerResourceName = selectedBannerResource?.let {
                getDrawableResourceName(it)
            } ?: currentBannerResourceName

            saveProfileData(
                name,
                username,
                email,
                passwordToUpdate,
                profileResourceName,
                currentProfileImageUrl,
                bannerResourceName,
                currentBannerImageUrl
            )
        }
    }

    private fun updateUserPosts(
        name: String,
        username: String,
        profileImageResource: String?,
        profileImageUrl: String?,
        bannerImageResource: String?,
        bannerImageUrl: String?
    ) {
        val currentUser = auth.currentUser ?: return

        db.collection("posts")
            .whereEqualTo("userId", currentUser.uid)
            .whereEqualTo("isAnonymous", false) // Hanya update postingan non-anonim
            .get()
            .addOnSuccessListener { documents ->
                val batch = db.batch()
                for (doc in documents) {
                    val postRef = db.collection("posts").document(doc.id)
                    batch.update(postRef, mapOf(
                        "name" to name,
                        "username" to username,
                        "profileImageResource" to (profileImageResource ?: ""),
                        "profileImageUrl" to (profileImageUrl ?: ""),
                        "bannerImageResource" to (bannerImageResource ?: ""),
                        "bannerImageUrl" to (bannerImageUrl ?: "")
                    ))
                }
                batch.commit()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
                        finishUpdate()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Gagal update postingan: ${e.message}", Toast.LENGTH_SHORT).show()
                        finishUpdate()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal mengambil postingan: ${e.message}", Toast.LENGTH_SHORT).show()
                finishUpdate()
            }
    }

    private fun finishUpdate() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun updateUserDataWithDrawables(
        userId: String,
        name: String,
        username: String,
        email: String,
        password: String
    ) {
        val updates = mutableMapOf<String, Any>(
            "name" to name,
            "username" to username,
            "email" to email
        )

        // Simpan nama resource drawable untuk avatar
        selectedAvatarResource?.let {
            updates["profileImageResource"] = getDrawableResourceName(it)
            updates["profileImageUrl"] = "" // Clear URL jika pakai drawable
        }

        // Simpan nama resource drawable untuk banner
        selectedBannerResource?.let {
            updates["bannerImageResource"] = getDrawableResourceName(it)
            updates["bannerImageUrl"] = "" // Clear URL jika pakai drawable
        }

        if (password.isNotEmpty()) {
            updates["password"] = password
        }

        db.collection("users").document(userId)
            .update(updates)
            .addOnSuccessListener {
                if (username != originalUsername) {
                    updateUsernameInPosts(userId, username)
                } else {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUsernameInPosts(userId: String, newUsername: String) {
        val db = FirebaseFirestore.getInstance()

        // Query semua posts milik user ini
        db.collection("posts")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                    return@addOnSuccessListener
                }

                val batch = db.batch()

                // Update username di setiap post
                querySnapshot.documents.forEach { document ->
                    batch.update(document.reference, "username", newUsername)
                }

                // Commit batch update
                batch.commit()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Log.e("EditProfile", "Failed to update posts", e)
                        Toast.makeText(this, "Profile updated, but failed to update posts", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    }
            }
            .addOnFailureListener { e ->
                Log.e("EditProfile", "Failed to query posts", e)
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            }
    }


    private fun uploadGalleryImages(
        userId: String,
        name: String,
        username: String,
        email: String,
        password: String
    ) {
        var profileImageUrl: String? = null
        var bannerImageUrl: String? = null
        val storageRef = storage.reference
        val uploadTasks = mutableListOf<com.google.android.gms.tasks.Task<Uri>>()

        // Upload profile image dari galeri
        if (selectedProfileImageUri != null) {
            val profilePath = "profile_images/$userId/profile_${System.currentTimeMillis()}.jpg"
            val profileRef = storageRef.child(profilePath)
            val compressedData = compressImage(selectedProfileImageUri!!, 512)

            val uploadTask = profileRef.putBytes(compressedData).continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                profileRef.downloadUrl
            }
            uploadTasks.add(uploadTask)
        }

        // Upload banner image dari galeri
        if (selectedBannerImageUri != null) {
            val bannerPath = "banner_images/$userId/banner_${System.currentTimeMillis()}.jpg"
            val bannerRef = storageRef.child(bannerPath)
            val compressedData = compressImage(selectedBannerImageUri!!, 1024)

            val uploadTask = bannerRef.putBytes(compressedData).continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                bannerRef.downloadUrl
            }
            uploadTasks.add(uploadTask)
        }

        if (uploadTasks.isEmpty()) {
            updateUserDataWithDrawables(userId, name, username, email, password)
            return
        }

        com.google.android.gms.tasks.Tasks.whenAllSuccess<Uri>(uploadTasks)
            .addOnSuccessListener { uris ->
                var index = 0
                if (selectedProfileImageUri != null) {
                    profileImageUrl = uris[index].toString()
                    index++
                }
                if (selectedBannerImageUri != null) {
                    bannerImageUrl = uris[index].toString()
                }

                val updates = mutableMapOf<String, Any>(
                    "name" to name,
                    "username" to username,
                    "email" to email
                )

                // Clear drawable resource jika upload dari galeri
                if (selectedProfileImageUri != null) {
                    updates["profileImageUrl"] = profileImageUrl!!
                    updates["profileImageResource"] = ""
                }

                if (selectedBannerImageUri != null) {
                    updates["bannerImageUrl"] = bannerImageUrl!!
                    updates["bannerImageResource"] = ""
                }

                if (password.isNotEmpty()) {
                    updates["password"] = password
                }

                db.collection("users").document(userId)
                    .update(updates)
                    .addOnSuccessListener {
                        if (username != originalUsername) {
                            updateUsernameInPosts(userId, username)
                        } else {
                            Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                            setResult(RESULT_OK)
                            finish()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to upload images: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun uploadImagesWithAvatars(
        userId: String,
        name: String,
        username: String,
        email: String,
        password: String
    ) {
        var profileImageUrl: String? = null
        var bannerImageUrl: String? = null
        val storageRef = storage.reference

        val uploadTasks = mutableListOf<com.google.android.gms.tasks.Task<Uri>>()

        // Upload profile image or avatar
        if (selectedAvatarResource != null) {
            val bitmap = BitmapFactory.decodeResource(resources, selectedAvatarResource!!)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            val data = outputStream.toByteArray()

            val profilePath = "profile_images/$userId/avatar_${System.currentTimeMillis()}.png"
            val profileRef = storageRef.child(profilePath)

            val uploadTask = profileRef.putBytes(data).continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                profileRef.downloadUrl
            }
            uploadTasks.add(uploadTask)
        } else if (selectedProfileImageUri != null) {
            val profilePath = "profile_images/$userId/profile_${System.currentTimeMillis()}.jpg"
            val profileRef = storageRef.child(profilePath)
            val compressedData = compressImage(selectedProfileImageUri!!, 1024)

            val uploadTask = profileRef.putBytes(compressedData).continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                profileRef.downloadUrl
            }
            uploadTasks.add(uploadTask)
        }

        // Upload banner image or preset
        if (selectedBannerResource != null) {
            val bitmap = BitmapFactory.decodeResource(resources, selectedBannerResource!!)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            val data = outputStream.toByteArray()

            val bannerPath = "banner_images/$userId/banner_${System.currentTimeMillis()}.jpg"
            val bannerRef = storageRef.child(bannerPath)

            val uploadTask = bannerRef.putBytes(data).continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                bannerRef.downloadUrl
            }
            uploadTasks.add(uploadTask)
        } else if (selectedBannerImageUri != null) {
            val bannerPath = "banner_images/$userId/banner_${System.currentTimeMillis()}.jpg"
            val bannerRef = storageRef.child(bannerPath)
            val compressedData = compressImage(selectedBannerImageUri!!, 1024)

            val uploadTask = bannerRef.putBytes(compressedData).continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                bannerRef.downloadUrl
            }
            uploadTasks.add(uploadTask)
        }

        // Wait for all uploads to complete
        com.google.android.gms.tasks.Tasks.whenAllSuccess<Uri>(uploadTasks)
            .addOnSuccessListener { uris ->
                var index = 0
                if (selectedAvatarResource != null || selectedProfileImageUri != null) {
                    profileImageUrl = uris[index].toString()
                    index++
                }
                if (selectedBannerResource != null || selectedBannerImageUri != null) {
                    bannerImageUrl = uris[index].toString()
                }

                updateUserData(userId, name, username, email, password, profileImageUrl, bannerImageUrl)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to upload images: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadAvatarAsImage(
        userId: String,
        avatarResource: Int,
        name: String,
        username: String,
        email: String,
        password: String
    ) {
        val bitmap = BitmapFactory.decodeResource(resources, avatarResource)
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val data = outputStream.toByteArray()

        val storageRef = storage.reference
        val profilePath = "profile_images/$userId/avatar_${System.currentTimeMillis()}.png"
        val profileRef = storageRef.child(profilePath)

        profileRef.putBytes(data)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                profileRef.downloadUrl
            }
            .addOnSuccessListener { uri ->
                val profileImageUrl = uri.toString()

                // Handle banner upload jika ada
                if (selectedBannerImageUri != null) {
                    uploadBannerOnly(userId, name, username, email, password, profileImageUrl)
                } else {
                    updateUserData(userId, name, username, email, password, profileImageUrl, null)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to upload avatar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadBannerOnly(
        userId: String,
        name: String,
        username: String,
        email: String,
        password: String,
        profileImageUrl: String
    ) {
        val storageRef = storage.reference
        val bannerPath = "banner_images/$userId/banner_${System.currentTimeMillis()}.jpg"
        val bannerRef = storageRef.child(bannerPath)

        val compressedData = compressImage(selectedBannerImageUri!!, 1024)

        bannerRef.putBytes(compressedData)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                bannerRef.downloadUrl
            }
            .addOnSuccessListener { uri ->
                updateUserData(userId, name, username, email, password, profileImageUrl, uri.toString())
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to upload banner: ${e.message}", Toast.LENGTH_SHORT).show()
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

    private fun updateAllUserPosts(
        newProfileResourceName: String?,
        newProfileImageUrl: String?,
        newName: String,
        newUsername: String
    ) {
        val currentUser = auth.currentUser ?: return

        db.collection("posts")
            .whereEqualTo("userId", currentUser.uid)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val batch = db.batch()

                for (document in querySnapshot.documents) {
                    val postRef = db.collection("posts").document(document.id)

                    val updates = hashMapOf<String, Any?>(
                        "profileImageResource" to (newProfileResourceName ?: ""),
                        "profileImageUrl" to (newProfileImageUrl ?: ""),
                        "name" to newName,
                        "username" to newUsername
                    )

                    batch.update(postRef, updates)
                }

                batch.commit()
                    .addOnSuccessListener {
                        Log.d("EditProfilActivity", "All posts updated successfully")
                    }
                    .addOnFailureListener { e ->
                        Log.e("EditProfilActivity", "Error updating posts", e)
                    }
            }
    }

    private fun saveProfileData(
        name: String,
        username: String,
        email: String,
        password: String?,
        profileImageResource: String?,
        profileImageUrl: String?,
        bannerImageResource: String?,
        bannerImageUrl: String?
    ) {
        val currentUser = auth.currentUser ?: return

        // Update data user di Firestore
        val userUpdates = hashMapOf<String, Any?>(
            "name" to name,
            "username" to username,
            "email" to email,
            "profileImageResource" to profileImageResource,
            "profileImageUrl" to profileImageUrl,
            "bannerImageResource" to bannerImageResource,
            "bannerImageUrl" to bannerImageUrl
        )

        db.collection("users").document(currentUser.uid)
            .update(userUpdates)
            .addOnSuccessListener {
                // Update password jika ada
                if (!password.isNullOrEmpty()) {
                    currentUser.updatePassword(password)
                        .addOnSuccessListener {
                            updateUserPosts(name, username, profileImageResource, profileImageUrl, bannerImageResource, bannerImageUrl)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Gagal update password: ${e.message}", Toast.LENGTH_SHORT).show()
                            updateUserPosts(name, username, profileImageResource, profileImageUrl, bannerImageResource, bannerImageUrl)
                        }
                } else {
                    updateUserPosts(name, username, profileImageResource, profileImageUrl, bannerImageResource, bannerImageUrl)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal update profil: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

}
