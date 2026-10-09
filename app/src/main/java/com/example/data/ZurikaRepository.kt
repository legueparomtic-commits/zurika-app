package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class ZurikaRepository(private val dao: ZurikaDao) {

    val userProfile: Flow<UserEntity?> = dao.getUserProfile()
    val allServices: Flow<List<ServiceEntity>> = dao.getAllServices()
    val favoriteServices: Flow<List<ServiceEntity>> = dao.getFavoriteServices()
    val allInterventions: Flow<List<InterventionEntity>> = dao.getAllInterventions()
    val recentSearches: Flow<List<SearchHistoryEntity>> = dao.getRecentSearches()

    suspend fun saveUser(user: UserEntity) = dao.saveUserProfile(user)

    suspend fun saveSearchQuery(query: String) {
        val clean = query.trim()
        if (clean.length >= 2) {
            dao.deleteSearchQuery(clean)
            dao.insertSearchQuery(SearchHistoryEntity(query = clean))
        }
    }

    suspend fun deleteSearchQuery(query: String) {
        dao.deleteSearchQuery(query)
    }

    suspend fun clearSearchHistory() {
        dao.clearSearchHistory()
    }

    suspend fun deleteAccount() {
        dao.clearUserProfile()
    }

    suspend fun toggleFavorite(serviceId: String, currentStatus: Boolean) {
        dao.updateFavorite(serviceId, !currentStatus)
    }

    suspend fun updateServiceStatus(serviceId: String, status: ServiceStatus) {
        dao.updateServiceStatus(serviceId, status)
    }

    suspend fun updateServiceRating(serviceId: String, rating: Float, reviewCount: Int) {
        dao.updateServiceRating(serviceId, rating, reviewCount)
    }

    suspend fun addService(service: ServiceEntity) {
        dao.insertService(service)
    }

    suspend fun deleteService(serviceId: String) {
        dao.deleteService(serviceId)
    }

    suspend fun submitIntervention(intervention: InterventionEntity) {
        dao.insertIntervention(intervention)
    }

    suspend fun updateInterventionStatus(id: String, status: RequestStatus) {
        dao.updateInterventionStatus(id, status)
    }

    suspend fun deleteIntervention(id: String) {
        dao.deleteIntervention(id)
    }

    suspend fun clearInterventions() {
        dao.clearInterventions()
    }

    suspend fun addReview(review: ReviewEntity) {
        dao.insertReview(review)
    }

    fun getReviews(serviceId: String): Flow<List<ReviewEntity>> = dao.getReviewsForService(serviceId)

    suspend fun ensureSeedData() {
        val existing = allServices.firstOrNull()
        if (existing.isNullOrEmpty()) {
            val seedServices = listOf(
                ServiceEntity(
                    id = "station_1",
                    name = "TotalEnergies Tokoin",
                    category = ServiceCategory.STATION,
                    status = ServiceStatus.OPEN,
                    latitude = 6.1420,
                    longitude = 1.2180,
                    address = "Boulevard du 13 Janvier, Tokoin, Lomé",
                    phoneNumber = "+228 90 12 34 56",
                    rating = 4.8f,
                    reviewCount = 142,
                    imageResName = "station",
                    servicesOffered = "Super 95, Diesel, Vidange Express, Boutique, Lavage",
                    isFavorite = false,
                    openingHours = "24h/24 - 7j/7",
                    priceInfo = "700 FCFA/L"
                ),
                ServiceEntity(
                    id = "station_2",
                    name = "Shell Bè Beach",
                    category = ServiceCategory.STATION,
                    status = ServiceStatus.OPEN,
                    latitude = 6.1280,
                    longitude = 1.2310,
                    address = "Boulevard de la Marina, Bè, Lomé",
                    phoneNumber = "+228 91 23 45 67",
                    rating = 4.6f,
                    reviewCount = 98,
                    imageResName = "station",
                    servicesOffered = "Super, Gazole, Station Gonflage, Shell Shop",
                    isFavorite = true,
                    openingHours = "06h00 - 22h00",
                    priceInfo = "700 FCFA/L"
                ),
                ServiceEntity(
                    id = "station_3",
                    name = "Cap Esso Adidogomé",
                    category = ServiceCategory.STATION,
                    status = ServiceStatus.BUSY,
                    latitude = 6.1650,
                    longitude = 1.1820,
                    address = "Route de Kpalimé, Adidogomé",
                    phoneNumber = "+228 92 34 56 78",
                    rating = 4.3f,
                    reviewCount = 54,
                    imageResName = "station",
                    servicesOffered = "Carburant, Huiles Moteur, Gonflage",
                    isFavorite = false,
                    openingHours = "06h00 - 21h30",
                    priceInfo = "695 FCFA/L"
                ),
                ServiceEntity(
                    id = "station_4",
                    name = "Sanofi Station Esso Kodjoviakopé",
                    category = ServiceCategory.STATION,
                    status = ServiceStatus.CLOSED,
                    latitude = 6.1190,
                    longitude = 1.2050,
                    address = "Kodjoviakopé près de la Frontière",
                    phoneNumber = "+228 93 45 67 89",
                    rating = 4.1f,
                    reviewCount = 32,
                    imageResName = "station",
                    servicesOffered = "Super, Diesel (Ravitaillement en cours)",
                    isFavorite = false,
                    openingHours = "Recharge en cours",
                    priceInfo = "Fermé temporairement"
                ),
                ServiceEntity(
                    id = "mechanic_1",
                    name = "Garage Auto Excellence Kodjoviakopé",
                    category = ServiceCategory.MECHANIC,
                    status = ServiceStatus.OPEN,
                    latitude = 6.1250,
                    longitude = 1.2090,
                    address = "Rue du Grand Marché, Lomé",
                    phoneNumber = "+228 90 99 88 77",
                    rating = 4.9f,
                    reviewCount = 210,
                    imageResName = "mechanic",
                    servicesOffered = "Diagnostic Électronique, Moteur, Climatisation, Freinage, Suspension",
                    isFavorite = true,
                    openingHours = "07h30 - 19h00",
                    priceInfo = "Devis gratuit"
                ),
                ServiceEntity(
                    id = "mechanic_2",
                    name = "Atelier Mécanique Général Hédzranawoé",
                    category = ServiceCategory.MECHANIC,
                    status = ServiceStatus.BUSY,
                    latitude = 6.1580,
                    longitude = 1.2380,
                    address = "Marché Hédzranawoé, Lomé",
                    phoneNumber = "+228 91 88 77 66",
                    rating = 4.5f,
                    reviewCount = 87,
                    imageResName = "mechanic",
                    servicesOffered = "Mécanique Lourde, Vidange, Parallélisme",
                    isFavorite = false,
                    openingHours = "08h00 - 18h00",
                    priceInfo = "Variable"
                ),
                ServiceEntity(
                    id = "mechanic_3",
                    name = "Auto Express Nyékonakpoè",
                    category = ServiceCategory.MECHANIC,
                    status = ServiceStatus.CLOSED,
                    latitude = 6.1320,
                    longitude = 1.2010,
                    address = "Avenue de la Libération, Nyékonakpoè",
                    phoneNumber = "+228 92 77 66 55",
                    rating = 4.2f,
                    reviewCount = 41,
                    imageResName = "mechanic",
                    servicesOffered = "Injections, Boîte de vitesse, Diagnostic OBD",
                    isFavorite = false,
                    openingHours = "Fermé - Ouvre à 08h00",
                    priceInfo = "Standard"
                ),
                ServiceEntity(
                    id = "vulcanizer_1",
                    name = "Sanofi Vulcanisation & Pneumatique 24h",
                    category = ServiceCategory.VULCANIZER,
                    status = ServiceStatus.OPEN,
                    latitude = 6.1400,
                    longitude = 1.2220,
                    address = "Carrefour Ramos, Tokoin",
                    phoneNumber = "+228 90 55 44 33",
                    rating = 4.8f,
                    reviewCount = 175,
                    imageResName = "vulcanizer",
                    servicesOffered = "Chambre à air, Rustine, Équilibrage, Pneus Neufs/Occasions, Gonflage Azote",
                    isFavorite = false,
                    openingHours = "24h/24",
                    priceInfo = "Pneu : dès 1 000 FCFA"
                ),
                ServiceEntity(
                    id = "vulcanizer_2",
                    name = "Vulca Pro Agbalépédogan",
                    category = ServiceCategory.VULCANIZER,
                    status = ServiceStatus.OPEN,
                    latitude = 6.1720,
                    longitude = 1.2150,
                    address = "Boulevard de la Kara, Agbalépédogan",
                    phoneNumber = "+228 91 44 33 22",
                    rating = 4.7f,
                    reviewCount = 92,
                    imageResName = "vulcanizer",
                    servicesOffered = "Réparation rapide crevaison, Pression pneu, Vente jantes",
                    isFavorite = false,
                    openingHours = "06h30 - 22h00",
                    priceInfo = "Rustine : 500 FCFA"
                ),
                ServiceEntity(
                    id = "vulcanizer_3",
                    name = "Vulcanisateur Express Bè-Kpota",
                    category = ServiceCategory.VULCANIZER,
                    status = ServiceStatus.BUSY,
                    latitude = 6.1390,
                    longitude = 1.2480,
                    address = "Carrefour Bè-Kpota, Lomé",
                    phoneNumber = "+228 92 33 22 11",
                    rating = 4.4f,
                    reviewCount = 63,
                    imageResName = "vulcanizer",
                    servicesOffered = "Dépannage bord de route, Changement de roue",
                    isFavorite = false,
                    openingHours = "07h00 - 20h00",
                    priceInfo = "Économique"
                ),
                ServiceEntity(
                    id = "towing_1",
                    name = "Togo Dépannage Auto 24/7",
                    category = ServiceCategory.TOWING,
                    status = ServiceStatus.OPEN,
                    latitude = 6.1480,
                    longitude = 1.2110,
                    address = "Couverture tout Lomé & Interurbain",
                    phoneNumber = "+228 90 00 11 22",
                    rating = 5.0f,
                    reviewCount = 310,
                    imageResName = "towing",
                    servicesOffered = "Remorquage Plateau, Boost Batterie, Panne Sèche, Dépannage Urgent GPS",
                    isFavorite = true,
                    openingHours = "24h/24 - Intervention < 20 min",
                    priceInfo = "Dépannage : dès 10 000 FCFA"
                ),
                ServiceEntity(
                    id = "towing_2",
                    name = "SOS Remorquage Grand Lomé",
                    category = ServiceCategory.TOWING,
                    status = ServiceStatus.OPEN,
                    latitude = 6.1610,
                    longitude = 1.2290,
                    address = "Zone Portuaire & Autoroute Aritmetique",
                    phoneNumber = "+228 91 11 22 33",
                    rating = 4.8f,
                    reviewCount = 124,
                    imageResName = "towing",
                    servicesOffered = "Camion Grue, Transport Véhicule Panne, Assistance Panne d'Essence",
                    isFavorite = false,
                    openingHours = "24h/24",
                    priceInfo = "Remorquage : dès 12 000 FCFA"
                )
            )
            dao.insertServices(seedServices)
        }

        val existingInterventions = allInterventions.firstOrNull()
        if (existingInterventions.isNullOrEmpty()) {
            val seedInterventions = listOf(
                InterventionEntity(
                    id = "seed_interv_1",
                    userId = "+228 90 00 11 22",
                    userName = "Jean Koffi",
                    userPhone = "+228 90 00 11 22",
                    serviceId = "towing_1",
                    serviceName = "Togo Dépannage Auto 24/7",
                    problemType = ProblemType.PANNE_SECHE,
                    description = "Panne d'essence urgente près du Boulevard du 13 Janvier, Tokoin.",
                    latitude = 6.1420,
                    longitude = 1.2180,
                    status = RequestStatus.COMPLETED,
                    timestamp = System.currentTimeMillis() - 86400000L * 2
                ),
                InterventionEntity(
                    id = "seed_interv_2",
                    userId = "+228 90 00 11 22",
                    userName = "Jean Koffi",
                    userPhone = "+228 90 00 11 22",
                    serviceId = "mechanic_1",
                    serviceName = "Garage Auto Excellence Kodjoviakopé",
                    problemType = ProblemType.BATTERIE,
                    description = "Problème de démarrage, batterie totalement déchargée.",
                    latitude = 6.1250,
                    longitude = 1.2090,
                    status = RequestStatus.ACCEPTED,
                    timestamp = System.currentTimeMillis() - 3600000L * 4
                )
            )
            seedInterventions.forEach { dao.insertIntervention(it) }
        }

        val existingSearches = recentSearches.firstOrNull()
        if (existingSearches.isNullOrEmpty()) {
            val seedSearches = listOf(
                "Vidange express Tokoin",
                "Vulcanisateur 24h/24",
                "Remorquage plateau Bè"
            )
            seedSearches.forEach { dao.insertSearchQuery(SearchHistoryEntity(query = it)) }
        }
    }

    companion object {
        fun calculateDistanceKm(
            lat1: Double, lon1: Double,
            lat2: Double, lon2: Double
        ): Double {
            val r = 6371.0 // Radius of the earth in km
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                    sin(dLon / 2) * sin(dLon / 2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return r * c
        }
    }
}
