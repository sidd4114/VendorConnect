package com.example.vendorconnect

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VendorDetailActivity : AppCompatActivity() {
    private lateinit var photosRecyclerView: RecyclerView
    private lateinit var photoAdapter: PhotoAdapter
    private val photosList = mutableListOf<String>()
    private var currentPhotoPath: String = ""
    private var currentVendor: Vendor? = null
    private val storage by lazy { FirebaseStorage.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Upload the captured photo
            val file = File(currentPhotoPath)
            val uri = Uri.fromFile(file)
            uploadImage(uri)
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                // Upload selected image from gallery
                uploadImage(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vendor_detail)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Get vendor data from intent
        @Suppress("DEPRECATION")
        currentVendor = intent.getParcelableExtra("vendor")
        if (currentVendor == null) {
            // Fallback: build minimal Vendor from primitive extras
            val name = intent.getStringExtra("name") ?: ""
            val lat = intent.getDoubleExtra("lat", 0.0)
            val lng = intent.getDoubleExtra("lng", 0.0)
            val address = intent.getStringExtra("address") ?: ""
            val phone = intent.getStringExtra("phone") ?: ""
            if (name.isNotEmpty()) {
                currentVendor = Vendor(
                    name = name,
                    description = "",
                    lat = lat,
                    lng = lng,
                    address = address,
                    phoneNumber = phone
                )
            }
        }
        
        // Initialize photos RecyclerView
        setupPhotosRecyclerView()
        
        // Set up photo upload buttons
        findViewById<MaterialButton>(R.id.btnTakePhoto)?.setOnClickListener {
            if (checkCameraPermission()) {
                takePicture()
            } else {
                requestCameraPermission()
            }
        }

        findViewById<MaterialButton>(R.id.btnChoosePhoto)?.setOnClickListener {
            pickImageFromGallery()
        }
        
        // Set vendor details
        currentVendor?.let {
            findViewById<TextView>(R.id.vendorName).text = it.name
            findViewById<TextView>(R.id.vendorDescription).text = it.description
            findViewById<TextView>(R.id.vendorAddress).text = it.address
            findViewById<TextView>(R.id.vendorPhone).text = it.phoneNumber
            findViewById<TextView>(R.id.vendorRating).text = "Rating: ★ ${it.rating}/5.0"
            findViewById<TextView>(R.id.vendorPriceRange).text = "Price: ${it.priceRange}"
            findViewById<TextView>(R.id.vendorCategory).text = "Category: ${it.categories.firstOrNull() ?: "General"}"
            
            // Set specialties
            val specialtiesText = if (it.specialties?.isNotEmpty() == true) {
                "Specialties: ${it.specialties.joinToString(", ")}"
            } else {
                "No specialties listed"
            }
            findViewById<TextView>(R.id.vendorSpecialties).text = specialtiesText
            
            // Set image
            if (it.imageUrl.isNotEmpty()) {
                Glide.with(this)
                    .load(it.imageUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(findViewById(R.id.vendorImage))
            } else {
                findViewById<ImageView>(R.id.vendorImage).setImageResource(R.drawable.ic_launcher_background)
            }
            
        // Load existing photos
        if (it.photoUrls.isNotEmpty()) {
            photosList.addAll(it.photoUrls)
            updatePhotosList()
        }

        // Load contributed photos from Firestore
        loadContributedPhotos()
            
            // Set up contact button
            findViewById<Button>(R.id.contactButton).setOnClickListener { _ ->
                // In a real app, this would open a messaging interface or phone dialer
                Toast.makeText(this, "Contacting ${it.name}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun setupPhotosRecyclerView() {
        photosRecyclerView = findViewById(R.id.photosRecyclerView)
        photosRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        photoAdapter = PhotoAdapter(photosList) { photoUrl ->
            // Show full-screen photo when clicked
            showFullScreenPhoto(photoUrl)
        }
        photosRecyclerView.adapter = photoAdapter
    }
    
    private fun updatePhotosList() {
        photoAdapter = PhotoAdapter(photosList) { photoUrl ->
            showFullScreenPhoto(photoUrl)
        }
        photosRecyclerView.adapter = photoAdapter
    }

    private fun generateVendorKey(vendor: Vendor): String {
        // Use name and coordinates for a stable key
        val nameKey = vendor.name.replace("[^a-zA-Z0-9]+".toRegex(), "_").lowercase(Locale.getDefault())
        val latKey = String.format(Locale.getDefault(), "%.6f", vendor.lat)
        val lngKey = String.format(Locale.getDefault(), "%.6f", vendor.lng)
        return "${nameKey}_${latKey}_${lngKey}"
    }

    private fun uploadImage(imageUri: Uri) {
        val vendor = currentVendor
        if (vendor == null) {
            Toast.makeText(this, "Vendor not loaded", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Uploading photo...", Toast.LENGTH_SHORT).show()

        val key = generateVendorKey(vendor)
        val storageRef = storage.reference.child("vendorPhotos/$key/${System.currentTimeMillis()}.jpg")
        val uploadTask = storageRef.putFile(imageUri)

        uploadTask
            .addOnSuccessListener {
                storageRef.downloadUrl
                    .addOnSuccessListener { downloadUri ->
                        val url = downloadUri.toString()
                        // Try to append to existing doc; if missing, create it
                        val docRef = db.collection("vendorPhotos").document(key)
                        docRef.update("photoUrls", FieldValue.arrayUnion(url))
                            .addOnSuccessListener {
                                photosList.add(url)
                                updatePhotosList()
                                Toast.makeText(this, "Photo uploaded", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { _ ->
                                val data = hashMapOf(
                                    "name" to vendor.name,
                                    "lat" to vendor.lat,
                                    "lng" to vendor.lng,
                                    "photoUrls" to listOf(url)
                                )
                                docRef.set(data)
                                    .addOnSuccessListener {
                                        photosList.add(url)
                                        updatePhotosList()
                                        Toast.makeText(this, "Photo uploaded", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("VendorDetailActivity", "Failed to save photo URL", e)
                                        Toast.makeText(this, "Failed to save photo URL", Toast.LENGTH_SHORT).show()
                                    }
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e("VendorDetailActivity", "Failed to get download URL", e)
                        Toast.makeText(this, "Failed to get download URL", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.e("VendorDetailActivity", "Upload failed", e)
                Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadContributedPhotos() {
        val vendor = currentVendor ?: return
        val key = generateVendorKey(vendor)
        db.collection("vendorPhotos").document(key).get()
            .addOnSuccessListener { doc ->
                val urls = doc.get("photoUrls") as? List<*>
                val stringUrls = urls?.mapNotNull { it as? String } ?: emptyList()
                if (stringUrls.isNotEmpty()) {
                    photosList.addAll(stringUrls)
                    updatePhotosList()
                }
            }
            .addOnFailureListener { e ->
                Log.w("VendorDetailActivity", "Failed to load contributed photos", e)
            }
    }
    
    private fun showFullScreenPhoto(photoUrl: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_fullscreen_photo, null)
        val photoImageView = dialogView.findViewById<ImageView>(R.id.fullscreenPhotoView)
        
        Glide.with(this)
            .load(photoUrl)
            .into(photoImageView)
            
        MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .show()
    }
    
    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }
    
    private fun takePicture() {
        try {
            val photoFile = createImageFile()
            photoFile.also {
                val photoURI = FileProvider.getUriForFile(
                    this,
                    "com.example.vendorconnect.fileprovider",
                    it
                )
                val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                takePictureLauncher.launch(takePictureIntent)
            }
        } catch (ex: Exception) {
            Log.e("VendorDetailActivity", "Error taking picture", ex)
            Toast.makeText(this, "Failed to take picture", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(null)
        return try {
            val image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
            )
            currentPhotoPath = image.absolutePath
            image
        } catch (ex: IOException) {
            Log.e("VendorDetailActivity", "Error creating image file", ex)
            File(storageDir, "$imageFileName.jpg").apply {
                currentPhotoPath = absolutePath
            }
        }
    }
    
    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
    }
}