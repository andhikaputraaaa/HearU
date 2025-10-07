package com.innovape.hearuapp

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
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

    private var originalUsername: String? = null

    private var selectedAvatarResource: Int? = null

    private var selectedBannerResource: Int? = null

    private var currentProfileResourceName: String? = null
    private var currentProfileImageUrl: String? = null
    private var currentBannerResourceName: String? = null
    private var currentBannerImageUrl: String? = null

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
        val passwordToUpdate = password.ifEmpty { null }

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
        setResult(RESULT_OK)
        finish()
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
