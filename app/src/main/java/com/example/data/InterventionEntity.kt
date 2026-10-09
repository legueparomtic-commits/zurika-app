package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ProblemType {
    PANNE_SECHE,   // Out of fuel
    CREVAISON,     // Flat tire
    MOTEUR,        // Engine failure
    BATTERIE,      // Dead battery
    REMORQUAGE,    // Towing
    AUTRE          // Other
}

enum class RequestStatus {
    PENDING,
    ACCEPTED,
    EN_ROUTE,
    COMPLETED,
    CANCELLED
}

@Entity(tableName = "interventions")
data class InterventionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val userName: String,
    val userPhone: String,
    val serviceId: String?,
    val serviceName: String?,
    val problemType: ProblemType,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val status: RequestStatus = RequestStatus.PENDING,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUri: String? = null
)
