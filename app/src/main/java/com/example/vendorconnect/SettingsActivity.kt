package com.example.vendorconnect

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var backButton: ImageView
    private lateinit var notificationsSwitch: Switch
    private lateinit var locationSwitch: Switch
    private lateinit var privacyText: TextView
    private lateinit var termsText: TextView
    private lateinit var versionText: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        // Initialize views
        backButton = findViewById(R.id.backButton)
        notificationsSwitch = findViewById(R.id.notificationsSwitch)
        locationSwitch = findViewById(R.id.locationSwitch)
        privacyText = findViewById(R.id.privacyText)
        termsText = findViewById(R.id.termsText)
        versionText = findViewById(R.id.versionText)
        
        // Set click listeners
        backButton.setOnClickListener {
            finish()
        }
        
        privacyText.setOnClickListener {
            // TODO: Open privacy policy
            // For now, just show a toast
            android.widget.Toast.makeText(this, "Privacy Policy", android.widget.Toast.LENGTH_SHORT).show()
        }
        
        termsText.setOnClickListener {
            // TODO: Open terms of service
            // For now, just show a toast
            android.widget.Toast.makeText(this, "Terms of Service", android.widget.Toast.LENGTH_SHORT).show()
        }
        
        // Set version
        versionText.text = "Version 1.0.0"
    }
}
