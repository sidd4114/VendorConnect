package com.example.vendorconnect

import android.app.Application
import android.content.pm.PackageManager
import com.google.firebase.FirebaseApp
import com.google.android.libraries.places.api.Places

class VendorConnectApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        try {
            if (!Places.isInitialized()) {
                val ai = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
                val apiKey = ai.metaData.getString("com.google.android.geo.API_KEY") ?: ""
                if (apiKey.isNotEmpty()) {
                    Places.initialize(applicationContext, apiKey)
                }
            }
        } catch (_: Exception) { }
    }
}
