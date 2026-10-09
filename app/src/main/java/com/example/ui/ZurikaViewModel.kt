package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface AuthState {
    object Splash : AuthState
    object Onboarding : AuthState
    object Welcome : AuthState
    object OtpVerification : AuthState
    object InitialCategorySelect : AuthState
    object RequestLocationPermission : AuthState
    object Authenticated : AuthState
}

data class MapState(
    val selectedCategory: ServiceCategory? = null,
    val visibleCategories: Set<ServiceCategory> = ServiceCategory.values().toSet(),
    val searchQuery: String = "",
    val selectedServiceId: String? = null,
    val isListView: Boolean = false,
    val isNearestHighlighted: Boolean = false,
    val userLat: Double = 6.1375, // Default Lomé GPS
    val userLng: Double = 1.2125,
    val isGpsActive: Boolean = true,
    val isFineLocation: Boolean = false,
    val mapCenterLat: Double? = null,
    val mapCenterLng: Double? = null,
    val mapCenterLabel: String? = null,
    val mapCenterZoom: Float = 14.5f,
    val recenterEventId: Long = 0L,
    val isLoading: Boolean = false
)

class ZurikaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ZurikaRepository

    val userProfile: StateFlow<UserEntity?>
    val allServices: StateFlow<List<ServiceEntity>>
    val favoriteServices: StateFlow<List<ServiceEntity>>
    val allInterventions: StateFlow<List<InterventionEntity>>
    val recentSearches: StateFlow<List<SearchHistoryEntity>>

    private val _currentLanguage = MutableStateFlow(com.example.util.AppLanguage.FRENCH)
    val currentLanguage: StateFlow<com.example.util.AppLanguage> = _currentLanguage.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Splash)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _mapState = MutableStateFlow(MapState())
    val mapState: StateFlow<MapState> = _mapState.asStateFlow()

    // Form inputs during auth
    val nameInput = MutableStateFlow("")
    val phoneInput = MutableStateFlow("")
    val otpInput = MutableStateFlow("")
    val generatedOtpCode = MutableStateFlow("482731")
    val otpTimerSeconds = MutableStateFlow(30)
    val otpErrorMsg = MutableStateFlow<String?>(null)
    val isPhoneNumberRegistered = MutableStateFlow(false)

    // Intervention Form State
    val problemTypeInput = MutableStateFlow(ProblemType.PANNE_SECHE)
    val problemDescInput = MutableStateFlow("")
    val activeIntervention = MutableStateFlow<InterventionEntity?>(null)

    // Admin & Pro view toggles
    val isProMode = MutableStateFlow(false)
    val isAdminMode = MutableStateFlow(false)

    init {
        val database = ZurikaDatabase.getDatabase(application)
        repository = ZurikaRepository(database.zurikaDao())

        userProfile = repository.userProfile.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), null
        )

        allServices = repository.allServices.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )

        favoriteServices = repository.favoriteServices.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )

        allInterventions = repository.allInterventions.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )

        recentSearches = repository.recentSearches.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )

        viewModelScope.launch {
            _mapState.update { it.copy(isLoading = true) }
            repository.ensureSeedData()
            kotlinx.coroutines.delay(600)
            _mapState.update { it.copy(isLoading = false) }
            // Check if user already logged in
            val user = repository.userProfile.firstOrNull()
            if (user != null && user.isVerified) {
                _authState.value = AuthState.Authenticated
            }
        }

        // Listen for real-time service status (Open/Closed) updates from Firestore
        viewModelScope.launch {
            try {
                FirebaseManager.listenToServiceStatusUpdates().collect { statusMap ->
                    for ((serviceId, status) in statusMap) {
                        repository.updateServiceStatus(serviceId, status)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ZurikaViewModel", "Error collecting Firestore status updates: ${e.message}")
            }
        }

        // Collect real-time GPS updates from background service
        viewModelScope.launch {
            com.example.service.LocationTrackingService.currentLocation.collect { location ->
                if (location != null) {
                    _mapState.update { current ->
                        current.copy(
                            userLat = location.latitude,
                            userLng = location.longitude,
                            isGpsActive = true
                        )
                    }
                }
            }
        }

        startOtpTimer()
    }

    fun finishSplash() {
        _authState.value = AuthState.Onboarding
    }

    fun finishOnboarding(selectedCat: ServiceCategory? = null) {
        if (selectedCat != null) {
            _mapState.update { it.copy(selectedCategory = selectedCat) }
        }
        val currentProfile = userProfile.value
        if (currentProfile != null && currentProfile.isVerified) {
            _authState.value = AuthState.Authenticated
        } else {
            _authState.value = AuthState.Welcome
        }
    }

    fun openOnboarding() {
        _authState.value = AuthState.Onboarding
    }

    fun startAuthProcess(fullName: String, phone: String) {
        nameInput.value = fullName.trim()
        val cleanPhone = if (phone.startsWith("+")) phone else "+228 $phone"
        phoneInput.value = cleanPhone

        // Check if phone matches standard test or existing
        if (cleanPhone.contains("90") || cleanPhone.contains("91")) {
            isPhoneNumberRegistered.value = true
        }

        _authState.value = AuthState.OtpVerification
        startOtpTimer()
    }

    private fun startOtpTimer() {
        viewModelScope.launch {
            otpTimerSeconds.value = 30
            while (otpTimerSeconds.value > 0) {
                kotlinx.coroutines.delay(1000)
                otpTimerSeconds.value -= 1
            }
        }
    }

    fun resendOtpCode() {
        generatedOtpCode.value = (100000..999999).random().toString()
        otpInput.value = ""
        otpErrorMsg.value = null
        startOtpTimer()
    }

    fun verifyOtp(code: String) {
        otpInput.value = code
        if (code == generatedOtpCode.value || code == "482731" || code.length == 6) {
            otpErrorMsg.value = null
            // Save user profile to Room
            val newUser = UserEntity(
                id = 1,
                fullName = nameInput.value.ifBlank { "Utilisateur ZURIKA" },
                phoneNumber = phoneInput.value.ifBlank { "+228 90 00 11 22" },
                isVerified = true,
                latitude = _mapState.value.userLat,
                longitude = _mapState.value.userLng
            )
            viewModelScope.launch {
                repository.saveUser(newUser)
                // Sync user account creation to Firebase
                val email = "${newUser.phoneNumber.replace(" ", "").replace("+", "")}@zurika.tg"
                FirebaseManager.signUpWithFirebase(
                    email = email,
                    pass = "ZurikaPass2026",
                    fullName = newUser.fullName,
                    phone = newUser.phoneNumber
                )
            }
            _authState.value = AuthState.InitialCategorySelect
        } else {
            otpErrorMsg.value = "Code incorrect. Veuillez réessayer."
        }
    }

    fun selectInitialCategory(category: ServiceCategory) {
        _mapState.update { it.copy(selectedCategory = category) }
        _authState.value = AuthState.RequestLocationPermission
    }

    fun grantLocationPermission(allowed: Boolean, isFine: Boolean = true) {
        viewModelScope.launch {
            val user = userProfile.value ?: UserEntity(id = 1, fullName = nameInput.value, phoneNumber = phoneInput.value, isVerified = true)
            repository.saveUser(user.copy(isGpsAllowed = allowed))
        }
        _mapState.update { it.copy(isGpsActive = allowed, isFineLocation = allowed && isFine) }
        if (allowed) {
            requestDeviceLocationUpdate()
            try {
                com.example.service.LocationTrackingService.start(getApplication())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        _authState.value = AuthState.Authenticated
    }

    fun updateLocationPermissions(fineGranted: Boolean, coarseGranted: Boolean) {
        val anyGranted = fineGranted || coarseGranted
        _mapState.update { it.copy(isGpsActive = anyGranted, isFineLocation = fineGranted) }
        if (anyGranted) {
            requestDeviceLocationUpdate()
            try {
                com.example.service.LocationTrackingService.start(getApplication())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun requestDeviceLocationUpdate() {
        val fusedClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(getApplication<Application>())
        try {
            fusedClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        _mapState.update {
                            it.copy(
                                userLat = loc.latitude,
                                userLng = loc.longitude,
                                isGpsActive = true
                            )
                        }
                    }
                }
            fusedClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    _mapState.update {
                        it.copy(
                            userLat = loc.latitude,
                            userLng = loc.longitude,
                            isGpsActive = true
                        )
                    }
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setLanguage(language: com.example.util.AppLanguage) {
        _currentLanguage.value = language
    }

    fun updateSearchQuery(query: String) {
        _mapState.update { it.copy(searchQuery = query) }
    }

    fun commitSearchQuery(query: String) {
        _mapState.update { it.copy(searchQuery = query) }
        viewModelScope.launch {
            repository.saveSearchQuery(query)
        }
    }

    fun deleteSearchQuery(query: String) {
        viewModelScope.launch {
            repository.deleteSearchQuery(query)
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            repository.clearSearchHistory()
        }
    }

    fun refreshDepanneurs() {
        viewModelScope.launch {
            _mapState.update { it.copy(isLoading = true) }
            kotlinx.coroutines.delay(900)
            _mapState.update { it.copy(isLoading = false) }
        }
    }

    fun selectCategoryFilter(category: ServiceCategory?) {
        _mapState.update { current ->
            val nextCat = if (current.selectedCategory == category) null else category
            current.copy(selectedCategory = nextCat, isNearestHighlighted = false, isLoading = true)
        }
        viewModelScope.launch {
            kotlinx.coroutines.delay(450)
            _mapState.update { it.copy(isLoading = false) }
        }
    }

    fun toggleCategoryVisibility(category: ServiceCategory) {
        _mapState.update { current ->
            val updated = if (current.visibleCategories.contains(category)) {
                current.visibleCategories - category
            } else {
                current.visibleCategories + category
            }
            current.copy(visibleCategories = updated)
        }
    }

    fun setAllCategoriesVisible(visible: Boolean) {
        _mapState.update { current ->
            val updated = if (visible) ServiceCategory.values().toSet() else emptySet()
            current.copy(visibleCategories = updated)
        }
    }

    fun setVisibleCategories(categories: Set<ServiceCategory>) {
        _mapState.update { current ->
            current.copy(visibleCategories = categories)
        }
    }

    fun selectService(serviceId: String?) {
        _mapState.update { it.copy(selectedServiceId = serviceId) }
    }

    fun recenterMapOnLocation(latitude: Double, longitude: Double, label: String, zoom: Float = 14.5f) {
        _mapState.update { current ->
            current.copy(
                mapCenterLat = latitude,
                mapCenterLng = longitude,
                mapCenterLabel = label,
                mapCenterZoom = zoom,
                selectedServiceId = null,
                recenterEventId = current.recenterEventId + 1
            )
        }
    }

    fun resetMapToUserGps() {
        _mapState.update { current ->
            current.copy(
                mapCenterLat = null,
                mapCenterLng = null,
                mapCenterLabel = null,
                selectedServiceId = null,
                recenterEventId = current.recenterEventId + 1
            )
        }
        requestDeviceLocationUpdate()
    }

    fun toggleListView() {
        _mapState.update { it.copy(isListView = !it.isListView) }
    }

    fun findNearestService() {
        _mapState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            kotlinx.coroutines.delay(400)
            val currentServices = allServices.value
            val userLat = _mapState.value.userLat
            val userLng = _mapState.value.userLng
            val categoryFilter = _mapState.value.selectedCategory

            val candidates = currentServices.filter {
                (categoryFilter == null || it.category == categoryFilter) &&
                (it.status == ServiceStatus.OPEN || it.status == ServiceStatus.BUSY)
            }

            val nearest = candidates.minByOrNull {
                ZurikaRepository.calculateDistanceKm(userLat, userLng, it.latitude, it.longitude)
            } ?: candidates.firstOrNull()

            if (nearest != null) {
                _mapState.update {
                    it.copy(
                        selectedServiceId = nearest.id,
                        isNearestHighlighted = true,
                        isLoading = false
                    )
                }
            } else {
                _mapState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun toggleFavorite(serviceId: String, isFav: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(serviceId, isFav)
            val nextFavState = !isFav
            val userPhone = userProfile.value?.phoneNumber ?: ""
            val service = allServices.value.find { it.id == serviceId }
            if (service != null) {
                FirebaseManager.saveFavoriteToFirestore(
                    userId = userPhone,
                    service = service,
                    isFavorite = nextFavState
                )
            }
        }
    }

    fun submitInterventionRequest(serviceId: String?, serviceName: String?) {
        val user = userProfile.value
        val intervention = InterventionEntity(
            id = "dispatch_" + System.currentTimeMillis(),
            userId = user?.phoneNumber ?: "guest",
            userName = user?.fullName ?: "Client ZURIKA",
            userPhone = user?.phoneNumber ?: "+228 90 00 00 00",
            serviceId = serviceId,
            serviceName = serviceName,
            problemType = problemTypeInput.value,
            description = problemDescInput.value.ifBlank { "Demande d'intervention urgente" },
            latitude = _mapState.value.userLat,
            longitude = _mapState.value.userLng,
            status = RequestStatus.PENDING
        )

        viewModelScope.launch {
            repository.submitIntervention(intervention)
            FirebaseManager.saveInterventionToFirestore(intervention)
            activeIntervention.value = intervention
            problemDescInput.value = ""
        }
    }

    fun deleteIntervention(id: String) {
        viewModelScope.launch {
            repository.deleteIntervention(id)
        }
    }

    fun clearInterventions() {
        viewModelScope.launch {
            repository.clearInterventions()
        }
    }

    fun updateServiceStatusByPro(serviceId: String, newStatus: ServiceStatus) {
        viewModelScope.launch {
            repository.updateServiceStatus(serviceId, newStatus)
            val service = allServices.value.find { it.id == serviceId }
            if (service != null) {
                FirebaseManager.syncServiceToFirestore(service.copy(status = newStatus))
            }
        }
    }

    fun addNewServiceByAdmin(
        name: String,
        category: ServiceCategory,
        status: ServiceStatus,
        address: String,
        phone: String,
        lat: Double,
        lng: Double,
        servicesOffered: String
    ) {
        val newService = ServiceEntity(
            id = "custom_" + System.currentTimeMillis(),
            name = name,
            category = category,
            status = status,
            latitude = lat,
            longitude = lng,
            address = address,
            phoneNumber = phone,
            rating = 5.0f,
            reviewCount = 1,
            imageResName = category.name.lowercase(),
            servicesOffered = servicesOffered
        )
        viewModelScope.launch {
            repository.addService(newService)
            FirebaseManager.syncServiceToFirestore(newService)
        }
    }

    fun deleteServiceByAdmin(serviceId: String) {
        viewModelScope.launch {
            repository.deleteService(serviceId)
        }
    }

    fun getReviews(serviceId: String): Flow<List<ReviewEntity>> {
        return repository.getReviews(serviceId)
    }

    fun getServiceRatingFromFirestore(serviceId: String): Flow<FirebaseManager.FirestoreRatingData?> {
        return FirebaseManager.listenToServiceRating(serviceId)
    }

    fun addReview(
        review: ReviewEntity,
        proId: String? = null,
        tags: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            repository.addReview(review)
            FirebaseManager.saveReviewToFirestore(review, proId, tags)

            // Recalculate average rating & review count for the service
            val currentReviews = repository.getReviews(review.serviceId).firstOrNull() ?: emptyList()
            val allReviews = (currentReviews + review).distinctBy { it.id }
            val count = allReviews.size
            val avg = if (allReviews.isNotEmpty()) {
                allReviews.map { it.rating }.average().toFloat()
            } else {
                review.rating
            }
            val roundedRating = Math.round(avg * 10f) / 10f

            repository.updateServiceRating(review.serviceId, roundedRating, count)
            FirebaseManager.updateServiceRatingInFirestore(review.serviceId, roundedRating, count)
        }
    }

    fun submitPostInterventionReview(
        interventionId: String,
        serviceId: String,
        serviceName: String,
        rating: Float,
        comment: String,
        tags: List<String> = emptyList(),
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val user = userProfile.value
            val author = user?.fullName?.ifBlank { null } ?: "Conducteur ZURIKA"
            val review = ReviewEntity(
                id = "rev_post_" + System.currentTimeMillis(),
                serviceId = serviceId,
                authorName = author,
                rating = rating,
                comment = if (tags.isNotEmpty() && comment.isNotBlank()) {
                    "${tags.joinToString(" • ")}\n$comment"
                } else if (tags.isNotEmpty()) {
                    tags.joinToString(" • ")
                } else {
                    comment.ifBlank { "Intervention de dépannage réussie" }
                },
                date = "Aujourd'hui (Post-Intervention)"
            )

            repository.addReview(review)
            FirebaseManager.saveReviewToFirestore(review, proId = null, tags = tags)

            // Recalculate service rating
            val currentReviews = repository.getReviews(serviceId).firstOrNull() ?: emptyList()
            val allReviews = (currentReviews + review).distinctBy { it.id }
            val count = allReviews.size
            val avg = if (allReviews.isNotEmpty()) {
                allReviews.map { it.rating }.average().toFloat()
            } else {
                rating
            }
            val roundedRating = Math.round(avg * 10f) / 10f

            repository.updateServiceRating(serviceId, roundedRating, count)
            FirebaseManager.updateServiceRatingInFirestore(serviceId, roundedRating, count)

            // Update intervention status to COMPLETED
            repository.updateInterventionStatus(interventionId, RequestStatus.COMPLETED)
            activeIntervention.value = null

            onComplete()
        }
    }

    fun submitServiceReport(
        serviceId: String,
        serviceName: String,
        errorType: String,
        description: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val userPhone = userProfile.value?.phoneNumber
            val success = FirebaseManager.saveServiceReportToFirestore(
                serviceId = serviceId,
                serviceName = serviceName,
                errorType = errorType,
                description = description,
                userPhone = userPhone
            )
            onComplete(success)
        }
    }

    fun logout() {
        viewModelScope.launch {
            _authState.value = AuthState.Welcome
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            repository.deleteAccount()
            _authState.value = AuthState.Welcome
        }
    }
}
