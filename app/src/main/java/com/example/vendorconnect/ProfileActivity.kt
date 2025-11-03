package com.example.vendorconnect

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {
    
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var profileImage: ImageView
    private lateinit var userNameText: TextView
    private lateinit var userPhoneText: TextView
    private lateinit var userEmailText: TextView
    private lateinit var editProfileButton: Button
    private lateinit var backButton: ImageView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        
        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        
        // Initialize views
        profileImage = findViewById(R.id.profileImage)
        userNameText = findViewById(R.id.userNameText)
        userPhoneText = findViewById(R.id.userPhoneText)
        userEmailText = findViewById(R.id.userEmailText)
        editProfileButton = findViewById(R.id.editProfileButton)
        backButton = findViewById(R.id.backButton)
        
        // Set click listeners
        editProfileButton.setOnClickListener {
            // TODO: Open edit profile dialog
            Toast.makeText(this, "Edit Profile feature coming soon!", Toast.LENGTH_SHORT).show()
        }
        
        backButton.setOnClickListener {
            finish()
        }
        
        // Load user data
        loadUserData()
    }
    
    private fun loadUserData() {
        val user = auth.currentUser
        if (user != null) {
            val userId = user.uid
            
            // Load user data from Firestore
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val fullName = document.getString("fullName") ?: "User Name"
                        val phone = document.getString("phone") ?: "Not available"
                        val email = user.email ?: "Not provided"
                        
                        userNameText.text = fullName
                        userPhoneText.text = phone
                        userEmailText.text = email
                    } else {
                        // Document doesn't exist, use default values
                        userNameText.text = "User Name"
                        userPhoneText.text = "Not available"
                        userEmailText.text = user.email ?: "Not provided"
                    }
                }
                .addOnFailureListener { e ->
                    // Error loading data, use default values
                    userNameText.text = "User Name"
                    userPhoneText.text = "Not available"
                    userEmailText.text = user.email ?: "Not provided"
                    Toast.makeText(this, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            // User not logged in, redirect to login
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
