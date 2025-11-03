package com.example.vendorconnect

import android.location.Location
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class SearchFunctionalityTest {

    private lateinit var testVendors: List<Vendor>
    
    @Before
    fun setup() {
        // Create test vendors with different properties
        testVendors = listOf(
            Vendor(
                id = "1",
                name = "Pizza Place",
                description = "Best pizza in town",
                lat = 19.0760,
                lng = 72.8777,
                locality = "Andheri",
                category = "Restaurant",
                rating = 4.5f,
                priceRange = "$$",
                isOpen = true,
                distance = 0.0f,
                phoneNumber = "1234567890",
                address = "123 Main St, Andheri",
                imageUrl = "",
                specialties = listOf("Pizza", "Pasta")
            ),
            Vendor(
                id = "2",
                name = "Fashion Store",
                description = "Latest fashion trends",
                lat = 19.1136,
                lng = 72.8697,
                locality = "Juhu",
                category = "Retail",
                rating = 4.0f,
                priceRange = "$$$",
                isOpen = false,
                distance = 0.0,
                phoneNumber = "9876543210",
                address = "456 Fashion St, Juhu",
                imageUrl = "",
                specialties = listOf("Clothing", "Accessories")
            ),
            Vendor(
                id = "3",
                name = "Tech Repair",
                description = "Fix all your gadgets",
                lat = 19.0330,
                lng = 73.0297,
                locality = "Vashi",
                category = "Service",
                rating = 3.5f,
                priceRange = "$$",
                isOpen = true,
                distance = 0.0,
                phoneNumber = "5555555555",
                address = "789 Tech St, Vashi",
                imageUrl = "",
                specialties = listOf("Phone Repair", "Computer Repair")
            )
        )
    }

    @Test
    fun testFilterByName() {
        // Filter by name
        val filteredByName = testVendors.filter { vendor ->
            vendor.name.contains("Pizza", ignoreCase = true)
        }
        
        assertEquals(1, filteredByName.size)
        assertEquals("Pizza Place", filteredByName[0].name)
    }

    @Test
    fun testFilterByLocality() {
        // Filter by locality
        val filteredByLocality = testVendors.filter { vendor ->
            vendor.locality.contains("Juhu", ignoreCase = true)
        }
        
        assertEquals(1, filteredByLocality.size)
        assertEquals("Fashion Store", filteredByLocality[0].name)
    }

    @Test
    fun testFilterByCategory() {
        // Filter by category
        val filteredByCategory = testVendors.filter { vendor ->
            vendor.category == "Service"
        }
        
        assertEquals(1, filteredByCategory.size)
        assertEquals("Tech Repair", filteredByCategory[0].name)
    }

    @Test
    fun testFilterByOpenStatus() {
        // Filter by open status
        val filteredByOpenStatus = testVendors.filter { vendor ->
            vendor.isOpen
        }
        
        assertEquals(2, filteredByOpenStatus.size)
        assertEquals("Pizza Place", filteredByOpenStatus[0].name)
        assertEquals("Tech Repair", filteredByOpenStatus[1].name)
    }

    @Test
    fun testFilterByRadius() {
        // Create a mock location for the user
        val userLocation = mock(Location::class.java)
        userLocation.latitude = 19.0760 // Same as Pizza Place
        userLocation.longitude = 72.8777
        
        // Create a test function to calculate distance
        fun calculateDistance(vendor: Vendor, userLat: Double, userLng: Double): Double {
            val R = 6371 // Earth radius in kilometers
            val dLat = Math.toRadians(vendor.lat - userLat)
            val dLon = Math.toRadians(vendor.lng - userLng)
            val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(Math.toRadians(userLat)) * Math.cos(Math.toRadians(vendor.lat)) *
                    Math.sin(dLon / 2) * Math.sin(dLon / 2)
            val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
            return R * c
        }
        
        // Update distances
        val vendorsWithDistance = testVendors.map { vendor ->
            vendor.distance = calculateDistance(vendor, userLocation.latitude, userLocation.longitude)
            vendor
        }
        
        // Filter by radius (5km)
        val filteredByRadius = vendorsWithDistance.filter { vendor ->
            vendor.distance <= 5.0
        }
        
        // Pizza Place should be within 5km (it's at the same location)
        assertEquals(1, filteredByRadius.size)
        assertEquals("Pizza Place", filteredByRadius[0].name)
    }
}