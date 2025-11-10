package com.example.vendorconnect

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Parcelable
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

class VendorDetailActivity : AppCompatActivity() {
    private lateinit var photosRecyclerView: RecyclerView
    private lateinit var photoAdapter: PhotoAdapter
    private val photosList = mutableListOf<String>()
    private var currentPhotoPath: String = ""
    private var currentVendor: Vendor? = null

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                // Verify the file exists before adding
                val photoFile = File(currentPhotoPath)
                if (photoFile.exists() && photoFile.length() > 0) {
                    photosList.add(currentPhotoPath)
                    updatePhotosList()
                    Toast.makeText(this, "Photo captured successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Photo file not found or empty", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("VendorDetailActivity", "Error processing captured photo", e)
                Toast.makeText(this, "Failed to process photo: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else if (result.resultCode == Activity.RESULT_CANCELED) {
            Toast.makeText(this, "Photo capture cancelled", Toast.LENGTH_SHORT).show()
            // Clean up the file if capture was cancelled
            try {
                File(currentPhotoPath).delete()
            } catch (e: Exception) {
                Log.w("VendorDetailActivity", "Failed to delete cancelled photo file", e)
            }
        } else {
            Toast.makeText(this, "Photo capture failed", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                result.data?.data?.let { uri ->
                    // Validate the URI
                    if (uri.toString().isNotEmpty()) {
                        val imagePath = uri.toString()
                        photosList.add(imagePath)
                        updatePhotosList()
                        Toast.makeText(this, "Photo selected successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Invalid image selected", Toast.LENGTH_SHORT).show()
                    }
                } ?: run {
                    Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("VendorDetailActivity", "Error processing selected image", e)
                Toast.makeText(this, "Failed to process selected image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else if (result.resultCode == Activity.RESULT_CANCELED) {
            Toast.makeText(this, "Image selection cancelled", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Image selection failed", Toast.LENGTH_SHORT).show()
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
            findViewById<TextView>(R.id.vendorCategory).text = "Category: ${it.category}"
            
            // Set specialties
            val specialtiesText = if (it.specialties.isNotEmpty()) {
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
        try {
            val photos = mutableListOf<String>()
            
            // Add existing photos with validation
            photosList.forEach { photoUrl ->
                if (!photoUrl.isNullOrEmpty()) {
                    photos.add(photoUrl)
                }
            }
            
            // Update adapter with validated photos
            photoAdapter = PhotoAdapter(photos) { photoUrl ->
                showFullScreenPhoto(photoUrl)
            }
            photosRecyclerView.adapter = photoAdapter
            photosRecyclerView.visibility = if (photos.isEmpty()) View.GONE else View.VISIBLE
        } catch (e: Exception) {
            Log.e("VendorDetailActivity", "Error updating photos list", e)
            Toast.makeText(this, "Error displaying photos: ${e.message}", Toast.LENGTH_SHORT).show()
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
            if (photoFile == null) {
                Toast.makeText(this, "Failed to create image file", Toast.LENGTH_SHORT).show()
                return
            }
            
            photoFile.also { file ->
                try {
                    val photoURI = FileProvider.getUriForFile(
                        this,
                        "com.example.vendorconnect.fileprovider",
                        file
                    )
                    val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                        putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    }
                    
                    // Check if there's a camera app available
                    if (takePictureIntent.resolveActivity(packageManager) != null) {
                        takePictureLauncher.launch(takePictureIntent)
                    } else {
                        Toast.makeText(this, "No camera app available", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: IllegalArgumentException) {
                    Log.e("VendorDetailActivity", "FileProvider error", e)
                    Toast.makeText(this, "File provider configuration error", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (ex: Exception) {
            Log.e("VendorDetailActivity", "Error taking picture", ex)
            Toast.makeText(this, "Failed to take picture: ${ex.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun createImageFile(): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "JPEG_" + timeStamp + "_"
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            
            if (storageDir == null) {
                Log.e("VendorDetailActivity", "External storage directory is null")
                return null
            }
            
            // Ensure the directory exists
            if (!storageDir.exists()) {
                if (!storageDir.mkdirs()) {
                    Log.e("VendorDetailActivity", "Failed to create storage directory")
                    return null
                }
            }
            
            val image = File.createTempFile(imageFileName, ".jpg", storageDir)
            currentPhotoPath = image.absolutePath
            image
        } catch (e: IOException) {
            Log.e("VendorDetailActivity", "Error creating image file", e)
            null
        } catch (e: SecurityException) {
            Log.e("VendorDetailActivity", "Security error creating image file", e)
            null
        }
    }
    
    private fun pickImageFromGallery() {
        try {
            // Check if we have permission to read external storage (for Android 10 and below)
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
                    != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        GALLERY_PERMISSION_REQUEST_CODE
                    )
                    return
                }
            }
            
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png", "image/jpg"))
            }
            
            // Check if there's an app that can handle this intent
            if (intent.resolveActivity(packageManager) != null) {
                pickImageLauncher.launch(intent)
            } else {
                Toast.makeText(this, "No gallery app available", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("VendorDetailActivity", "Error opening gallery", e)
            Toast.makeText(this, "Failed to open gallery: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    takePicture()
                } else {
                    Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
                }
            }
            GALLERY_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pickImageFromGallery()
                } else {
                    Toast.makeText(this, "Storage permission is required to select photos", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
        private const val GALLERY_PERMISSION_REQUEST_CODE = 101
    }
}