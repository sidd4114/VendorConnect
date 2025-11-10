package com.example.vendorconnect

data class Vendor(
    val name: String,
    val description: String,
    val lat: Double,
    val lng: Double,
    val locality: String = "",
    val category: String = "General",
    val rating: Float = 0.0f,
    val priceRange: String = "$$",
    val isOpen: Boolean = true,
    var distance: Float = 0.0f,
    val phoneNumber: String = "",
    val address: String = "",
    val imageUrl: String = "",
    val photoUrls: List<String> = emptyList(),
    val specialties: List<String> = emptyList()
)
