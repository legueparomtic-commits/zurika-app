package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ZurikaDao {

    // User Profile
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserProfile(user: UserEntity)

    @Query("DELETE FROM user_profile")
    suspend fun clearUserProfile()

    // Services
    @Query("SELECT * FROM services")
    fun getAllServices(): Flow<List<ServiceEntity>>

    @Query("SELECT * FROM services WHERE isFavorite = 1")
    fun getFavoriteServices(): Flow<List<ServiceEntity>>

    @Query("SELECT * FROM services WHERE id = :serviceId LIMIT 1")
    suspend fun getServiceById(serviceId: String): ServiceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServices(services: List<ServiceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertService(service: ServiceEntity)

    @Update
    suspend fun updateService(service: ServiceEntity)

    @Query("DELETE FROM services WHERE id = :serviceId")
    suspend fun deleteService(serviceId: String)

    @Query("UPDATE services SET isFavorite = :isFav WHERE id = :serviceId")
    suspend fun updateFavorite(serviceId: String, isFav: Boolean)

    @Query("UPDATE services SET status = :status WHERE id = :serviceId")
    suspend fun updateServiceStatus(serviceId: String, status: ServiceStatus)

    @Query("UPDATE services SET rating = :rating, reviewCount = :reviewCount WHERE id = :serviceId")
    suspend fun updateServiceRating(serviceId: String, rating: Float, reviewCount: Int)

    // Interventions
    @Query("SELECT * FROM interventions ORDER BY timestamp DESC")
    fun getAllInterventions(): Flow<List<InterventionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntervention(intervention: InterventionEntity)

    @Query("UPDATE interventions SET status = :status WHERE id = :interventionId")
    suspend fun updateInterventionStatus(interventionId: String, status: RequestStatus)

    @Query("DELETE FROM interventions WHERE id = :interventionId")
    suspend fun deleteIntervention(interventionId: String)

    @Query("DELETE FROM interventions")
    suspend fun clearInterventions()

    // Reviews
    @Query("SELECT * FROM reviews WHERE serviceId = :serviceId")
    fun getReviewsForService(serviceId: String): Flow<List<ReviewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: ReviewEntity)

    // Search History
    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 10")
    fun getRecentSearches(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchQuery(search: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE query = :query")
    suspend fun deleteSearchQuery(query: String)

    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()
}
