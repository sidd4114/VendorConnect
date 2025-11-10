package com.example.vendorconnect

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.animation.ObjectAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class VendorMainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore


    // UI Components
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var btnHamburger: ImageButton
    private lateinit var btnPinLocation: com.google.android.material.button.MaterialButton
    private lateinit var btnManualLocation: com.google.android.material.button.MaterialButton
    private lateinit var locationStatusText: TextView
    private lateinit var btnToggleOverlay: com.google.android.material.button.MaterialButton
    private lateinit var vendorGreetingText: TextView
    private lateinit var vendorNameText: TextView
    private lateinit var statusIndicator: LinearLayout
    private lateinit var statusDot: View
    private lateinit var locationStatusBadge: TextView
    private lateinit var businessHoursText: TextView
    private lateinit var btnRefresh: ImageButton
    private lateinit var btnCloseDashboard: ImageButton
    private lateinit var btnSettings: com.google.android.material.floatingactionbutton.FloatingActionButton

    // Location display components
    private lateinit var locationCard: CardView
    private lateinit var locationDetailsText: TextView
    private lateinit var coordinatesText: TextView
    private lateinit var dashboardCard: CardView

    private var currentLocation: Location? = null
    private val locationPermissionRequestCode = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vendor_main)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Check if user is logged in
        if (auth.currentUser == null) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize UI components
        initializeViews()
        setupClickListeners()
        setupNavigationDrawer()

        // Setup Google Map
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Load vendor data
        loadVendorData()
    }

    private fun initializeViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        btnHamburger = findViewById(R.id.btnHamburger)
        btnPinLocation = findViewById(R.id.btnPinLocation)
        btnManualLocation = findViewById(R.id.btnManualLocation)
        locationStatusText = findViewById(R.id.locationStatusText)
        btnToggleOverlay = findViewById(R.id.btnToggleOverlay)
        vendorGreetingText = findViewById(R.id.vendorGreetingText)
        vendorNameText = findViewById(R.id.vendorNameText)
        statusIndicator = findViewById(R.id.statusIndicator)
        statusDot = findViewById<View>(R.id.statusDot)
        locationStatusBadge = findViewById(R.id.locationStatusBadge)
        businessHoursText = findViewById(R.id.businessHoursText)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnCloseDashboard = findViewById(R.id.btnCloseDashboard)
        btnSettings = findViewById(R.id.btnSettings)

        // Location display components
        locationCard = findViewById(R.id.locationCard)
        locationDetailsText = findViewById(R.id.locationDetailsText)
        coordinatesText = findViewById(R.id.coordinatesText)
        dashboardCard = findViewById(R.id.dashboardCard)

        // Set initial button text (dashboard starts hidden)
        btnToggleOverlay.text = "Show Dashboard"
    }

    private fun setupClickListeners() {
        btnHamburger.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        btnPinLocation.setOnClickListener {
            pinCurrentLocation()
        }

        btnManualLocation.setOnClickListener {
            showManualLocationDialog()
        }

        btnToggleOverlay.setOnClickListener {
            toggleDashboard()
        }

        btnRefresh.setOnClickListener {
            refreshDashboard()
        }

        btnCloseDashboard.setOnClickListener {
            hideDashboardWithAnimation()
        }

        btnSettings.setOnClickListener {
            // Open settings
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun toggleDashboard() {
        if (dashboardCard.visibility == android.view.View.VISIBLE) {
            // Hide dashboard
            hideDashboardWithAnimation()
        } else {
            // Show dashboard
            showDashboardWithAnimation()
        }
    }

    private fun showDashboardWithAnimation() {
        // Show dashboard card
        dashboardCard.visibility = android.view.View.VISIBLE
        vendorGreetingText.visibility = android.view.View.VISIBLE
        btnToggleOverlay.text = "Hide Dashboard"

        // Slide down dashboard from top
        val slideDown = ObjectAnimator.ofFloat(dashboardCard, "translationY", -dashboardCard.height.toFloat(), 0f)
        val dashboardFadeIn = ObjectAnimator.ofFloat(dashboardCard, "alpha", 0f, 1f)

        slideDown.duration = 400
        dashboardFadeIn.duration = 400

        slideDown.interpolator = AccelerateDecelerateInterpolator()
        dashboardFadeIn.interpolator = AccelerateDecelerateInterpolator()

        slideDown.start()
        dashboardFadeIn.start()
    }

    private fun hideDashboardWithAnimation() {
        btnToggleOverlay.text = "Show Dashboard"

        // Slide up dashboard to top
        val slideUp = ObjectAnimator.ofFloat(dashboardCard, "translationY", 0f, -dashboardCard.height.toFloat())
        val dashboardFadeOut = ObjectAnimator.ofFloat(dashboardCard, "alpha", 1f, 0f)

        slideUp.duration = 300
        dashboardFadeOut.duration = 300

        slideUp.interpolator = AccelerateDecelerateInterpolator()
        dashboardFadeOut.interpolator = AccelerateDecelerateInterpolator()

        slideUp.start()
        dashboardFadeOut.start()

        // Hide after animation completes
        slideUp.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                dashboardCard.visibility = android.view.View.GONE
                vendorGreetingText.visibility = android.view.View.GONE
                dashboardCard.alpha = 1f
                dashboardCard.translationY = 0f
            }
        })
    }

    private fun refreshDashboard() {
        // Refresh dashboard data
        loadVendorData()
        updateLocationStatus()
        Toast.makeText(this, "Dashboard refreshed!", Toast.LENGTH_SHORT).show()
    }

    private fun updateDashboardStats() {
        // Update business hours
        businessHoursText.text = "9:00 AM - 9:00 PM"
    }

    private fun updateLocationStatus() {
        // Check if location is set and update status indicators
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val isLocationSet = document.getBoolean("isLocationSet") ?: false
                        if (isLocationSet) {
                            // Location is set - show online status
                            statusDot.setBackground(getDrawable(R.drawable.status_dot_online))
                            locationStatusText.text = "Online"
                            locationStatusBadge.text = "Set"
                            locationStatusBadge.setBackgroundResource(R.drawable.status_badge_online)
                        } else {
                            // Location not set - show offline status
                            statusDot.setBackground(getDrawable(R.drawable.status_dot_offline))
                            locationStatusText.text = "Offline"
                            locationStatusBadge.text = "Not Set"
                            locationStatusBadge.setBackgroundResource(R.drawable.status_badge_offline)
                        }
                    }
                }
        }
    }

    private fun setupNavigationDrawer() {
        // Set vendor-specific menu
        navigationView.menu.clear()
        navigationView.inflateMenu(R.menu.drawer_menu_vendor)

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                }
                R.id.nav_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    startActivity(intent)
                }
                R.id.nav_settings -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                }
                R.id.nav_about -> {
                    val intent = Intent(this, AboutActivity::class.java)
                    startActivity(intent)
                }
                R.id.nav_help -> {
                    Toast.makeText(this, "Help & Support coming soon!", Toast.LENGTH_SHORT).show()
                }
                R.id.nav_logout -> {
                    auth.signOut()
                    Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun pinCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), locationPermissionRequestCode)
            return
        }

        // Show loading message
        Toast.makeText(this, "Getting your current location...", Toast.LENGTH_SHORT).show()

        // Check if location services are enabled and properly configured
        checkLocationSettings { isEnabled ->
            if (isEnabled) {
                // Location settings are good, proceed with getting location
                getCurrentLocation()
            } else {
                Toast.makeText(this, "Please enable location services and set to High Accuracy mode for best results", Toast.LENGTH_LONG).show()
                getCurrentLocation() // Try anyway, might still work
            }
        }
    }

    private fun checkLocationSettings(callback: (Boolean) -> Unit) {
        val locationRequest = com.google.android.gms.location.LocationRequest.create().apply {
            priority = com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true) // Show dialog to enable location if disabled

        val settingsClient: SettingsClient = LocationServices.getSettingsClient(this)
        val task = settingsClient.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            // All location settings are satisfied
            callback(true)
        }

        task.addOnFailureListener { exception ->
            // Location settings are not satisfied, but we can still try
            callback(false)
        }
    }

    private fun getCurrentLocation() {
        // First try to get last known location (fastest)
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null && location.accuracy < 50) { // Only use if accuracy is reasonable (< 50 meters)
                // We got a good location, use it
                currentLocation = location

                // Show current location on map first
                val currentLatLng = LatLng(location.latitude, location.longitude)

                // Clear existing markers and add current location marker
                mMap.clear()
                mMap.addMarker(
                    MarkerOptions()
                        .position(currentLatLng)
                        .title("Your Current Location")
                        .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(
                            com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_GREEN
                        ))
                )

                // Move camera to current location
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f))

                // Debug: Show the actual coordinates and accuracy
                Toast.makeText(this@VendorMainActivity, "Location found: ${String.format("%.6f", location.latitude)}, ${String.format("%.6f", location.longitude)} (Accuracy: ${location.accuracy}m)", Toast.LENGTH_LONG).show()

                showVendorDetailsDialog(location.latitude, location.longitude)
            } else {
                // No last known location or poor accuracy, request fresh location
                requestFreshLocation()
            }
        }.addOnFailureListener {
            // Failed to get last known location, request fresh location
            requestFreshLocation()
        }
    }

    private fun requestFreshLocation() {
        // Use a more reliable location request with enhanced accuracy
        val locationRequest = com.google.android.gms.location.LocationRequest.create().apply {
            priority = com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
            numUpdates = 1
            interval = 0
            fastestInterval = 0
            maxWaitTime = 15000 // 15 seconds max wait for better accuracy
            smallestDisplacement = 0f // Accept any location update
        }

        val locationCallback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                fusedLocationClient.removeLocationUpdates(this)
                val location = locationResult.lastLocation

                if (location != null) {
                    // Check if location accuracy is acceptable
                    if (location.accuracy <= 100) { // Accept locations with accuracy <= 100 meters
                        currentLocation = location

                        // Show current location on map first
                        val currentLatLng = LatLng(location.latitude, location.longitude)

                        // Clear existing markers and add current location marker
                        mMap.clear()
                        mMap.addMarker(
                            MarkerOptions()
                                .position(currentLatLng)
                                .title("Your Current Location")
                                .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(
                                    com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_GREEN
                                ))
                        )

                        // Move camera to current location
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f))

                        // Debug: Show the actual coordinates and accuracy
                        Toast.makeText(this@VendorMainActivity, "Fresh location: ${String.format("%.6f", location.latitude)}, ${String.format("%.6f", location.longitude)} (Accuracy: ${location.accuracy}m)", Toast.LENGTH_LONG).show()

                        showVendorDetailsDialog(location.latitude, location.longitude)
                    } else {
                        // Poor accuracy, show warning but still use it
                        currentLocation = location
                        Toast.makeText(this@VendorMainActivity, "Location accuracy is poor (${location.accuracy}m). Consider moving to an area with better GPS signal.", Toast.LENGTH_LONG).show()

                        // Still show on map
                        val currentLatLng = LatLng(location.latitude, location.longitude)
                        mMap.clear()
                        mMap.addMarker(
                            MarkerOptions()
                                .position(currentLatLng)
                                .title("Your Current Location (Poor Accuracy)")
                                .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(
                                    com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_ORANGE
                                ))
                        )
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f))

                        showVendorDetailsDialog(location.latitude, location.longitude)
                    }
                } else {
                    Toast.makeText(this@VendorMainActivity, "Unable to get current location", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Request location updates
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)

            // Timeout after 15 seconds to match maxWaitTime
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            fusedLocationClient.removeLocationUpdates(locationCallback)
            if (currentLocation == null) {
                Toast.makeText(this, "Location request timed out. Please try again.", Toast.LENGTH_LONG).show()
            }
        }, 15000)
        }
    }

    private fun showManualLocationDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_manual_location, null)
        val editTextSearch = dialogView.findViewById<EditText>(R.id.editTextLocationSearch)
        val editTextLatitude = dialogView.findViewById<EditText>(R.id.editTextLatitude)
        val editTextLongitude = dialogView.findViewById<EditText>(R.id.editTextLongitude)
        val recyclerViewResults = dialogView.findViewById<RecyclerView>(R.id.recyclerViewSearchResults)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancelManual)
        val btnSearch = dialogView.findViewById<Button>(R.id.btnSearchLocation)
        val btnUseCoordinates = dialogView.findViewById<Button>(R.id.btnUseCoordinates)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // Setup RecyclerView
        recyclerViewResults.layoutManager = LinearLayoutManager(this)
        var searchResults = mutableListOf<PlaceSearchResult>()
        var adapter = SearchResultAdapter(searchResults) { result ->
            dialog.dismiss()
            showVendorDetailsDialog(result.latitude, result.longitude)
        }
        recyclerViewResults.adapter = adapter

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSearch.setOnClickListener {
            val query = editTextSearch.text.toString().trim()
            if (query.isNotEmpty()) {
                // Show loading
                btnSearch.text = "Searching..."
                btnSearch.isEnabled = false

                searchPlaces(query) { results ->
                    runOnUiThread {
                        searchResults.clear()
                        searchResults.addAll(results)
                        adapter.notifyDataSetChanged()

                        // Reset button
                        btnSearch.text = "Search"
                        btnSearch.isEnabled = true

                        if (results.isEmpty()) {
                            Toast.makeText(this, "No results found for '$query'", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Please enter a search query", Toast.LENGTH_SHORT).show()
            }
        }

        btnUseCoordinates.setOnClickListener {
            val latText = editTextLatitude.text.toString().trim()
            val lngText = editTextLongitude.text.toString().trim()

            if (latText.isNotEmpty() && lngText.isNotEmpty()) {
                try {
                    val latitude = latText.toDouble()
                    val longitude = lngText.toDouble()

                    if (latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180) {
                        dialog.dismiss()
                        showVendorDetailsDialog(latitude, longitude)
                    } else {
                        Toast.makeText(this, "Invalid coordinates. Latitude: -90 to 90, Longitude: -180 to 180", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: NumberFormatException) {
                    Toast.makeText(this, "Please enter valid numbers for coordinates", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please enter both latitude and longitude", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun searchPlaces(query: String, callback: (List<PlaceSearchResult>) -> Unit) {
        Executors.newSingleThreadExecutor().execute {
            try {
                // Get API key from manifest
                val apiKey = "AIzaSyBOBqbUU_WbGeKix-WnuJcmsydHUfUkWwY"
                val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                val url = "https://maps.googleapis.com/maps/api/place/textsearch/json?query=$encodedQuery&key=$apiKey"

                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(response)

                val results = mutableListOf<PlaceSearchResult>()
                val status = jsonResponse.getString("status")

                runOnUiThread {
                    Toast.makeText(this, "Search status: $status", Toast.LENGTH_SHORT).show()
                }

                if (status == "OK") {
                    val places = jsonResponse.getJSONArray("results")
                    for (i in 0 until minOf(places.length(), 5)) { // Limit to 5 results
                        val place = places.getJSONObject(i)
                        val name = place.getString("name")
                        val address = place.getString("formatted_address")
                        val location = place.getJSONObject("geometry").getJSONObject("location")
                        val lat = location.getDouble("lat")
                        val lng = location.getDouble("lng")

                        results.add(PlaceSearchResult(name, address, lat, lng))
                    }

                    runOnUiThread {
                        Toast.makeText(this, "Found ${results.size} results", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorMessage = jsonResponse.optString("error_message", "Unknown error")
                    runOnUiThread {
                        Toast.makeText(this, "API error: $errorMessage. Using sample results.", Toast.LENGTH_LONG).show()
                    }
                    // Fallback to sample results
                    results.addAll(getSampleSearchResults(query))
                }

                callback(results)
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Search failed: ${e.message}. Using sample results.", Toast.LENGTH_LONG).show()
                }
                // Fallback to sample results
                callback(getSampleSearchResults(query))
            }
        }
    }

    private fun getSampleSearchResults(query: String): List<PlaceSearchResult> {
        val sampleResults = listOf(
            PlaceSearchResult("Times Square", "Times Square, New York, NY, USA", 40.7580, -73.9855),
            PlaceSearchResult("Central Park", "Central Park, New York, NY, USA", 40.7829, -73.9654),
            PlaceSearchResult("Eiffel Tower", "Champ de Mars, 7th arrondissement, Paris, France", 48.8584, 2.2945),
            PlaceSearchResult("Big Ben", "Westminster, London SW1A 0AA, UK", 51.4994, -0.1245),
            PlaceSearchResult("Sydney Opera House", "Bennelong Point, Sydney NSW 2000, Australia", -33.8568, 151.2153)
        )

        // Filter sample results based on query
        return sampleResults.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.address.contains(query, ignoreCase = true)
        }.take(3)
    }

    private fun showVendorDetailsDialog(latitude: Double, longitude: Double) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_vendor_details, null)
        val editTextStallName = dialogView.findViewById<EditText>(R.id.editTextStallName)
        val editTextStallType = dialogView.findViewById<EditText>(R.id.editTextStallType)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val stallName = editTextStallName.text.toString().trim()
            val stallType = editTextStallType.text.toString().trim()

            if (stallName.isEmpty() || stallType.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            dialog.dismiss()
            updateLocationInFirestore(latitude, longitude, stallName, stallType)
        }

        dialog.show()
    }

    private fun updateLocationInFirestore(latitude: Double, longitude: Double, stallName: String, stallType: String) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val locationData = hashMapOf<String, Any>(
                "latitude" to latitude,
                "longitude" to longitude,
                "stallName" to stallName,
                "stallType" to stallType,
                "isLocationSet" to true,
                "lastUpdated" to System.currentTimeMillis()
            )

            // Update UI IMMEDIATELY before saving to database
            updateMapLocation(latitude, longitude)
            updateLocationDisplay(stallName, stallType, latitude, longitude)
            locationStatusText.text = "Location pinned successfully!"
            btnPinLocation.text = "Update Location"
            locationCard.visibility = android.view.View.VISIBLE

            // Then save to database
            db.collection("users").document(userId).update(locationData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Location and details updated successfully!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to save to database: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateMapLocation(latitude: Double, longitude: Double) {
        val location = LatLng(latitude, longitude)

        // Clear existing markers
        mMap.clear()

        // Add new marker
        mMap.addMarker(MarkerOptions().position(location).title("Your Location"))

        // Move camera to location with animation
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))

        // Debug: Show coordinates
        Toast.makeText(this, "Map updated to: ${String.format("%.6f", latitude)}, ${String.format("%.6f", longitude)}", Toast.LENGTH_SHORT).show()
    }

    private fun updateLocationDisplay(stallName: String, stallType: String, latitude: Double, longitude: Double) {
        locationCard.visibility = android.view.View.VISIBLE
        locationDetailsText.text = "$stallName • $stallType"
        coordinatesText.text = "Coordinates: ${String.format("%.6f", latitude)}, ${String.format("%.6f", longitude)}"

        // Debug: Show coordinates in toast for verification (only if coordinates seem wrong)
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            Toast.makeText(this, "Invalid coordinates: ${String.format("%.6f", latitude)}, ${String.format("%.6f", longitude)}", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadExistingLocation() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val isLocationSet = document.getBoolean("isLocationSet") ?: false
                        if (isLocationSet) {
                            val stallName = document.getString("stallName") ?: "Unknown Stall"
                            val stallType = document.getString("stallType") ?: "Unknown Type"
                            val latitude = document.getDouble("latitude") ?: 0.0
                            val longitude = document.getDouble("longitude") ?: 0.0

                            if (latitude != 0.0 && longitude != 0.0) {
                                updateLocationDisplay(stallName, stallType, latitude, longitude)
                                updateMapLocation(latitude, longitude)
                                locationStatusText.text = "Location is set"
                                btnPinLocation.text = "Update Location"
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to load location: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadVendorData() {
        // Show vendor greeting
        vendorGreetingText.visibility = android.view.View.VISIBLE

        // Load vendor name and existing location
        loadVendorName()
        loadExistingLocation()
        updateDashboardStats()
    }

    private fun loadVendorName() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val fullName = document.getString("fullName") ?: "Vendor"
                        vendorGreetingText.text = "Welcome Back!"
                        vendorNameText.text = fullName
                    } else {
                        vendorGreetingText.text = "Welcome Back!"
                        vendorNameText.text = "Vendor"
                    }
                }
                .addOnFailureListener { e ->
                    vendorGreetingText.text = "Welcome Back!"
                    vendorNameText.text = "Vendor"
                }
        } else {
            vendorGreetingText.text = "Welcome Back!"
            vendorNameText.text = "Vendor"
        }
    }

    private fun refreshVendorData() {
        // Reload existing location data to refresh the display
        loadExistingLocation()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Enable location button and show current location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true

            // Get current location and show it on map
            getCurrentLocationAndShowOnMap()
        }

        // Load existing vendor location if available
        loadExistingLocation()

        // Debug: Show map ready message
        Toast.makeText(this, "Map is ready!", Toast.LENGTH_SHORT).show()
    }

    private fun getCurrentLocationAndShowOnMap() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)

                    // Move camera to current location
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f))

                    // Add a marker for current location
                    mMap.addMarker(
                        MarkerOptions()
                            .position(currentLatLng)
                            .title("Your Current Location")
                            .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(
                                com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_BLUE
                            ))
                    )

                    Toast.makeText(this, "Current location found!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to get current location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pinCurrentLocation()
            } else {
                Toast.makeText(this, "Location permission is required to pin your location", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh location data when returning to the activity
        loadExistingLocation()
    }
}
