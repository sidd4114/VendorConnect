package com.example.vendorconnect

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {
    
    private lateinit var auth: FirebaseAuth
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        
        // Check if user is already logged in
        checkUserLoginStatus()
    }
    
    private fun checkUserLoginStatus() {
        // Add a small delay for splash screen effect
        Handler(Looper.getMainLooper()).postDelayed({
            val currentUser = auth.currentUser
            
            if (currentUser != null) {
                // User is logged in, redirect to appropriate home page
                redirectToHomePage()
            } else {
                // User is not logged in, go to login page
                redirectToLogin()
            }
        }, 2000) // 2 second delay
    }
    
    private fun redirectToHomePage() {
        // For now, redirect to MainActivity (customer home)
        // In a real app, you might check user type and redirect accordingly
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
    
    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
