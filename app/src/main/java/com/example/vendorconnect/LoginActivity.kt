package com.example.vendorconnect

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    
    private lateinit var auth: FirebaseAuth
    private lateinit var phoneEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: MaterialButton
    private lateinit var registerTextView: TextView
    private lateinit var logoutButton: Button
    private var loadingDialog: AlertDialog? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        
        // Initialize views
        phoneEditText = findViewById(R.id.phoneEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.loginButton)
        registerTextView = findViewById(R.id.registerTextView)
        logoutButton = findViewById(R.id.logoutButton)
        
        // Set click listeners
        loginButton.setOnClickListener {
            loginUser()
        }
        
        registerTextView.setOnClickListener {
            val intent = Intent(this, ClientRegisterActivity::class.java)
            startActivity(intent)
        }
        
        logoutButton.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            logoutButton.visibility = android.view.View.GONE
        }
    }
    
    private fun loginUser() {
        val phone = phoneEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        
        if (phone.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (phone.length < 10) {
            Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Convert phone to email format for Firebase Auth
        val loginEmail = phone + "@test.com"
        
        // Show loading dialog
        showLoadingDialog()
        
        auth.signInWithEmailAndPassword(loginEmail, password)
            .addOnCompleteListener(this) { task ->
                hideLoadingDialog()
                
                if (task.isSuccessful) {
                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                    
                    // Navigate to MainActivity
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
    
    private fun showLoadingDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.loading_dialog, null)
        loadingDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        loadingDialog?.show()
    }
    
    private fun hideLoadingDialog() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }
    
    override fun onStart() {
        super.onStart()
        // Check if user is already logged in and show logout option
        val currentUser = auth.currentUser
        if (currentUser != null) {
            logoutButton.visibility = android.view.View.VISIBLE
            Toast.makeText(this, "You are already logged in. Click 'Logout Current User' to switch accounts.", Toast.LENGTH_LONG).show()
        } else {
            logoutButton.visibility = android.view.View.GONE
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        hideLoadingDialog()
    }
}
