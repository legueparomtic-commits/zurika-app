package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.AuthState
import com.example.ui.ZurikaViewModel
import com.example.ui.animation.LocalAnimatedVisibilityScope
import com.example.ui.animation.LocalSharedTransitionScope
import com.example.ui.screens.*
import com.example.ui.theme.ZurikaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        try {
            com.google.android.gms.maps.MapsInitializer.initialize(applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        FirebaseManager.init(applicationContext)

        setContent {
            ZurikaTheme {
                ZurikaApp()
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ZurikaApp(viewModel: ZurikaViewModel = viewModel()) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val mapState by viewModel.mapState.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val services by viewModel.allServices.collectAsStateWithLifecycle()
    val favoriteServices by viewModel.favoriteServices.collectAsStateWithLifecycle()
    val interventions by viewModel.allInterventions.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()
    val currentLanguage by viewModel.currentLanguage.collectAsStateWithLifecycle()

    val nameInput by viewModel.nameInput.collectAsStateWithLifecycle()
    val phoneInput by viewModel.phoneInput.collectAsStateWithLifecycle()
    val timerSeconds by viewModel.otpTimerSeconds.collectAsStateWithLifecycle()
    val generatedCode by viewModel.generatedOtpCode.collectAsStateWithLifecycle()
    val otpErrorMsg by viewModel.otpErrorMsg.collectAsStateWithLifecycle()
    val isRegistered by viewModel.isPhoneNumberRegistered.collectAsStateWithLifecycle()

    val activeIntervention by viewModel.activeIntervention.collectAsStateWithLifecycle()
    val isProMode by viewModel.isProMode.collectAsStateWithLifecycle()
    val isAdminMode by viewModel.isAdminMode.collectAsStateWithLifecycle()

    // Overlay Navigation states
    var activeDetailServiceId by remember { mutableStateOf<String?>(null) }
    var showInterventionDialog by remember { mutableStateOf(false) }
    var targetServiceForIntervention by remember { mutableStateOf<String?>(null) }
    var showProfileScreen by remember { mutableStateOf(false) }
    var showProScreen by remember { mutableStateOf(false) }
    var showAdminScreen by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Crossfade(targetState = authState, label = "auth_crossfade") { state ->
            when (state) {
                is AuthState.Splash -> {
                    SplashScreen(
                        onAnimationFinished = { viewModel.finishSplash() }
                    )
                }
                is AuthState.Onboarding -> {
                    OnboardingCarouselScreen(
                        currentLanguage = currentLanguage,
                        onLanguageToggle = { lang -> viewModel.setLanguage(lang) },
                        onFinishOnboarding = { cat -> viewModel.finishOnboarding(cat) }
                    )
                }
                is AuthState.Welcome -> {
                    WelcomeScreen(
                        nameVal = nameInput,
                        phoneVal = phoneInput,
                        onContinue = { name, phone ->
                            viewModel.startAuthProcess(name, phone)
                        }
                    )
                }
                is AuthState.OtpVerification -> {
                    OtpVerificationScreen(
                        phoneNumber = phoneInput,
                        timerSeconds = timerSeconds,
                        generatedCode = generatedCode,
                        errorMsg = otpErrorMsg,
                        isAlreadyRegistered = isRegistered,
                        onVerifyCode = { code -> viewModel.verifyOtp(code) },
                        onResendCode = { viewModel.resendOtpCode() },
                        onBack = { viewModel.logout() }
                    )
                }
                is AuthState.InitialCategorySelect -> {
                    InitialCategoryScreen(
                        userName = userProfile?.fullName ?: nameInput,
                        onCategorySelected = { category ->
                            viewModel.selectInitialCategory(category)
                        }
                    )
                }
                is AuthState.RequestLocationPermission -> {
                    LocationPermissionScreen(
                        onGrantPermission = { allowed, isFine ->
                            viewModel.grantLocationPermission(allowed, isFine)
                        }
                    )
                }
                is AuthState.Authenticated -> {
                    if (showProfileScreen) {
                        ProfileSettingsScreen(
                            user = userProfile,
                            favoriteServices = favoriteServices,
                            interventions = interventions,
                            recentSearches = recentSearches,
                            currentLanguage = currentLanguage,
                            onLanguageSelected = { lang -> viewModel.setLanguage(lang) },
                            onOpenServiceDetail = { service ->
                                activeDetailServiceId = service.id
                                showProfileScreen = false
                            },
                            onSelectSearchQuery = { query ->
                                viewModel.commitSearchQuery(query)
                                showProfileScreen = false
                            },
                            onDeleteSearchQuery = { query -> viewModel.deleteSearchQuery(query) },
                            onClearSearchHistory = { viewModel.clearSearchHistory() },
                            onDeleteIntervention = { id -> viewModel.deleteIntervention(id) },
                            onClearInterventions = { viewModel.clearInterventions() },
                            onLogout = {
                                showProfileScreen = false
                                viewModel.logout()
                            },
                            onDeleteAccount = {
                                showProfileScreen = false
                                viewModel.deleteAccount()
                            },
                            onBack = { showProfileScreen = false }
                        )
                    } else if (showProScreen) {
                        ProSpaceScreen(
                            services = services,
                            interventions = interventions,
                            onUpdateStatus = { serviceId, newStatus ->
                                viewModel.updateServiceStatusByPro(serviceId, newStatus)
                            },
                            onBack = { showProScreen = false }
                        )
                    } else if (showAdminScreen) {
                        AdminDashboardScreen(
                            services = services,
                            onAddNewService = { name, cat, status, address, phone, lat, lng, tags ->
                                viewModel.addNewServiceByAdmin(name, cat, status, address, phone, lat, lng, tags)
                            },
                            onDeleteService = { serviceId ->
                                viewModel.deleteServiceByAdmin(serviceId)
                            },
                            onBack = { showAdminScreen = false }
                        )
                    } else {
                        // Shared Transition Layout between Map / List Views and Service Detail Bottom Sheet
                        SharedTransitionLayout {
                            AnimatedContent(
                                targetState = activeDetailServiceId,
                                transitionSpec = {
                                    if (targetState != null) {
                                        // Fluid upward glide + fade into Detail Sheet
                                        (slideInVertically(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioLowBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            ),
                                            initialOffsetY = { it / 2 }
                                        ) + fadeIn(animationSpec = tween(280))).togetherWith(
                                            fadeOut(animationSpec = tween(200))
                                        )
                                    } else {
                                        // Fluid downward return into Map
                                        fadeIn(animationSpec = tween(250)).togetherWith(
                                            slideOutVertically(
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                                    stiffness = Spring.StiffnessMedium
                                                ),
                                                targetOffsetY = { it / 2 }
                                            ) + fadeOut(animationSpec = tween(220))
                                        )
                                    }
                                },
                                label = "service_detail_shared_transition"
                            ) { currentDetailId ->
                                CompositionLocalProvider(
                                    LocalSharedTransitionScope provides this@SharedTransitionLayout,
                                    LocalAnimatedVisibilityScope provides this@AnimatedContent
                                ) {
                                    if (currentDetailId == null) {
                                        MainMapScreen(
                                            userName = userProfile?.fullName ?: nameInput,
                                            services = services,
                                            mapState = mapState,
                                            isProMode = isProMode,
                                            isAdminMode = isAdminMode,
                                            recentSearches = recentSearches,
                                            onSearchQueryChange = { query -> viewModel.updateSearchQuery(query) },
                                            onCommitSearchQuery = { query -> viewModel.commitSearchQuery(query) },
                                            onSelectCategoryFilter = { cat -> viewModel.selectCategoryFilter(cat) },
                                            onToggleCategoryVisibility = { cat -> viewModel.toggleCategoryVisibility(cat) },
                                            onSetAllCategoriesVisible = { visible -> viewModel.setAllCategoriesVisible(visible) },
                                            onFindNearest = { viewModel.findNearestService() },
                                            onSelectService = { id -> viewModel.selectService(id) },
                                            onToggleListView = { viewModel.toggleListView() },
                                            onOpenServiceDetail = { service ->
                                                activeDetailServiceId = service.id
                                            },
                                            onOpenProfile = { showProfileScreen = true },
                                            onOpenInterventionDispatch = {
                                                targetServiceForIntervention = null
                                                showInterventionDialog = true
                                            },
                                            onOpenProSpace = { showProScreen = true },
                                            onOpenAdminDashboard = { showAdminScreen = true },
                                            onRecenterOnLocation = { lat, lng, label, zoom ->
                                                viewModel.recenterMapOnLocation(lat, lng, label, zoom)
                                            },
                                            onResetToUserLocation = {
                                                viewModel.resetMapToUserGps()
                                            },
                                            onRefreshDepanneurs = {
                                                viewModel.refreshDepanneurs()
                                            }
                                        )
                                    } else {
                                        val detailService = services.find { it.id == currentDetailId }
                                        if (detailService != null) {
                                            val serviceReviews by viewModel.getReviews(detailService.id)
                                                .collectAsStateWithLifecycle(initialValue = emptyList())

                                            ServiceDetailSheet(
                                                service = detailService,
                                                reviews = serviceReviews,
                                                userLat = mapState.userLat,
                                                userLng = mapState.userLng,
                                                onToggleFavorite = { isFav ->
                                                    viewModel.toggleFavorite(detailService.id, isFav)
                                                },
                                                onRequestIntervention = {
                                                    targetServiceForIntervention = detailService.name
                                                    activeDetailServiceId = null
                                                    showInterventionDialog = true
                                                },
                                                onAddReview = { rating, comment ->
                                                    viewModel.addReview(
                                                        ReviewEntity(
                                                            id = "rev_" + System.currentTimeMillis(),
                                                            serviceId = detailService.id,
                                                            authorName = userProfile?.fullName ?: "Conducteur ZURIKA",
                                                            rating = rating,
                                                            comment = comment,
                                                            date = "Aujourd'hui"
                                                        )
                                                    )
                                                },
                                                onSubmitReport = { errorType, description, onComplete ->
                                                    viewModel.submitServiceReport(
                                                        serviceId = detailService.id,
                                                        serviceName = detailService.name,
                                                        errorType = errorType,
                                                        description = description,
                                                        onComplete = onComplete
                                                    )
                                                },
                                                onDismiss = { activeDetailServiceId = null }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Overlay Intervention Dispatch Modal
        if (showInterventionDialog || activeIntervention != null) {
            InterventionDispatchDialog(
                targetServiceName = targetServiceForIntervention,
                userLat = mapState.userLat,
                userLng = mapState.userLng,
                activeIntervention = activeIntervention,
                onSubmitRequest = { type, desc ->
                    viewModel.submitInterventionRequest(
                        serviceId = activeDetailServiceId ?: services.firstOrNull { it.category == ServiceCategory.MECHANIC }?.id ?: "station_1",
                        serviceName = targetServiceForIntervention ?: "Service Dépannage ZURIKA"
                    )
                },
                onPostInterventionReview = { interventionId, serviceId, serviceName, rating, comment, tags, onDone ->
                    viewModel.submitPostInterventionReview(
                        interventionId = interventionId,
                        serviceId = serviceId,
                        serviceName = serviceName,
                        rating = rating,
                        comment = comment,
                        tags = tags,
                        onComplete = onDone
                    )
                },
                onCancelActive = {
                    viewModel.activeIntervention.value = null
                    showInterventionDialog = false
                },
                onDismiss = {
                    showInterventionDialog = false
                    viewModel.activeIntervention.value = null
                }
            )
        }
    }
}
