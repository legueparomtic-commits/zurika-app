package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserEntity(
    @PrimaryKey val id: Int = 1,
    val fullName: String = "",
    val phoneNumber: String = "",
    val isVerified: Boolean = false,
    val profilePicUri: String? = null,
    val latitude: Double = 6.1375, // Default Lomé, Togo
    val longitude: Double = 1.2125,
    val isGpsAllowed: Boolean = false,
    val isProUser: Boolean = false,
    val isAdmin: Boolean = false
)
