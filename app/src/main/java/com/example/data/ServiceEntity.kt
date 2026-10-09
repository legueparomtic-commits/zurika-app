package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ServiceCategory {
    STATION,
    MECHANIC,
    VULCANIZER,
    TOWING
}

enum class ServiceStatus {
    OPEN,            // Open / Available (Green)
    BUSY,            // Occupied (Orange)
    CLOSED,          // Closed (Red)
    TEMP_UNAVAILABLE,// Temporarily unavailable (Orange)
    UNKNOWN          // Unknown (Gray)
}

@Entity(tableName = "services")
data class ServiceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: ServiceCategory,
    val status: ServiceStatus,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val phoneNumber: String,
    val rating: Float,
    val reviewCount: Int,
    val imageResName: String,
    val servicesOffered: String, // Comma separated tags
    val isFavorite: Boolean = false,
    val openingHours: String = "24h/24",
    val priceInfo: String = "Prix standard",
    val isVerifiedPro: Boolean = true
)
