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

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Add the photo to the list
            photosList.add(currentPhotoPath)
            updatePhotosList()
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                // Convert URI to a file path or store the URI directly
                val imagePath = uri.toString()
                photosList.add(imagePath)
                updatePhotosList()
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
        photoAdapter = PhotoAdapter(photosList) { photoUrl ->
            showFullScreenPhoto(photoUrl)
        }
        photosRecyclerView.adapter = photoAdapter
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