package com.example.vendorconnect

data class Vendor(
    val id: String = "",
    val name: String,
    val description: String,
    val lat: Double,
    val lng: Double,
    val locality: String = "",
    val address: String = "",
    val category: String = "General",
    val categories: List<String> = listOf("General"),
    val rating: Float = 0.0f,
    val reviewCount: Int = 0,
    val priceRange: String = "$$",
    val isOpen: Boolean = true,
    val openingHours: String = "9 AM - 9 PM",
    var distance: Float = 0.0f,
    val phoneNumber: String = "",
    val imageUrl: String = "",
    val photoUrls: List<String> = emptyList(),
    val specialties: List<String> = emptyList()
)
