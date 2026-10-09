package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey val id: String,
    val serviceId: String,
    val authorName: String,
    val rating: Float,
    val comment: String,
    val date: String
)
