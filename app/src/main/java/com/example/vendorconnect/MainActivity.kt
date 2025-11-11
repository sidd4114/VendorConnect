package com.example.vendorconnect

// ... (Your imports remain exactly the same)
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.vendorconnect.chat.ChatActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.common.api.ResolvableApiException
import android.location.LocationManager
import android.provider.Settings
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
// Google Places imports for location search
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    // ... (All your properties from line 28 to 110 remain exactly the same)
    private lateinit var mMap: GoogleMap
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: VendorAdapter
    private lateinit var vendorList: List<Vendor>
    private lateinit var searchBar: EditText
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val AUTOCOMPLETE_REQUEST_CODE = 1002
    }

    // Filter chips
    private lateinit var chipAll: TextView
    private lateinit var chipFood: TextView
    private lateinit var chipCafe: TextView
    private lateinit var chipHealth: TextView
    private lateinit var chipOpen: TextView
    private var currentFilter = "All"

    // User's current location
    private var userLocation: Location? = null

    // Search filters
    private var selectedCategories: MutableSet<String> = mutableSetOf()
    private var showOnlyOpen: Boolean = false

    // Drawer components
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var btnHamburger: ImageButton
    private lateinit var btnStats: com.google.android.material.floatingactionbutton.FloatingActionButton
    private lateinit var btnToggleVendors: com.google.android.material.floatingactionbutton.FloatingActionButton
    private lateinit var btnMyLocation: com.google.android.material.floatingactionbutton.FloatingActionButton
    private lateinit var overlayLayout: LinearLayout
    private lateinit var slideIn: android.view.animation.Animation
    private lateinit var slideOut: android.view.animation.Animation
    private var isVendorListVisible = true
    private var pendingCameraLatLng: LatLng? = null

    // Firebase Auth
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Location services
    private var locationCallback: LocationCallback? = null
    private var isRequestingLocationUpdates = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_down)
        slideOut = AnimationUtils.loadAnimation(this, R.anim.slide_out_up)

        // Check if user is logged in
        if (auth.currentUser == null) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        // Check user type BEFORE setting up UI
        checkUserTypeAndRedirect()
    }

    // ... (Your onMapReady, enableMyLocation, requestLocationUpdates, updateLocation, generateVendorsNearLocation, stopLocationUpdates, and applyFilters methods from line 133 to 457 remain exactly the same)
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Set default location to Thane instead of Google HQ
        val mumbaiLocation = LatLng(19.2183, 72.9781)

        // Ensure the map starts focused and avoids world view flicker
        mMap.setMinZoomPreference(10f)
        mMap.setMaxZoomPreference(20f)

        // Completely prevent Google HQ from showing by setting map options
        try {
            mMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    this, R.raw.map_style
                )
            )
        } catch (e: Exception) {
            Log.e("MainActivity", "Can't find style. Error: ", e)
        }

        // Configure map UI settings
        mMap.uiSettings.isMapToolbarEnabled = false
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isCompassEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = false // We use custom FAB

        // Immediately move camera to Thane to prevent showing Google HQ
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mumbaiLocation, 12f))

        // Don't set default location here - wait for actual location
        // Vendors will be generated when actual location is obtained

        // Add markers for vendors (if vendors are already loaded)
        if (::vendorList.isInitialized && vendorList.isNotEmpty()) {
            for (vendor in vendorList) {
                val location = LatLng(vendor.lat, vendor.lng)
                mMap.addMarker(MarkerOptions().position(location).title(vendor.name))
            }
        } else {
            // Show message that vendors are being loaded
            Toast.makeText(this, "Getting your location to show nearby vendors...", Toast.LENGTH_LONG).show()
        }

        // Enable location layer if permission is granted
        enableMyLocation()

        // If a place was selected before the map was ready, move now
        pendingCameraLatLng?.let { latLng ->
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
            pendingCameraLatLng = null
        }
    }

    private fun enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true

            // Check if GPS is enabled
            val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!isGpsEnabled && !isNetworkEnabled) {
                // Show dialog to enable location services
                AlertDialog.Builder(this)
                    .setTitle("Location Services Disabled")
                    .setMessage("Please enable location services (GPS and network) to find nearby vendors.")
                    .setPositiveButton("Settings") { _, _ ->
                        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        startActivity(intent)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                return
            }

            // Request location with proper configuration
            requestLocationUpdates()

            // Also try to get last known location immediately as fallback
            fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                if (lastLoc != null && userLocation == null) {
                    // Use last known location if we don't have a fresh one
                    updateLocation(lastLoc)
                }
            }
        } else {
            // Request both fine and coarse location permissions
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // Stop any existing updates
        stopLocationUpdates()

        // Create location request
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setMinUpdateIntervalMillis(5000)
            .setMaxUpdateDelayMillis(10000)
            .setWaitForAccurateLocation(false)
            .build()

        // Create location callback
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    updateLocation(location)
                    // Stop updates after getting first accurate location
                    stopLocationUpdates()
                }
            }
        }

        // Check location settings
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true)

        val settingsClient: SettingsClient = LocationServices.getSettingsClient(this)
        val task = settingsClient.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            // Location settings are satisfied, request location updates
            isRequestingLocationUpdates = true
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                mainLooper
            )
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                // Location settings are not satisfied, show dialog to user
                try {
                    exception.startResolutionForResult(this, LOCATION_PERMISSION_REQUEST_CODE + 1)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error showing location settings dialog", e)
                }
            }
            // Fallback to last known location
            fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                lastLoc?.let { updateLocation(it) }
            }
        }

        // Fallback timeout: if no location received in 15 seconds, use last known location
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (userLocation == null) {
                fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                    lastLoc?.let { updateLocation(it) }
                }
            }
            stopLocationUpdates()
        }, 15000)
    }

    private fun updateLocation(location: Location) {
        val previousLocation = userLocation
        
        if (userLocation != null && location.accuracy > userLocation!!.accuracy) {
            // Don't update if new location is less accurate
            return
        }

        val currentLatLng = LatLng(location.latitude, location.longitude)

        if (this::mMap.isInitialized) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f))
        } else {
            pendingCameraLatLng = currentLatLng
        }

        userLocation = location

        // Generate vendors near new location if:
        // 1. Not generated yet, OR
        // 2. Location changed significantly (more than 500 meters)
        val shouldRegenerateVendors = !::vendorList.isInitialized || 
                                     vendorList.isEmpty() || 
                                     (previousLocation != null && calculateDistance(
                                         previousLocation.latitude, 
                                         previousLocation.longitude,
                                         location.latitude, 
                                         location.longitude
                                     ) > 0.5) // 500 meters

        if (shouldRegenerateVendors) {
            vendorList = generateVendorsNearLocation(location)
            // Note: Real vendors are loaded asynchronously in loadRealVendorsFromFirebase
            // and will be added to the list and UI automatically
        }

        val query = if (::searchBar.isInitialized) searchBar.text.toString() else ""
        filterVendors(query)

        Log.d("MainActivity", "Location updated: ${location.latitude}, ${location.longitude}, accuracy: ${location.accuracy}m")
    }

    // Calculate distance between two coordinates using Haversine formula
    private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val earthRadius = 6371.0 // Earth's radius in kilometers
        val latDiff = Math.toRadians(lat2 - lat1)
        val lngDiff = Math.toRadians(lng2 - lng1)
        
        val a = Math.sin(latDiff / 2) * Math.sin(latDiff / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(lngDiff / 2) * Math.sin(lngDiff / 2)
        
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadius * c
    }

    private fun generateVendorsNearLocation(userLocation: Location?): List<Vendor> {
        // Default location if user location not available
        val baseLat = userLocation?.latitude ?: 19.2183
        val baseLng = userLocation?.longitude ?: 72.9781

        // Generate vendors around user location
        val vendors = mutableListOf<Vendor>()
        
        // First, load real vendors from Firebase
        loadRealVendorsFromFirebase(vendors, userLocation)
        
        // Then add dummy vendors if needed (only if we don't have enough real vendors)
        if (vendors.size < 5) {
            addDummyVendors(vendors, baseLat, baseLng, userLocation)
        }

        return vendors
    }

    private fun loadRealVendorsFromFirebase(vendors: MutableList<Vendor>, userLocation: Location?) {
        // Load real vendors from Firestore
        db.collection("users")
            .whereEqualTo("isLocationSet", true)
            .get()
            .addOnSuccessListener { documents ->
                val realVendors = mutableListOf<Vendor>()
                
                for (document in documents) {
                    val latitude = document.getDouble("latitude") ?: continue
                    val longitude = document.getDouble("longitude") ?: continue
                    val stallName = document.getString("stallName") ?: "Unknown Stall"
                    val stallType = document.getString("stallType") ?: "Unknown Type"
                    
                    // Only include vendors near the customer (within 5km)
                    if (userLocation != null) {
                        val distance = calculateDistance(
                            userLocation.latitude, userLocation.longitude,
                            latitude, longitude
                        )
                        if (distance > 5.0) { // Skip if more than 5km away
                            continue
                        }
                    }
                    
                    val distanceKm = if (userLocation != null) {
                        calculateDistance(
                            userLocation.latitude, userLocation.longitude,
                            latitude, longitude
                        ).toFloat()
                    } else 1.0f
                    
                    realVendors.add(
                        Vendor(
                            name = stallName,
                            description = "Real vendor location from Firebase",
                            lat = latitude,
                            lng = longitude,
                            locality = "Real Location",
                            category = stallType,
                            rating = 4.0f + (Math.random() * 1.0f).toFloat(), // Random rating 4.0-5.0
                            priceRange = "$",
                            isOpen = Math.random() > 0.3, // 70% chance of being open
                            distance = distanceKm,
                            phoneNumber = "+91 98765 ${40000 + (Math.random() * 10000).toInt()}",
                            address = "Real vendor location",
                            imageUrl = "https://images.unsplash.com/photo-${1517248135467 + vendors.size}?ixlib=rb-1.2.1&auto=format&fit=crop&w=800&q=60",
                            photoUrls = listOf(
                                "https://images.unsplash.com/photo-${1517248135467 + vendors.size}?ixlib=rb-1.2.1&auto=format&fit=crop&w=800&q=60"
                            ),
                            specialties = listOf("Real", "Authentic", "Local")
                        )
                    )
                }
                
                // Add real vendors to the main list
                vendors.addAll(realVendors)
                
                // Update the UI with combined vendors
                vendorList = vendors
                if (::adapter.isInitialized) {
                    adapter.updateList(vendorList)
                }
                
                // Add markers to map
                if (this::mMap.isInitialized && vendorList.isNotEmpty()) {
                    mMap.clear() // Clear old markers
                    for (vendor in vendorList) {
                        val vendorLocation = LatLng(vendor.lat, vendor.lng)
                        val markerOptions = MarkerOptions()
                            .position(vendorLocation)
                            .title(vendor.name)
                        
                        // Add special marker for real vendors
                        if (vendor.locality == "Real Location") {
                            markerOptions.snippet("Real Vendor")
                        }
                        
                        mMap.addMarker(markerOptions)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("MainActivity", "Error loading real vendors: ${e.message}")
            }
    }

    private fun addDummyVendors(vendors: MutableList<Vendor>, baseLat: Double, baseLng: Double, userLocation: Location?) {
        val vendorData = listOf(
            VendorData("Local Chai Corner", "Authentic cutting chai and fresh snacks. Open 24/7!", "Food & Beverage", 4.5f, "$", true, listOf("Fresh", "24/7", "Authentic")),
            VendorData("Fresh Sandwich Hub", "Custom sandwiches made with fresh ingredients. Perfect for breakfast!", "Food & Beverage", 4.2f, "$$", true, listOf("Fresh", "Custom", "Healthy")),
            VendorData("Green Juice Bar", "Organic cold-pressed juices and smoothies. Detox and energize!", "Health & Wellness", 4.7f, "$$$", false, listOf("Organic", "Healthy", "Detox")),
            VendorData("Street Food Paradise", "Traditional street food with a modern twist. Must-try specialties!", "Food & Beverage", 4.8f, "$$", true, listOf("Traditional", "Spicy", "Popular")),
            VendorData("Coffee & Co", "Artisanal coffee and light snacks. Perfect for work meetings!", "Café", 4.3f, "$$", true, listOf("Coffee", "Cozy", "Work-friendly")),
            VendorData("Quick Bites Express", "Fast food and quick snacks. Open late for midnight cravings!", "Fast Food", 3.9f, "$", true, listOf("Fast", "Late Night", "Budget")),
            VendorData("Pizza Palace", "Wood-fired pizzas and Italian favorites. Fresh ingredients daily!", "Food & Beverage", 4.6f, "$$", true, listOf("Italian", "Fresh", "Popular")),
            VendorData("Sweet Treats", "Delicious desserts and baked goods. Perfect for satisfying your sweet tooth!", "Food & Beverage", 4.4f, "$$", true, listOf("Desserts", "Fresh", "Sweet")),
            VendorData("Burger Junction", "Juicy burgers and crispy fries. Classic fast food favorites!", "Fast Food", 4.1f, "$", true, listOf("Burgers", "Fast", "Popular")),
            VendorData("Health Hub", "Nutritious meals and protein bowls. Fuel your active lifestyle!", "Health & Wellness", 4.9f, "$$$", true, listOf("Healthy", "Protein", "Fitness"))
        )

        var phoneCounter = 43210
        vendorData.forEachIndexed { index, data ->
            // Generate random offset for vendor locations
            val randomOffsetLat = (Math.random() * 0.01 - 0.005) // Small random offset
            val randomOffsetLng = (Math.random() * 0.01 - 0.005)
            val distanceKm = if (userLocation != null) {
                calculateDistance(
                    userLocation.latitude, userLocation.longitude,
                    baseLat + randomOffsetLat, baseLng + randomOffsetLng
                ).toFloat()
            } else 0.5f

            vendors.add(
                Vendor(
                    name = data.name,
                    description = data.description,
                    lat = baseLat + randomOffsetLat,
                    lng = baseLng + randomOffsetLng,
                    locality = "Near You",
                    category = data.category,
                    rating = data.rating,
                    priceRange = data.priceRange,
                    isOpen = data.isOpen,
                    distance = distanceKm,
                    phoneNumber = "+91 98765 ${phoneCounter++}",
                    address = "Nearby location",
                    imageUrl = "https://images.unsplash.com/photo-${1517248135467 + index}?ixlib=rb-1.2.1&auto=format&fit=crop&w=800&q=60",
                    photoUrls = listOf(
                        "https://images.unsplash.com/photo-${1517248135467 + index}?ixlib=rb-1.2.1&auto=format&fit=crop&w=800&q=60"
                    ),
                    specialties = data.specialties
                )
            )
        }
    }

    private data class VendorData(
        val name: String,
        val description: String,
        val category: String,
        val rating: Float,
        val priceRange: String,
        val isOpen: Boolean,
        val specialties: List<String>
    )

    private fun stopLocationUpdates() {
        if (isRequestingLocationUpdates && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback!!)
            isRequestingLocationUpdates = false
        }
    }

    private fun applyFilters(filter: String) {
        // Implementation of filter logic
        Log.d("MainActivity", "Applying filter: $filter")
        // Filter implementation code would go here
    }

    private fun setupCustomerUI() {
        setContentView(R.layout.activity_main)

        // Initialize DrawerLayout and NavigationView
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        btnHamburger = findViewById(R.id.btnHamburger)
        btnStats = findViewById(R.id.btnStats)
        btnToggleVendors = findViewById(R.id.btnToggleVendors)
        btnMyLocation = findViewById(R.id.btnMyLocation)
        overlayLayout = findViewById(R.id.overlayLayout)

        // --- MODIFIED SECTION START ---

        val fragmentManager = supportFragmentManager

        // Location search functionality removed - only vendor name search remains

        // --- END OF FIRST MODIFIED SECTION ---


        // Open drawer when hamburger clicked
        btnHamburger.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Show stats dialog when stats button clicked
        btnStats.setOnClickListener {
            showStatsDialog()
        }

        // Toggle vendor list visibility
        btnToggleVendors.setOnClickListener {
            toggleVendorList()
        }

        // My Location button
        btnMyLocation.setOnClickListener {
            getCurrentLocation()
        }

        // Handle navigation item clicks
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    // Stay on home (current activity)
                    drawerLayout.closeDrawer(GravityCompat.START)
                }
                R.id.nav_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    startActivity(intent)
                }
                R.id.nav_vendors -> {
                    // Stay on home (already showing vendors)
                    drawerLayout.closeDrawer(GravityCompat.START)
                }
                R.id.nav_favorites -> {
                    Toast.makeText(this, "Favorites feature coming soon!", Toast.LENGTH_SHORT).show()
                }
                R.id.nav_orders -> {
                    Toast.makeText(this, "Orders feature coming soon!", Toast.LENGTH_SHORT).show()
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
                    // Logout functionality
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

        // Initialize vendors - will be generated when location is available
        vendorList = if (userLocation != null) {
            generateVendorsNearLocation(userLocation)
        } else {
            emptyList() // Empty list until location is obtained
        }

        // Setup RecyclerView
        recyclerView = findViewById(R.id.vendorRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = VendorAdapter(vendorList) { vendor ->
            showVendorDetailsDialog(vendor)
        }
        recyclerView.adapter = adapter

        // Setup Search Bar
        searchBar = findViewById(R.id.searchBar)
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterVendors(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Setup Location Search click listener
        val locationSearchArea = findViewById<LinearLayout>(R.id.locationSearchArea)
        locationSearchArea?.setOnClickListener {
            // Launch Google Places autocomplete
            val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)
            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                .build(this)
            startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
        }

        // Add focus animation to search bar
        searchBar.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                searchBar.animate()
                    .scaleX(1.02f)
                    .scaleY(1.02f)
                    .setDuration(200)
                    .start()
            } else {
                searchBar.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start()
            }
        }

        // Setup Filter Chips
        setupFilterChips()

        // --- MODIFIED SECTION START ---

        // Setup Google Map
        // We already defined fragmentManager
        var mapFragment = fragmentManager
            .findFragmentById(R.id.map) as? SupportMapFragment

        // If fragment is null (first launch), create it. Otherwise, use restored one.
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance()
            fragmentManager.beginTransaction()
                .add(R.id.map, mapFragment)
                .commit()
            // We must execute transactions so getMapAsync can find the fragment
            fragmentManager.executePendingTransactions()
        }

        mapFragment?.getMapAsync(this)

        // --- END OF SECOND MODIFIED SECTION ---

        // Check location permission for customers
        checkLocationPermission()

        val buttonChat = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.buttonChat)
        buttonChat.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            startActivity(intent)
        }
    }

    // ... (All your other methods from line 636 to the end remain exactly the same)
    // ... (moveCameraToVendor, setupFilterChips, selectFilterChip, applyFilters, filterVendors, ...)
    // ... (showVendorDetailsDialog, generateVendorKey, loadContributedPhotosForDialog, toggleVendorList, ...)
    // ... (setupDialogSpecialties, checkUserTypeAndRedirect, checkLocationPermission, showLocationPermissionDialog, ...)
    // ... (navigateToVendor, getCurrentLocation, onResume, onPause, onDestroy, showNearbyVendors, ...)
    // ... (onRequestPermissionsResult, onActivityResult, showStatsDialog)
    private fun moveCameraToVendor(vendor: Vendor) {
        val location = LatLng(vendor.lat, vendor.lng)
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 17f))
    }

    // Setup filter chips
    private fun setupFilterChips() {
        chipAll = findViewById(R.id.chipAll)
        chipFood = findViewById(R.id.chipFood)
        chipCafe = findViewById(R.id.chipCafe)
        chipHealth = findViewById(R.id.chipHealth)
        chipOpen = findViewById(R.id.chipOpen)

        val chips = listOf(chipAll, chipFood, chipCafe, chipHealth, chipOpen)

        chips.forEach { chip ->
            chip.setOnClickListener {
                selectFilterChip(chip, chips)
                applyFilters()
            }
        }

        // Set default filter
        selectFilterChip(chipAll, chips)
    }

    private fun selectFilterChip(selectedChip: TextView, allChips: List<TextView>) {
        allChips.forEach { chip ->
            if (chip == selectedChip) {
                chip.setTextColor(getColor(R.color.white))
                chip.setBackgroundResource(R.drawable.filter_chip_selected)
                currentFilter = when (chip) {
                    chipAll -> "All"
                    chipFood -> "Food & Beverage"
                    chipCafe -> "Café"
                    chipHealth -> "Health & Wellness"
                    chipOpen -> "Open Now"
                    else -> "All"
                }
            } else {
                chip.setTextColor(getColor(R.color.text_secondary))
                chip.setBackgroundResource(R.drawable.filter_chip_unselected)
            }
        }
    }

    private fun applyFilters() {
        val query = searchBar.text.toString()
        filterVendors(query)
    }

    // Enhanced search filter function
    private fun filterVendors(
        query: String = ""
    ) {
        var filteredList = vendorList

        // Apply category filter
        if (currentFilter != "All") {
            filteredList = if (currentFilter == "Open Now") {
                filteredList.filter { it.isOpen }
            } else {
                filteredList.filter { it.category.contains(currentFilter, ignoreCase = true) }
            }
        }

        // Apply search query
        if (query.isNotEmpty()) {
            filteredList = filteredList.filter { vendor ->
                vendor.name.contains(query, ignoreCase = true) ||
                        vendor.description.contains(query, ignoreCase = true) ||
                        vendor.locality.contains(query, ignoreCase = true) ||
                        vendor.category.contains(query, ignoreCase = true) ||
                        vendor.specialties.any { specialty ->
                            specialty.contains(query, ignoreCase = true)
                        }
            }
        }

        // Compute distance for display and sort by distance to user location
        userLocation?.let { location ->
            filteredList.forEach { vendor ->
                val vendorLocation = Location("").apply {
                    latitude = vendor.lat
                    longitude = vendor.lng
                }
                val distanceInMeters = location.distanceTo(vendorLocation)
                vendor.distance = distanceInMeters / 1000
            }
            filteredList = filteredList.sortedBy { it.distance }
        }

        adapter.updateList(filteredList)

        // Add smooth transition animation
        recyclerView.animate()
            .alpha(0.7f)
            .setDuration(150)
            .withEndAction {
                recyclerView.animate()
                    .alpha(1f)
                    .setDuration(150)
                    .start()
            }
    }

    private fun showVendorDetailsDialog(vendor: Vendor) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_vendor_details_enhanced, null)

        // Setup dialog views
        val vendorName = dialogView.findViewById(R.id.dialogVendorName) as TextView
        val vendorCategory = dialogView.findViewById(R.id.dialogVendorCategory) as TextView
        val vendorStatus = dialogView.findViewById(R.id.dialogVendorStatus) as TextView
        val vendorRating = dialogView.findViewById(R.id.dialogVendorRating) as TextView
        val vendorDesc = dialogView.findViewById(R.id.dialogVendorDesc) as TextView
        val vendorLocality = dialogView.findViewById(R.id.dialogVendorLocality) as TextView
        val vendorAddress = dialogView.findViewById(R.id.dialogVendorAddress) as TextView
        val vendorDistance = dialogView.findViewById(R.id.dialogVendorDistance) as TextView
        val vendorPhone = dialogView.findViewById(R.id.dialogVendorPhone) as TextView
        val vendorPriceRange = dialogView.findViewById(R.id.dialogVendorPriceRange) as TextView
        val photosRecycler = dialogView.findViewById<RecyclerView>(R.id.dialogPhotosRecyclerView)
        photosRecycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        val photosList = mutableListOf<String>()
        val photoAdapter = PhotoAdapter(photosList) { url ->
            // Simple full screen dialog
            val fullView = layoutInflater.inflate(R.layout.dialog_fullscreen_photo, null)
            val image = fullView.findViewById<android.widget.ImageView>(R.id.fullscreenPhotoView)
            com.bumptech.glide.Glide.with(this).load(url).into(image)
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(fullView)
                .show()
        }
        photosRecycler.adapter = photoAdapter

        // Populate data
        vendorName.text = vendor.name
        vendorCategory.text = vendor.category
        vendorDesc.text = vendor.description
        vendorLocality.text = vendor.locality.ifEmpty { "Unknown Location" }
        vendorAddress.text = vendor.address.ifEmpty { "Address not available" }
        vendorPhone.text = vendor.phoneNumber.ifEmpty { "Phone not available" }
        vendorPriceRange.text = vendor.priceRange

        // Rating
        vendorRating.text = if (vendor.rating > 0) String.format("%.1f", vendor.rating) else "N/A"

        // Distance
        vendorDistance.text = if (vendor.distance > 0) {
            if (vendor.distance < 1) {
                String.format("%.0f m", vendor.distance * 1000)
            } else {
                String.format("%.1f km", vendor.distance)
            }
        } else "Distance unknown"

        // Status
        if (vendor.isOpen) {
            vendorStatus.text = "OPEN"
            vendorStatus.setTextColor(getColor(R.color.success_green))
            vendorStatus.setBackgroundResource(R.drawable.status_open_background)
        } else {
            vendorStatus.text = "CLOSED"
            vendorStatus.setTextColor(getColor(R.color.text_secondary))
            vendorStatus.setBackgroundResource(R.drawable.specialty_chip_background)
        }

        // Specialties
        setupDialogSpecialties(dialogView, vendor.specialties)

        // Load any predefined photos
        if (vendor.photoUrls.isNotEmpty()) {
            photosList.addAll(vendor.photoUrls)
            photoAdapter.notifyDataSetChanged()
        }
        // Load contributed photos from Firestore
        loadContributedPhotosForDialog(vendor) { urls ->
            if (urls.isNotEmpty()) {
                photosList.addAll(urls)
                photoAdapter.notifyDataSetChanged()
            }
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Vendor Details")
            .setView(dialogView)
            .setPositiveButton("Close") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        // Setup action buttons
        val btnCall = dialogView.findViewById(R.id.btnCallVendor) as com.google.android.material.button.MaterialButton
        val btnDirections = dialogView.findViewById(R.id.btnDirections) as com.google.android.material.button.MaterialButton
        val btnAddPhoto = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAddPhoto)

        btnCall.setOnClickListener {
            if (vendor.phoneNumber.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:${vendor.phoneNumber}")
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show()
            }
        }

        btnDirections.setOnClickListener {
            try {
                // Get current location if available
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                        navigateToVendor(vendor, location)
                    }.addOnFailureListener {
                        // If getting location fails, navigate without origin
                        navigateToVendor(vendor, null)
                    }
                } else {
                    // No permission, navigate without origin
                    navigateToVendor(vendor, null)
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
                // Log the error for debugging
                Log.e("VendorConnect", "Navigation error", e)
            }
        }

        btnAddPhoto.setOnClickListener {
            // Open upload screen with minimal extras to avoid Parcelable dependency
            val intent = Intent(this, VendorDetailActivity::class.java)
            intent.putExtra("name", vendor.name)
            intent.putExtra("lat", vendor.lat)
            intent.putExtra("lng", vendor.lng)
            intent.putExtra("address", vendor.address)
            intent.putExtra("phone", vendor.phoneNumber)
            startActivity(intent)
        }

        dialog.show()
    }

    private fun generateVendorKey(vendor: Vendor): String {
        val nameKey = vendor.name.replace("[^a-zA-Z0-9]+".toRegex(), "_").lowercase(java.util.Locale.getDefault())
        val latKey = String.format(java.util.Locale.getDefault(), "%.6f", vendor.lat)
        val lngKey = String.format(java.util.Locale.getDefault(), "%.6f", vendor.lng)
        return "${nameKey}_${latKey}_${lngKey}"
    }

    private fun loadContributedPhotosForDialog(vendor: Vendor, onLoaded: (List<String>) -> Unit) {
        try {
            val key = generateVendorKey(vendor)
            db.collection("vendorPhotos").document(key).get()
                .addOnSuccessListener { doc ->
                    val urls = doc.get("photoUrls") as? List<*>
                    val stringUrls = urls?.mapNotNull { it as? String } ?: emptyList()
                    onLoaded(stringUrls)
                }
                .addOnFailureListener { _ -> onLoaded(emptyList()) }
        } catch (_: Exception) {
            onLoaded(emptyList())
        }
    }

    private fun toggleVendorList() {
        val isVisible = !isVendorListVisible
        if (isVisible) {
            // Show vendor list with animation
            overlayLayout.visibility = View.VISIBLE
            overlayLayout.startAnimation(slideIn)

            // Update button with smooth transition
            btnToggleVendors.animate()
                .rotation(0f)
                .setDuration(300)
                .withEndAction {
                    btnToggleVendors.setImageResource(R.drawable.ic_map)
                    btnToggleVendors.contentDescription = "Hide Vendor List"
                }
        } else {
            // Hide vendor list to show full map with animation
            overlayLayout.startAnimation(slideOut)
            overlayLayout.visibility = View.GONE

            // Update button with smooth transition
            btnToggleVendors.animate()
                .rotation(180f)
                .setDuration(300)
                .withEndAction {
                    btnToggleVendors.setImageResource(R.drawable.ic_list)
                    btnToggleVendors.contentDescription = "Show Vendor List"
                }
        }
        isVendorListVisible = isVisible
    }

    private fun setupDialogSpecialties(dialogView: View, specialties: List<String>) {
        val specialtyViews = listOf(
            dialogView.findViewById(R.id.dialogSpecialty1) as TextView,
            dialogView.findViewById(R.id.dialogSpecialty2) as TextView,
            dialogView.findViewById(R.id.dialogSpecialty3) as TextView
        )

        // Hide all specialty views first
        specialtyViews.forEach { it.visibility = View.GONE }

        // Show specialties up to 3
        specialties.take(3).forEachIndexed { index, specialty ->
            if (index < specialtyViews.size) {
                specialtyViews[index].text = specialty
                specialtyViews[index].visibility = View.VISIBLE
            }
        }
    }

    private fun checkUserTypeAndRedirect() {
        try {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                db.collection("users").document(userId).get()
                    .addOnSuccessListener { document ->
                        try {
                            if (document != null && document.exists()) {
                                val userType = document.getString("userType") ?: "Customer"
                                if (userType == "Vendor") {
                                    // Redirect to VendorMainActivity immediately
                                    val intent = Intent(this@MainActivity, VendorMainActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                    finish()
                                } else {
                                    // Customer - setup customer UI
                                    setupCustomerUI()
                                }
                            } else {
                                // Document doesn't exist, assume Customer
                                setupCustomerUI()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this@MainActivity, "Error checking user type: ${e.message}", Toast.LENGTH_SHORT).show()
                            setupCustomerUI() // Default to customer UI on error
                        }
                    }
                    .addOnFailureListener { e ->
                        // If failed to get user type, assume Customer
                        setupCustomerUI()
                    }
            } else {
                Toast.makeText(this, "No user found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error in user type check: ${e.message}", Toast.LENGTH_LONG).show()
            setupCustomerUI() // Default to customer UI on error
        }
    }

    private fun checkLocationPermission() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                showLocationPermissionDialog()
            } else {
                // Permission already granted, get current location
                getCurrentLocation()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error checking location permission: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLocationPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Location Permission")
            .setMessage("Do you want to see vendors in your locality? We need location access to show nearby vendors.")
            .setPositiveButton("Yes") { _, _ ->
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            }
            .setNegativeButton("No") { _, _ ->
                Toast.makeText(this, "You can still browse vendors manually", Toast.LENGTH_SHORT).show()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * Navigate to vendor location using the most appropriate available method
     * @param vendor The vendor to navigate to
     * @param currentLocation The user's current location (can be null)
     */
    private fun navigateToVendor(vendor: Vendor, currentLocation: Location?) {
        try {
            // First try: Google Maps with origin-destination
            if (currentLocation != null) {
                val originParam = "${currentLocation.latitude},${currentLocation.longitude}"
                val destParam = "${vendor.lat},${vendor.lng}"

                // Note: The daddr/saddr URL is more reliable than the navigation:q=
                val googleMapsIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("http://maps.google.com/maps?saddr=$originParam&daddr=$destParam")
                    setPackage("com.google.android.apps.maps")
                }

                if (googleMapsIntent.resolveActivity(packageManager) != null) {
                    startActivity(googleMapsIntent)
                    return
                }
            }

            // Second try: Google Maps navigation directly to destination (if no start loc)
            val mapsIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("google.navigation:q=${vendor.lat},${vendor.lng}")
                setPackage("com.google.android.apps.maps")
            }

            if (mapsIntent.resolveActivity(packageManager) != null) {
                startActivity(mapsIntent)
                return
            }

            // Third try: Web browser with Google Maps
            val webIntent = Intent(Intent.ACTION_VIEW).apply {
                val originParam = if (currentLocation != null) "&saddr=${currentLocation.latitude},${currentLocation.longitude}" else ""
                data = Uri.parse("http://maps.google.com/maps?daddr=${vendor.lat},${vendor.lng}$originParam&travelmode=driving")
            }
            startActivity(webIntent)

        } catch (e: Exception) {
            // Final fallback: Generic geo intent
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("geo:${vendor.lat},${vendor.lng}?q=${vendor.lat},${vendor.lng}(${vendor.name})")
                }
                if (fallbackIntent.resolveActivity(packageManager) != null) {
                    startActivity(fallbackIntent)
                } else {
                    Toast.makeText(this, "No maps app available", Toast.LENGTH_SHORT).show()
                }
            } catch (e2: Exception) {
                Toast.makeText(this, "Navigation failed: ${e2.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            if (this::mMap.isInitialized) {
                mMap.isMyLocationEnabled = true
                mMap.uiSettings.isMyLocationButtonEnabled = false // We have our own button
            }

            Toast.makeText(this, "Getting your location...", Toast.LENGTH_SHORT).show()

            // Use LocationRequest for more reliable location
            requestLocationUpdates()

            // Immediate fallback: try getCurrentLocation with timeout
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location: Location? ->
                    location?.let { updateLocation(it) }
                }
                .addOnFailureListener {
                    // Fallback to last known location
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                        lastLoc?.let { updateLocation(it) } ?: run {
                            Toast.makeText(this, "Unable to get location. Please enable GPS and try again.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
        } else {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show()
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onResume() {
        super.onResume()
        // Restart location updates if permission granted
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            this::mMap.isInitialized &&
            mMap.isMyLocationEnabled
        ) {
            if (!isRequestingLocationUpdates) {
                requestLocationUpdates()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }

    private fun showNearbyVendors(latitude: Double, longitude: Double) {
        // For now, just show a toast. Later we'll implement the vendor list screen
        Toast.makeText(this, "Finding vendors near you...", Toast.LENGTH_SHORT).show()
        // TODO: Implement vendor list screen
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                (grantResults.size < 2 || grantResults[1] == PackageManager.PERMISSION_GRANTED)
            ) {
                enableMyLocation()
                getCurrentLocation()
            } else {
                Toast.makeText(this, "Location permission denied. You can still browse vendors manually.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        // Handle Google Places autocomplete result
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                val place = Autocomplete.getPlaceFromIntent(data!!)
                place.latLng?.let { latLng ->
                    // Move camera to selected location
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
                    Toast.makeText(this, "Location: ${place.name}", Toast.LENGTH_SHORT).show()
                }
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                val status = Autocomplete.getStatusFromIntent(data!!)
                Toast.makeText(this, "Error: ${status.statusMessage}", Toast.LENGTH_SHORT).show()
            }
            return
        }
        
        // Handle location settings resolution result
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE + 1) {
            if (resultCode == RESULT_OK) {
                // User enabled location settings, request location again
                requestLocationUpdates()
            } else {
                // User didn't enable location settings, try last known location
                fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                    lastLoc?.let { updateLocation(it) }
                }
            }
        }
    }

    private fun showStatsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_stats, null)

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Stats & Dashboard")
            .setView(dialogView)
            .setPositiveButton("Close") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        // Set up stats data (mock data for now)
        val totalVendorsText = dialogView.findViewById<TextView>(R.id.totalVendorsText)
        val nearbyVendorsText = dialogView.findViewById<TextView>(R.id.nearbyVendorsText)
        val totalOrdersText = dialogView.findViewById<TextView>(R.id.totalOrdersText)
        val averageRatingText = dialogView.findViewById<TextView>(R.id.averageRatingText)

        // Update with actual data
        totalVendorsText.text = "25"
        nearbyVendorsText.text = "8"
        totalOrdersText.text = "156"
        averageRatingText.text = "4.2"

        // Set up quick action buttons
        val btnViewVendors = dialogView.findViewById<Button>(R.id.btnViewVendors)
        val btnViewOrders = dialogView.findViewById<Button>(R.id.btnViewOrders)
        val btnSettings = dialogView.findViewById<Button>(R.id.btnSettings)

        btnViewVendors.setOnClickListener {
            dialog.dismiss()
            // Scroll to vendor list
            recyclerView.smoothScrollToPosition(0)
        }

        btnViewOrders.setOnClickListener {
            dialog.dismiss()
            Toast.makeText(this, "Order history feature coming soon!", Toast.LENGTH_SHORT).show()
        }

        btnSettings.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        dialog.show()
    }
}