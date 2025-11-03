package com.example.vendorconnect

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AboutActivity : AppCompatActivity() {
    
    private lateinit var backButton: ImageView
    private lateinit var websiteText: TextView
    private lateinit var emailText: TextView
    private lateinit var versionText: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        
        // Initialize views
        backButton = findViewById(R.id.backButton)
        websiteText = findViewById(R.id.websiteText)
        emailText = findViewById(R.id.emailText)
        versionText = findViewById(R.id.versionText)
        
        // Set click listeners
        backButton.setOnClickListener {
            finish()
        }
        
        websiteText.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://vendorconnect.com"))
            startActivity(intent)
        }
        
        emailText.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("mailto:support@vendorconnect.com")
            startActivity(intent)
        }
        
        // Set version
        versionText.text = "Version 1.0.0"
    }
}
