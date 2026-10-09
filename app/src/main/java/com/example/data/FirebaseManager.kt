package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object FirebaseManager {
    private const val TAG = "FirebaseManager"

    private var isInitialized = false

    val auth: FirebaseAuth?
        get() = try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "FirebaseAuth error: ${e.message}")
            null
        }

    val firestore: FirebaseFirestore?
        get() = try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "FirebaseFirestore error: ${e.message}")
            null
        }

    fun init(context: Context) {
        if (!isInitialized) {
            try {
                FirebaseApp.initializeApp(context)
                isInitialized = true
                Log.d(TAG, "Firebase initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Firebase: ${e.message}")
            }
        }
    }

    // --- FIREBASE AUTHENTICATION ---

    fun getCurrentFirebaseUser(): FirebaseUser? {
        return auth?.currentUser
    }

    suspend fun signUpWithFirebase(
        email: String,
        pass: String,
        fullName: String,
        phone: String
    ): Result<FirebaseUser?> {
        val firebaseAuth = auth ?: return Result.failure(Exception("FirebaseAuth unavailable"))
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, pass).await()
            val user = authResult.user
            if (user != null) {
                // Save user profile to Firestore
                val userMap = hashMapOf(
                    "uid" to user.uid,
                    "email" to email,
                    "fullName" to fullName,
                    "phone" to phone,
                    "createdAt" to System.currentTimeMillis()
                )
                firestore?.collection("users")?.document(user.uid)?.set(userMap)?.await()
            }
            Result.success(user)
        } catch (e: Exception) {
            Log.w(TAG, "Firebase Auth sign-up skipped or remote key unprovisioned: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun signInWithFirebase(email: String, pass: String): Result<FirebaseUser?> {
        val firebaseAuth = auth ?: return Result.failure(Exception("FirebaseAuth unavailable"))
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, pass).await()
            Result.success(authResult.user)
        } catch (e: Exception) {
            Log.w(TAG, "Firebase Auth sign-in skipped or remote key unprovisioned: ${e.message}")
            Result.failure(e)
        }
    }

    fun signOutFirebase() {
        try {
            auth?.signOut()
        } catch (e: Exception) {
            Log.e(TAG, "SignOut error: ${e.message}")
        }
    }

    // --- FIRESTORE DATA STORAGE ---

    suspend fun syncServiceToFirestore(service: ServiceEntity) {
        val db = firestore ?: return
        try {
            val map = hashMapOf(
                "id" to service.id,
                "name" to service.name,
                "category" to service.category.name,
                "status" to service.status.name,
                "latitude" to service.latitude,
                "longitude" to service.longitude,
                "address" to service.address,
                "phoneNumber" to service.phoneNumber,
                "rating" to service.rating,
                "reviewCount" to service.reviewCount,
                "servicesOffered" to service.servicesOffered,
                "openingHours" to service.openingHours,
                "priceInfo" to service.priceInfo,
                "updatedAt" to System.currentTimeMillis()
            )
            db.collection("services").document(service.id).set(map).await()
            Log.d(TAG, "Service synced to Firestore: ${service.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing service to Firestore: ${e.message}")
        }
    }

    suspend fun saveInterventionToFirestore(intervention: InterventionEntity) {
        val db = firestore ?: return
        try {
            val map = hashMapOf(
                "id" to intervention.id,
                "userName" to intervention.userName,
                "userPhone" to intervention.userPhone,
                "problemType" to intervention.problemType.name,
                "description" to intervention.description,
                "latitude" to intervention.latitude,
                "longitude" to intervention.longitude,
                "status" to intervention.status.name,
                "timestamp" to intervention.timestamp
            )
            db.collection("interventions").document(intervention.id).set(map).await()
            Log.d(TAG, "Intervention saved to Firestore: ${intervention.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving intervention: ${e.message}")
        }
    }

    suspend fun saveReviewToFirestore(review: ReviewEntity, proId: String? = null, tags: List<String> = emptyList()) {
        val db = firestore ?: return
        try {
            val map = hashMapOf(
                "id" to review.id,
                "serviceId" to review.serviceId,
                "authorName" to review.authorName,
                "rating" to review.rating,
                "comment" to review.comment,
                "date" to review.date,
                "tags" to tags,
                "createdAt" to System.currentTimeMillis()
            )
            // 1. Store as a sub-document in 'reviews' subcollection linked to the professional/service
            // Path: /services/{serviceId}/reviews/{reviewId}
            db.collection("services")
                .document(review.serviceId)
                .collection("reviews")
                .document(review.id)
                .set(map)
                .await()

            // 2. If a specific pro profile ID exists, also link to /pros/{proId}/reviews/{reviewId}
            if (!proId.isNullOrBlank()) {
                db.collection("pros")
                    .document(proId)
                    .collection("reviews")
                    .document(review.id)
                    .set(map)
                    .await()
            }

            // 3. Top-level reviews collection for indexing
            db.collection("reviews").document(review.id).set(map).await()

            Log.d(TAG, "Review successfully stored in sub-document /services/${review.serviceId}/reviews/${review.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving review to Firestore subdocument: ${e.message}")
        }
    }

    /**
     * Real-time listener for the 'reviews' subcollection under a specific service/pro document.
     */
    fun listenToServiceSubReviews(serviceId: String): Flow<List<ReviewEntity>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = db.collection("services")
            .document(serviceId)
            .collection("reviews")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to subcollection reviews for $serviceId: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        val id = doc.getString("id") ?: doc.id
                        val sId = doc.getString("serviceId") ?: serviceId
                        val author = doc.getString("authorName") ?: "Conducteur ZURIKA"
                        val rating = (doc.getDouble("rating") ?: (doc.get("rating") as? Number)?.toDouble() ?: 5.0).toFloat()
                        val comment = doc.getString("comment") ?: ""
                        val date = doc.getString("date") ?: "Récemment"
                        ReviewEntity(id, sId, author, rating, comment, date)
                    }
                    trySend(list)
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    suspend fun saveFavoriteToFirestore(userId: String, service: ServiceEntity, isFavorite: Boolean) {
        val db = firestore ?: return
        try {
            val sanitizedUserId = if (userId.isBlank()) "default_user" else userId.replace("+", "").replace(" ", "")
            val favRef = db.collection("users").document(sanitizedUserId).collection("favorites").document(service.id)
            if (isFavorite) {
                val map = hashMapOf(
                    "serviceId" to service.id,
                    "name" to service.name,
                    "category" to service.category.name,
                    "address" to service.address,
                    "rating" to service.rating,
                    "phoneNumber" to service.phoneNumber,
                    "syncedToFirestore" to true,
                    "timestamp" to System.currentTimeMillis()
                )
                favRef.set(map).await()
                Log.d(TAG, "Favorite saved to Firestore for user $sanitizedUserId: ${service.name}")
            } else {
                favRef.delete().await()
                Log.d(TAG, "Favorite removed from Firestore for user $sanitizedUserId: ${service.name}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating favorite in Firestore: ${e.message}")
        }
    }

    /**
     * Real-time listener for service availability and status updates from Firestore.
     * Emits a map of serviceId -> ServiceStatus whenever Firestore changes.
     */
    fun listenToServiceStatusUpdates(): Flow<Map<String, ServiceStatus>> = callbackFlow {
        val db = firestore
        if (db == null) {
            close()
            return@callbackFlow
        }

        val listener = db.collection("services").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Firestore service status listener error: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val statusMap = mutableMapOf<String, ServiceStatus>()
                for (doc in snapshot.documents) {
                    val serviceId = doc.getString("id") ?: doc.id
                    val statusStr = doc.getString("status")
                    if (statusStr != null) {
                        try {
                            statusMap[serviceId] = ServiceStatus.valueOf(statusStr)
                        } catch (e: Exception) {
                            Log.w(TAG, "Unknown ServiceStatus '$statusStr' for $serviceId")
                        }
                    }
                }
                trySend(statusMap)
            }
        }

        awaitClose {
            listener.remove()
        }
    }

    suspend fun updateServiceStatusInFirestore(serviceId: String, status: ServiceStatus) {
        val db = firestore ?: return
        try {
            db.collection("services").document(serviceId)
                .update("status", status.name, "updatedAt", System.currentTimeMillis())
                .await()
            Log.d(TAG, "Service status updated in Firestore: $serviceId -> ${status.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating status in Firestore: ${e.message}")
        }
    }

    data class FirestoreRatingData(
        val serviceId: String,
        val rating: Float,
        val reviewCount: Int,
        val isLiveFromFirestore: Boolean = true,
        val timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Real-time listener for a specific service document in Firestore to fetch its average rating and reviewCount.
     */
    fun listenToServiceRating(serviceId: String): Flow<FirestoreRatingData?> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val docRef = db.collection("services").document(serviceId)
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error listening to service rating for $serviceId: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val ratingDouble = snapshot.getDouble("rating")
                    ?: (snapshot.get("rating") as? Number)?.toDouble()
                    ?: snapshot.getString("rating")?.toDoubleOrNull()
                val reviewCountLong = snapshot.getLong("reviewCount")
                    ?: (snapshot.get("reviewCount") as? Number)?.toLong()
                    ?: snapshot.getString("reviewCount")?.toLongOrNull()

                val rating = ratingDouble?.toFloat() ?: 5.0f
                val count = reviewCountLong?.toInt() ?: 0
                trySend(
                    FirestoreRatingData(
                        serviceId = serviceId,
                        rating = rating,
                        reviewCount = count,
                        isLiveFromFirestore = true,
                        timestamp = snapshot.getLong("updatedAt") ?: System.currentTimeMillis()
                    )
                )
            } else {
                trySend(null)
            }
        }

        awaitClose {
            listener.remove()
        }
    }

    /**
     * Update the average rating and review count directly in the Firestore service document.
     */
    suspend fun updateServiceRatingInFirestore(serviceId: String, rating: Float, reviewCount: Int) {
        val db = firestore ?: return
        try {
            db.collection("services").document(serviceId)
                .update(
                    mapOf(
                        "rating" to rating,
                        "reviewCount" to reviewCount,
                        "updatedAt" to System.currentTimeMillis()
                    )
                ).await()
            Log.d(TAG, "Service rating updated in Firestore: $serviceId -> $rating ($reviewCount avis)")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating service rating in Firestore: ${e.message}")
        }
    }

    /**
     * Save an error report/incorrect information notification for a service to Firestore.
     */
    suspend fun saveServiceReportToFirestore(
        serviceId: String,
        serviceName: String,
        errorType: String,
        description: String,
        userPhone: String? = null
    ): Boolean {
        val db = firestore ?: return true // gracefully succeed locally if remote unavailable
        return try {
            val reportId = "report_" + System.currentTimeMillis()
            val reportData = hashMapOf(
                "id" to reportId,
                "serviceId" to serviceId,
                "serviceName" to serviceName,
                "errorType" to errorType,
                "description" to description,
                "userPhone" to (userPhone ?: "anonyme"),
                "status" to "PENDING_REVIEW",
                "timestamp" to System.currentTimeMillis()
            )
            db.collection("service_reports").document(reportId).set(reportData).await()
            Log.d(TAG, "Service error report saved to Firestore: $reportId for service $serviceName")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving service report to Firestore: ${e.message}")
            false
        }
    }
}
