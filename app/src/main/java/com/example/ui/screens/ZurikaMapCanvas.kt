package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.MapShimmerLoadingOverlay
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import com.example.data.*
import com.example.ui.animation.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

enum class MapDisplayMode {
    GOOGLE_MAP_NORMAL,
    GOOGLE_MAP_SATELLITE,
    GOOGLE_MAP_TERRAIN,
    LOME_VECTOR
}

/**
 * Calculates estimated travel times and realistic polyline waypoints along Lomé roads.
 */
object RouteCalculator {
    data class RouteEstimate(
        val distanceKm: Double,
        val carTimeMinutes: Int,
        val motoTimeMinutes: Int,
        val walkingTimeMinutes: Int,
        val waypoints: List<LatLng>
    )

    fun calculateRoute(
        startLat: Double,
        startLng: Double,
        destLat: Double,
        destLng: Double
    ): RouteEstimate {
        val directDist = ZurikaRepository.calculateDistanceKm(startLat, startLng, destLat, destLng)
        // City road winding factor ~ 1.25x direct distance
        val roadDistKm = directDist * 1.25

        // Typical urban speeds in Lomé
        // Car average ~ 28 km/h in city traffic
        val carMin = ((roadDistKm / 28.0) * 60).toInt().coerceAtLeast(1)
        // Moto-taxi (Zemidjan) agile in traffic ~ 38 km/h
        val motoMin = ((roadDistKm / 38.0) * 60).toInt().coerceAtLeast(1)
        // Walking ~ 4.5 km/h
        val walkMin = ((roadDistKm / 4.5) * 60).toInt().coerceAtLeast(2)

        // Interpolate structured route waypoints mimicking city grid turns
        val points = mutableListOf<LatLng>()
        points.add(LatLng(startLat, startLng))

        // Mid-point orthogonal deflection to follow Lomé urban axes (Boulevard / Avenue)
        val midLat = (startLat + destLat) / 2.0
        val midLng = (startLng + destLng) / 2.0
        val deltaLat = destLat - startLat
        val deltaLng = destLng - startLng

        // Turn 1: 30% along primary axis
        points.add(LatLng(startLat + deltaLat * 0.35, startLng + deltaLng * 0.05))
        // Turn 2: 70% intersection
        points.add(LatLng(startLat + deltaLat * 0.70, startLng + deltaLng * 0.65))
        // Destination
        points.add(LatLng(destLat, destLng))

        return RouteEstimate(
            distanceKm = roadDistKm,
            carTimeMinutes = carMin,
            motoTimeMinutes = motoMin,
            walkingTimeMinutes = walkMin,
            waypoints = points
        )
    }
}

@Composable
fun ZurikaMapCanvas(
    services: List<ServiceEntity>,
    allServices: List<ServiceEntity> = services,
    selectedServiceId: String?,
    userLat: Double,
    userLng: Double,
    onSelectService: (String?) -> Unit,
    onOpenDetail: (ServiceEntity) -> Unit,
    visibleCategories: Set<ServiceCategory> = ServiceCategory.values().toSet(),
    onToggleCategoryVisibility: (ServiceCategory) -> Unit = {},
    onSetAllCategoriesVisible: (Boolean) -> Unit = {},
    isMyLocationGranted: Boolean = false,
    mapCenterLat: Double? = null,
    mapCenterLng: Double? = null,
    mapCenterLabel: String? = null,
    mapCenterZoom: Float = 14.5f,
    recenterEventId: Long = 0L,
    onRecenterOnLocation: (Double, Double, String, Float) -> Unit = { _, _, _, _ -> },
    onResetToUserLocation: () -> Unit = {},
    isLoading: Boolean = false,
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            com.google.android.gms.maps.MapsInitializer.initialize(context.applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    var mapDisplayMode by remember { mutableStateOf(MapDisplayMode.LOME_VECTOR) }
    var isTrafficEnabled by remember { mutableStateOf(false) }
    var showMapTypeMenu by remember { mutableStateOf(false) }
    var showCategoryFilterMenu by remember { mutableStateOf(false) }
    var showMissingMapsKeyDialog by remember { mutableStateOf(false) }
    var recenterTrigger by remember { mutableIntStateOf(0) }
    var vectorZoomInTrigger by remember { mutableIntStateOf(0) }
    var vectorZoomOutTrigger by remember { mutableIntStateOf(0) }
    var showRecenterBadge by remember { mutableStateOf(false) }

    val displayedServices = remember(services, visibleCategories) {
        services.filter { it.category in visibleCategories }
    }

    val stationCount = remember(allServices) { allServices.count { it.category == ServiceCategory.STATION } }
    val mechanicCount = remember(allServices) { allServices.count { it.category == ServiceCategory.MECHANIC } }
    val vulcanizerCount = remember(allServices) { allServices.count { it.category == ServiceCategory.VULCANIZER } }
    val towingCount = remember(allServices) { allServices.count { it.category == ServiceCategory.TOWING } }

    val isGoogleMapsKeyConfigured = remember {
        try {
            val key = com.example.BuildConfig.MAPS_API_KEY
            key.isNotBlank()
        } catch (e: Exception) {
            true
        }
    }

    val userPosition = remember(userLat, userLng) { LatLng(userLat, userLng) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userPosition, 14.5f)
    }

    val haptic = LocalHapticFeedback.current

    val isCameraAwayFromUser by remember {
        derivedStateOf {
            val target = cameraPositionState.position.target
            val dLat = kotlin.math.abs(target.latitude - userLat)
            val dLng = kotlin.math.abs(target.longitude - userLng)
            (dLat > 0.003 || dLng > 0.003) || mapCenterLat != null
        }
    }

    val selectedService = displayedServices.find { it.id == selectedServiceId } ?: services.find { it.id == selectedServiceId }
    val routeEstimate = remember(userLat, userLng, selectedService?.latitude, selectedService?.longitude) {
        if (selectedService != null) {
            RouteCalculator.calculateRoute(
                startLat = userLat,
                startLng = userLng,
                destLat = selectedService.latitude,
                destLng = selectedService.longitude
            )
        } else null
    }

    // Keep camera positioned on user location when coordinates update if no service is selected and no custom center is active
    LaunchedEffect(userLat, userLng) {
        if (selectedServiceId == null && mapCenterLat == null) {
            safeAnimateCamera(
                cameraPositionState = cameraPositionState,
                target = userPosition,
                zoom = cameraPositionState.position.zoom.coerceAtLeast(14.5f),
                durationMs = 500
            )
        }
    }

    // Smoothly animate camera when a service is selected
    LaunchedEffect(selectedServiceId) {
        if (selectedService != null) {
            safeAnimateCamera(
                cameraPositionState = cameraPositionState,
                target = LatLng(selectedService.latitude, selectedService.longitude),
                zoom = 16f,
                durationMs = 600
            )
        }
    }

    // Smoothly animate camera when a city or specific address is selected to recenter the map
    LaunchedEffect(recenterEventId, mapCenterLat, mapCenterLng) {
        if (mapCenterLat != null && mapCenterLng != null) {
            safeAnimateCamera(
                cameraPositionState = cameraPositionState,
                target = LatLng(mapCenterLat, mapCenterLng),
                zoom = mapCenterZoom,
                durationMs = 700
            )
        } else if (recenterEventId > 0 && mapCenterLat == null) {
            safeAnimateCamera(
                cameraPositionState = cameraPositionState,
                target = userPosition,
                zoom = 15.5f,
                durationMs = 600
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("interactive_map_canvas")
    ) {
        if (mapDisplayMode != MapDisplayMode.LOME_VECTOR) {
            // Google Maps Mode via maps.compose
            val googleMapType = when (mapDisplayMode) {
                MapDisplayMode.GOOGLE_MAP_SATELLITE -> MapType.SATELLITE
                MapDisplayMode.GOOGLE_MAP_TERRAIN -> MapType.TERRAIN
                else -> MapType.NORMAL
            }

            val mapProperties = remember(googleMapType, isTrafficEnabled, isMyLocationGranted) {
                MapProperties(
                    mapType = googleMapType,
                    isTrafficEnabled = isTrafficEnabled,
                    isMyLocationEnabled = isMyLocationGranted
                )
            }

            val mapUiSettings = remember {
                MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false, // Handled via our custom M3 recenter button
                    compassEnabled = true,
                    rotationGesturesEnabled = true,
                    scrollGesturesEnabled = true,
                    tiltGesturesEnabled = true,
                    zoomGesturesEnabled = true,
                    mapToolbarEnabled = false
                )
            }

            GoogleMap(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("google_map_view"),
                cameraPositionState = cameraPositionState,
                properties = mapProperties,
                uiSettings = mapUiSettings,
                onMapClick = {
                    onSelectService(null)
                }
            ) {
                // User Location Accuracy Circle
                Circle(
                    center = userPosition,
                    radius = 220.0,
                    fillColor = ZurikaPrimary.copy(alpha = 0.18f),
                    strokeColor = ZurikaPrimary.copy(alpha = 0.65f),
                    strokeWidth = 3f
                )

                // User Location Custom Pin
                MarkerComposable(
                    keys = arrayOf(userLat, userLng),
                    state = MarkerState(position = userPosition),
                    title = "Votre position",
                    snippet = "Lomé, Togo",
                    onClick = {
                        coroutineScope.launch {
                            safeAnimateCamera(
                                cameraPositionState = cameraPositionState,
                                target = userPosition,
                                zoom = 16f
                            )
                        }
                        true
                    }
                ) {
                    UserLocationPin()
                }

                // Target Searched Location / City Pin
                if (mapCenterLat != null && mapCenterLng != null) {
                    val targetLoc = LatLng(mapCenterLat, mapCenterLng)
                    Circle(
                        center = targetLoc,
                        radius = 320.0,
                        fillColor = ZurikaAmber.copy(alpha = 0.22f),
                        strokeColor = ZurikaAmber,
                        strokeWidth = 3f
                    )
                    MarkerComposable(
                        keys = arrayOf<Any>(mapCenterLat ?: 0.0, mapCenterLng ?: 0.0, mapCenterLabel ?: ""),
                        state = MarkerState(position = targetLoc),
                        title = mapCenterLabel ?: "Lieu recentré",
                        snippet = "Carte recentrée ici",
                        onClick = {
                            coroutineScope.launch {
                                safeAnimateCamera(
                                    cameraPositionState = cameraPositionState,
                                    target = targetLoc,
                                    zoom = mapCenterZoom
                                )
                            }
                            true
                        }
                    ) {
                        SearchedLocationMarkerPill(label = mapCenterLabel ?: "Lieu sélectionné")
                    }
                }

                // Polyline route with realistic waypoints from User to Selected Service
                if (selectedService != null && routeEstimate != null) {
                    val isDep = selectedService.isDepanneurService()
                    val routeColor = if (isDep) ZurikaAmber else ZurikaPrimary

                    // Route background shadow line
                    Polyline(
                        points = routeEstimate.waypoints,
                        color = Color.Black.copy(alpha = 0.45f),
                        width = 16f,
                        geodesic = true
                    )
                    // Active Route Polyline
                    Polyline(
                        points = routeEstimate.waypoints,
                        color = routeColor,
                        width = 10f,
                        geodesic = true
                    )

                    // Floating ETA badge positioned at mid-route waypoint
                    val midPoint = routeEstimate.waypoints.getOrNull(routeEstimate.waypoints.size / 2)
                        ?: LatLng(
                            (userLat + selectedService.latitude) / 2.0,
                            (userLng + selectedService.longitude) / 2.0
                        )

                    MarkerComposable(
                        keys = arrayOf<Any>(selectedService.id, routeEstimate.carTimeMinutes, isDep),
                        state = MarkerState(position = midPoint),
                        title = if (isDep) "Temps trajet dépanneur" else "Estimation itinéraire",
                        snippet = "${routeEstimate.carTimeMinutes} min en dépanneuse/voiture • ${routeEstimate.motoTimeMinutes} min en moto",
                        onClick = { true }
                    ) {
                        RouteEtaMarkerPill(
                            carMinutes = routeEstimate.carTimeMinutes,
                            motoMinutes = routeEstimate.motoTimeMinutes,
                            distanceKm = routeEstimate.distanceKm,
                            isDepanneur = isDep
                        )
                    }
                }

                // Nearby Service Markers
                displayedServices.forEachIndexed { index, service ->
                    val isSelected = service.id == selectedServiceId
                    val servicePos = LatLng(service.latitude, service.longitude)
                    val distanceKm = remember(userLat, userLng, service.latitude, service.longitude) {
                        ZurikaRepository.calculateDistanceKm(userLat, userLng, service.latitude, service.longitude)
                    }

                    MarkerComposable(
                        keys = arrayOf(service.id, isSelected, service.status.name, distanceKm, visibleCategories),
                        state = MarkerState(position = servicePos),
                        title = service.name,
                        snippet = "${service.address} • ${service.status}",
                        onClick = {
                            onSelectService(service.id)
                            coroutineScope.launch {
                                safeAnimateCamera(
                                    cameraPositionState = cameraPositionState,
                                    target = servicePos,
                                    zoom = 16f,
                                    durationMs = 500
                                )
                            }
                            true
                        }
                    ) {
                        GoogleMapServicePin(
                            service = service,
                            isSelected = isSelected,
                            distanceKm = distanceKm,
                            animationDelayMs = (index * 35).coerceAtMost(250),
                            filterTriggerKey = visibleCategories
                        )
                    }
                }
            }
        } else {
            // Stylized Lomé Vector Canvas Mode
            LomeStylizedVectorCanvas(
                services = displayedServices,
                selectedServiceId = selectedServiceId,
                userLat = userLat,
                userLng = userLng,
                onSelectService = onSelectService,
                recenterTrigger = recenterTrigger,
                zoomInTrigger = vectorZoomInTrigger,
                zoomOutTrigger = vectorZoomOutTrigger,
                isTrafficEnabled = isTrafficEnabled,
                routeEstimate = routeEstimate
            )
        }

        // Animated GPS Recenter Confirmation Badge
        LaunchedEffect(recenterTrigger) {
            if (recenterTrigger > 0) {
                showRecenterBadge = true
                kotlinx.coroutines.delay(2000)
                showRecenterBadge = false
            }
        }

        AnimatedVisibility(
            visible = showRecenterBadge,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 64.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ZurikaPrimary,
                contentColor = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.testTag("recenter_feedback_badge")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Position GPS recentrée (Lomé)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Top Floating Control Bar: Service Count, Marker Visibility Filter Menu, and Map Layers
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 12.dp, end = 76.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: Active Count Pill, Marker Visibility Filter Menu Button, and Layers Selector Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Active Marker Count Pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DarkSurface.copy(alpha = 0.94f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (displayedServices.isNotEmpty()) ZurikaStatusOpen else ZurikaStatusClosed,
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${displayedServices.size} marqueurs",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Filter Menu Trigger Button with popup
                Box {
                    Surface(
                        onClick = { showCategoryFilterMenu = !showCategoryFilterMenu },
                        shape = RoundedCornerShape(16.dp),
                        color = if (visibleCategories.size < 4) ZurikaPrimary.copy(alpha = 0.95f) else DarkSurface.copy(alpha = 0.94f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (visibleCategories.size < 4) Color.White else ZurikaPrimary.copy(alpha = 0.6f)
                        ),
                        shadowElevation = 4.dp,
                        modifier = Modifier.testTag("marker_filter_menu_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Menu de filtres de visibilité",
                                tint = if (visibleCategories.size < 4) Color.White else ZurikaPrimaryDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Filtres (${visibleCategories.size}/4)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Dropdown / Popover Menu for toggling marker visibility by service type
                    DropdownMenu(
                        expanded = showCategoryFilterMenu,
                        onDismissRequest = { showCategoryFilterMenu = false },
                        modifier = Modifier
                            .width(300.dp)
                            .background(DarkSurface)
                            .padding(vertical = 4.dp)
                    ) {
                        // Header
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FilterAlt,
                                    contentDescription = null,
                                    tint = ZurikaAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Filtres des marqueurs",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "Basculez la visibilité des services sur la carte",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.65f)
                            )
                        }

                        Divider(color = Color.White.copy(alpha = 0.12f), thickness = 1.dp)

                        // 1. Station-service (Station)
                        MarkerVisibilityFilterMenuItem(
                            title = "Station-service (Station)",
                            subtitle = "Essence, Gasoil, Lubrifiants",
                            icon = Icons.Default.LocalGasStation,
                            tintColor = ZurikaPrimary,
                            count = stationCount,
                            isChecked = visibleCategories.contains(ServiceCategory.STATION),
                            testTag = "filter_toggle_station",
                            onToggle = { onToggleCategoryVisibility(ServiceCategory.STATION) }
                        )

                        // 2. Mécanicien
                        MarkerVisibilityFilterMenuItem(
                            title = "Mécanicien",
                            subtitle = "Garages, Réparation moteur, Auto/Moto",
                            icon = Icons.Default.Build,
                            tintColor = ZurikaAmber,
                            count = mechanicCount,
                            isChecked = visibleCategories.contains(ServiceCategory.MECHANIC),
                            testTag = "filter_toggle_mechanic",
                            onToggle = { onToggleCategoryVisibility(ServiceCategory.MECHANIC) }
                        )

                        // 3. Vulcanisateur
                        MarkerVisibilityFilterMenuItem(
                            title = "Vulcanisateur",
                            subtitle = "Pneus, Crevaisons, Gonflage",
                            icon = Icons.Default.TwoWheeler,
                            tintColor = Color(0xFFA855F7),
                            count = vulcanizerCount,
                            isChecked = visibleCategories.contains(ServiceCategory.VULCANIZER),
                            testTag = "filter_toggle_vulcanizer",
                            onToggle = { onToggleCategoryVisibility(ServiceCategory.VULCANIZER) }
                        )

                        // 4. Dépannage
                        MarkerVisibilityFilterMenuItem(
                            title = "Dépannage",
                            subtitle = "Remorquage, Assistance 24/7",
                            icon = Icons.Default.Warning,
                            tintColor = ZurikaStatusClosed,
                            count = towingCount,
                            isChecked = visibleCategories.contains(ServiceCategory.TOWING),
                            testTag = "filter_toggle_towing",
                            onToggle = { onToggleCategoryVisibility(ServiceCategory.TOWING) }
                        )

                        Divider(color = Color.White.copy(alpha = 0.12f), thickness = 1.dp)

                        // Action Buttons Footer
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { onSetAllCategoriesVisible(true) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("filter_show_all_button")
                            ) {
                                Text("Tout afficher", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ZurikaPrimaryDark)
                            }
                            TextButton(
                                onClick = { onSetAllCategoriesVisible(false) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("filter_hide_all_button")
                            ) {
                                Text("Tout masquer", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.6f))
                            }
                        }
                    }
                }

                // Map Layer Selector Button
                Box {
                    Surface(
                        onClick = { showMapTypeMenu = !showMapTypeMenu },
                        shape = RoundedCornerShape(16.dp),
                        color = DarkSurface.copy(alpha = 0.94f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ZurikaAmber.copy(alpha = 0.5f)),
                        shadowElevation = 4.dp,
                        modifier = Modifier.testTag("map_layers_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = "Layers",
                                tint = ZurikaAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when (mapDisplayMode) {
                                    MapDisplayMode.GOOGLE_MAP_NORMAL -> "Plan"
                                    MapDisplayMode.GOOGLE_MAP_SATELLITE -> "Satellite"
                                    MapDisplayMode.GOOGLE_MAP_TERRAIN -> "Relief"
                                    MapDisplayMode.LOME_VECTOR -> "Stylisé"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ZurikaAmber
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showMapTypeMenu,
                        onDismissRequest = { showMapTypeMenu = false },
                        modifier = Modifier.background(DarkSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Google Maps (Plan Standard)", color = Color.White, fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Map, contentDescription = null, tint = ZurikaPrimary) },
                            onClick = {
                                mapDisplayMode = MapDisplayMode.GOOGLE_MAP_NORMAL
                                showMapTypeMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Google Maps (Satellite)", color = Color.White, fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Satellite, contentDescription = null, tint = ZurikaAmber) },
                            onClick = {
                                mapDisplayMode = MapDisplayMode.GOOGLE_MAP_SATELLITE
                                showMapTypeMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Google Maps (Relief / Terrain)", color = Color.White, fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Terrain, contentDescription = null, tint = ZurikaStatusOpen) },
                            onClick = {
                                mapDisplayMode = MapDisplayMode.GOOGLE_MAP_TERRAIN
                                showMapTypeMenu = false
                            }
                        )
                        Divider(color = Color.White.copy(alpha = 0.1f))
                        DropdownMenuItem(
                            text = { Text("Vue Lomé stylisée (Éco / Hors-ligne)", color = Color.White, fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null, tint = ZurikaStatusClosed) },
                            onClick = {
                                mapDisplayMode = MapDisplayMode.LOME_VECTOR
                                showMapTypeMenu = false
                            }
                        )
                    }
                }

                // Refresh Dépanneurs Button (triggers data retrieval with shimmer animation)
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onRefresh()
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = DarkSurface.copy(alpha = 0.94f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ZurikaPrimary.copy(alpha = 0.6f)),
                    shadowElevation = 4.dp,
                    modifier = Modifier.testTag("map_refresh_button")
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Actualiser les dépanneurs",
                                tint = ZurikaPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Actualiser",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Row 2: Direct 1-Tap Quick Category Toggle Chips floating at top of map
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_marker_filter_row")
            ) {
                item {
                    MapQuickFilterChip(
                        label = "Station",
                        count = stationCount,
                        icon = Icons.Default.LocalGasStation,
                        activeColor = ZurikaPrimary,
                        isVisible = visibleCategories.contains(ServiceCategory.STATION),
                        testTag = "quick_chip_station",
                        onClick = { onToggleCategoryVisibility(ServiceCategory.STATION) }
                    )
                }
                item {
                    MapQuickFilterChip(
                        label = "Mécanicien",
                        count = mechanicCount,
                        icon = Icons.Default.Build,
                        activeColor = ZurikaAmber,
                        isVisible = visibleCategories.contains(ServiceCategory.MECHANIC),
                        testTag = "quick_chip_mechanic",
                        onClick = { onToggleCategoryVisibility(ServiceCategory.MECHANIC) }
                    )
                }
                item {
                    MapQuickFilterChip(
                        label = "Vulcanisateur",
                        count = vulcanizerCount,
                        icon = Icons.Default.TwoWheeler,
                        activeColor = Color(0xFFA855F7),
                        isVisible = visibleCategories.contains(ServiceCategory.VULCANIZER),
                        testTag = "quick_chip_vulcanizer",
                        onClick = { onToggleCategoryVisibility(ServiceCategory.VULCANIZER) }
                    )
                }
                item {
                    MapQuickFilterChip(
                        label = "Dépannage",
                        count = towingCount,
                        icon = Icons.Default.Warning,
                        activeColor = ZurikaStatusClosed,
                        isVisible = visibleCategories.contains(ServiceCategory.TOWING),
                        testTag = "quick_chip_towing",
                        onClick = { onToggleCategoryVisibility(ServiceCategory.TOWING) }
                    )
                }
            }
        }

        // Floating Action Controls on Map (GPS Recenter, Traffic, Zoom +, Zoom -)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 68.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Main GPS Recenter Floating Action Button (M3 Compliant touch target & elevation)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Quick "Ma position" label pill when camera is moved away from current location
                AnimatedVisibility(
                    visible = isCameraAwayFromUser,
                    enter = fadeIn() + slideInHorizontally { it },
                    exit = fadeOut() + slideOutHorizontally { it }
                ) {
                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onResetToUserLocation()
                            coroutineScope.launch {
                                safeAnimateCamera(
                                    cameraPositionState = cameraPositionState,
                                    target = userPosition,
                                    zoom = 16.5f,
                                    durationMs = 600
                                )
                            }
                            recenterTrigger++
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = DarkSurface.copy(alpha = 0.95f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ZurikaPrimary),
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.NearMe,
                                contentDescription = null,
                                tint = ZurikaPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Ma position",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ZurikaPrimary
                            )
                        }
                    }
                }

                Box {
                    FloatingActionButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onResetToUserLocation()
                            coroutineScope.launch {
                                safeAnimateCamera(
                                    cameraPositionState = cameraPositionState,
                                    target = userPosition,
                                    zoom = 16.5f,
                                    durationMs = 600
                                )
                            }
                            recenterTrigger++
                        },
                        containerColor = if (isCameraAwayFromUser) ZurikaPrimary else DarkSurface.copy(alpha = 0.95f),
                        contentColor = if (isCameraAwayFromUser) Color.White else ZurikaPrimary,
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 6.dp,
                            pressedElevation = 10.dp
                        ),
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("recenter_gps_fab")
                            .testTag("recenter_user_location_fab")
                            .testTag("recenter_map_button")
                    ) {
                        Icon(
                            imageVector = if (isCameraAwayFromUser) Icons.Default.MyLocation else Icons.Default.GpsFixed,
                            contentDescription = "Recentrer rapidement la caméra sur ma position actuelle",
                            tint = if (isCameraAwayFromUser) Color.White else ZurikaPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Active indicator notification dot when camera is panned away from user position
                    if (isCameraAwayFromUser) {
                        Surface(
                            shape = CircleShape,
                            color = ZurikaAmber,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White),
                            modifier = Modifier
                                .size(13.dp)
                                .align(Alignment.TopEnd)
                        ) {}
                    }
                }
            }

            // Live Traffic Toggle
            FloatingActionButton(
                onClick = { isTrafficEnabled = !isTrafficEnabled },
                containerColor = if (isTrafficEnabled) ZurikaAmber else DarkSurface.copy(alpha = 0.95f),
                contentColor = if (isTrafficEnabled) DarkBackground else Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .size(48.dp)
                    .testTag("toggle_traffic_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Traffic,
                    contentDescription = "Trafic en direct",
                    modifier = Modifier.size(22.dp)
                )
            }

            // Zoom In (+)
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        safeZoomIn(cameraPositionState)
                    }
                    vectorZoomInTrigger++
                },
                containerColor = DarkSurface.copy(alpha = 0.95f),
                contentColor = Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .size(48.dp)
                    .testTag("zoom_in_button")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Zoom In", tint = Color.White, modifier = Modifier.size(22.dp))
            }

            // Zoom Out (-)
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        safeZoomOut(cameraPositionState)
                    }
                    vectorZoomOutTrigger++
                },
                containerColor = DarkSurface.copy(alpha = 0.95f),
                contentColor = Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .size(48.dp)
                    .testTag("zoom_out_button")
            ) {
                Icon(imageVector = Icons.Default.Remove, contentDescription = "Zoom Out", tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }

        // Floating Recenter Feedback Banner on Map
        AnimatedVisibility(
            visible = mapCenterLat != null && mapCenterLng != null,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 64.dp, start = 12.dp, end = 80.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = DarkSurface.copy(alpha = 0.95f),
                border = androidx.compose.foundation.BorderStroke(1.dp, ZurikaAmber.copy(alpha = 0.7f)),
                shadowElevation = 8.dp,
                modifier = Modifier.testTag("map_recenter_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = ZurikaAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = "Recentré : ${mapCenterLabel ?: "Lieu sélectionné"}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        onClick = onResetToUserLocation,
                        shape = RoundedCornerShape(10.dp),
                        color = ZurikaPrimary,
                        modifier = Modifier
                            .height(26.dp)
                            .testTag("banner_reset_gps_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Ma position",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Google Maps Watermark & Branding Attribution
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.Black.copy(alpha = 0.6f),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = ZurikaPrimary,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Google Maps • Togo",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }

        // Floating On-Map Travel Time Indicator & Mini-popup Card above map when a marker is tapped
        AnimatedVisibility(
            visible = selectedService != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (selectedService != null) {
                val distanceKm = routeEstimate?.distanceKm ?: ZurikaRepository.calculateDistanceKm(
                    userLat, userLng,
                    selectedService.latitude, selectedService.longitude
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Visual Indicator on the map displaying estimated travel time to selected service / dépanneur
                    if (routeEstimate != null) {
                        MapRouteEtaFloatingIndicator(
                            service = selectedService,
                            routeEstimate = routeEstimate,
                            onFitRoute = {
                                coroutineScope.launch {
                                    safeFitBounds(
                                        cameraPositionState = cameraPositionState,
                                        pos1 = userPosition,
                                        pos2 = LatLng(selectedService.latitude, selectedService.longitude)
                                    )
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    MiniServicePopupCard(
                        service = selectedService,
                        distanceKm = distanceKm,
                        routeEstimate = routeEstimate,
                        onOpenDetail = { onOpenDetail(selectedService) },
                        onCall = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${selectedService.phoneNumber}"))
                            context.startActivity(intent)
                        },
                        onNavigate = {
                            try {
                                val gmmIntentUri = Uri.parse("google.navigation:q=${selectedService.latitude},${selectedService.longitude}&mode=d")
                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                                    setPackage("com.google.android.apps.maps")
                                }
                                if (mapIntent.resolveActivity(context.packageManager) != null) {
                                    context.startActivity(mapIntent)
                                } else {
                                    val geoIntent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("geo:${selectedService.latitude},${selectedService.longitude}?q=${selectedService.latitude},${selectedService.longitude}(${Uri.encode(selectedService.name)})")
                                    )
                                    if (geoIntent.resolveActivity(context.packageManager) != null) {
                                        context.startActivity(geoIntent)
                                    } else {
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${selectedService.latitude},${selectedService.longitude}"))
                                        context.startActivity(browserIntent)
                                    }
                                }
                            } catch (e: Exception) {
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${selectedService.latitude},${selectedService.longitude}"))
                                context.startActivity(browserIntent)
                            }
                        },
                        onRequestIntervention = {
                            onOpenDetail(selectedService)
                        },
                        onDismiss = { onSelectService(null) }
                    )
                }
            }
        }

        // Shimmer Loading Animation on the map canvas area during data retrieval
        MapShimmerLoadingOverlay(
            isLoading = isLoading,
            modifier = Modifier.fillMaxSize()
        )

        if (showMissingMapsKeyDialog) {
            AlertDialog(
                onDismissRequest = { showMissingMapsKeyDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = null,
                        tint = ZurikaAmber,
                        modifier = Modifier.size(28.dp)
                    )
                },
                title = {
                    Text(
                        text = "Mode Google Maps",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                },
                text = {
                    Text(
                        text = "Pour basculer vers les tuiles Google Maps (Plan, Satellite, Relief), configurez votre clé API Google Maps dans le panneau Secrets de Google AI Studio (variable MAPS_API_KEY).\n\nEn attendant, la vue cartographique vectorielle de Lomé est 100% active, rapide, fluide et autonome hors-ligne.",
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showMissingMapsKeyDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = ZurikaPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Continuer sur Lomé Stylisé", fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = DarkSurface,
                shape = RoundedCornerShape(18.dp)
            )
        }
    }
}

@Composable
fun UserLocationPin() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.testTag("user_location_marker")
    ) {
        Surface(
            shape = CircleShape,
            color = ZurikaPrimary,
            border = androidx.compose.foundation.BorderStroke(3.dp, Color.White),
            shadowElevation = 10.dp,
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = "Ma position",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = DarkSurface.copy(alpha = 0.9f),
            modifier = Modifier.padding(top = 2.dp)
        ) {
            Text(
                text = "Vous",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }
    }
}

@Composable
fun SearchedLocationMarkerPill(label: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "searched_loc_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.testTag("searched_location_pin")
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = ZurikaAmber,
            shadowElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = DarkBackground,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DarkBackground
                )
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp * pulseScale)
                    .background(ZurikaAmber.copy(alpha = 0.35f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(Color.White, CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(ZurikaAmber, CircleShape)
            )
        }
    }
}

@Composable
fun GoogleMapServicePin(
    service: ServiceEntity,
    isSelected: Boolean,
    distanceKm: Double? = null,
    animationDelayMs: Int = 0,
    filterTriggerKey: Any? = null
) {
    val statusColor = when (service.status) {
        ServiceStatus.OPEN -> ZurikaStatusOpen
        ServiceStatus.BUSY -> ZurikaAmber
        ServiceStatus.CLOSED -> ZurikaStatusClosed
        ServiceStatus.TEMP_UNAVAILABLE -> ZurikaAmber
        ServiceStatus.UNKNOWN -> ZurikaStatusUnknown
    }

    val statusText = when (service.status) {
        ServiceStatus.OPEN -> if (service.openingHours.contains("24h", ignoreCase = true)) "24/7 OUVERT" else "OUVERT"
        ServiceStatus.BUSY -> "OCCUPÉ"
        ServiceStatus.CLOSED -> "FERMÉ"
        ServiceStatus.TEMP_UNAVAILABLE -> "INDISPONIBLE"
        ServiceStatus.UNKNOWN -> "STATUT"
    }

    val formattedDistance = distanceKm?.let {
        if (it < 1.0) "${(it * 1000).toInt()} m" else "%.1f km".format(it)
    }

    val categoryColor = when (service.category) {
        ServiceCategory.STATION -> ZurikaPrimary
        ServiceCategory.MECHANIC -> ZurikaAmber
        ServiceCategory.VULCANIZER -> Color(0xFF10B981)
        ServiceCategory.TOWING -> Color(0xFFEF4444)
    }

    val iconVector = when (service.category) {
        ServiceCategory.STATION -> Icons.Default.LocalGasStation
        ServiceCategory.MECHANIC -> Icons.Default.Build
        ServiceCategory.VULCANIZER -> Icons.Default.TwoWheeler
        ServiceCategory.TOWING -> Icons.Default.Warning
    }

    // Dynamic Bounce Animation on Initial Load and Filter Changes
    val isDepanneur = service.category == ServiceCategory.TOWING ||
        service.name.contains("Dépannage", ignoreCase = true) ||
        service.name.contains("Remorquage", ignoreCase = true) ||
        service.servicesOffered.contains("Dépannage", ignoreCase = true) ||
        service.servicesOffered.contains("Remorquage", ignoreCase = true)

    // High bounce offset for dépanneurs to create a striking gravity drop & rebound effect
    val bounceOffsetY = remember(service.id, filterTriggerKey) {
        Animatable(if (isDepanneur) -110f else -32f)
    }
    val bounceScaleX = remember(service.id, filterTriggerKey) {
        Animatable(if (isDepanneur) 0.5f else 0.2f)
    }
    val bounceScaleY = remember(service.id, filterTriggerKey) {
        Animatable(if (isDepanneur) 1.5f else 0.2f)
    }
    val bounceAlpha = remember(service.id, filterTriggerKey) { Animatable(0f) }

    LaunchedEffect(service.id, filterTriggerKey) {
        val delayTime = if (isDepanneur) {
            // Stagger dépanneurs slightly so they drop like rhythmic eye-catching pins
            (animationDelayMs * 2).coerceIn(40, 350)
        } else {
            animationDelayMs.coerceAtMost(300)
        }
        if (delayTime > 0) {
            kotlinx.coroutines.delay(delayTime.toLong())
        }

        if (isDepanneur) {
            // High-impact multi-stage physical bounce animation specifically for breakdown & tow services
            launch {
                bounceAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 150, easing = LinearEasing)
                )
            }
            launch {
                // Initial rapid gravitational drop from -110f to ground 0f
                bounceOffsetY.animateTo(0f, tween(durationMillis = 280, easing = FastOutLinearInEasing))

                // First landing impact: squash horizontally and compress vertically
                launch {
                    bounceScaleX.animateTo(1.26f, tween(60))
                    bounceScaleX.animateTo(1.0f, tween(90))
                }
                launch {
                    bounceScaleY.animateTo(0.78f, tween(60))
                    bounceScaleY.animateTo(1.0f, tween(90))
                }

                // Rebound 1 (High bounce up to -50f with upward vertical stretch)
                launch {
                    bounceScaleX.animateTo(0.92f, tween(180))
                    bounceScaleX.animateTo(1.0f, tween(100))
                }
                launch {
                    bounceScaleY.animateTo(1.14f, tween(180))
                    bounceScaleY.animateTo(1.0f, tween(100))
                }
                bounceOffsetY.animateTo(-50f, tween(220, easing = LinearOutSlowInEasing))

                // Gravity pulls back down to ground 0f
                bounceOffsetY.animateTo(0f, tween(180, easing = FastOutLinearInEasing))

                // Second impact: moderate squash
                launch {
                    bounceScaleX.animateTo(1.15f, tween(50))
                    bounceScaleX.animateTo(1.0f, tween(80))
                }
                launch {
                    bounceScaleY.animateTo(0.88f, tween(50))
                    bounceScaleY.animateTo(1.0f, tween(80))
                }

                // Rebound 2 (Medium bounce up to -20f)
                bounceOffsetY.animateTo(-20f, tween(150, easing = LinearOutSlowInEasing))
                bounceOffsetY.animateTo(0f, tween(130, easing = FastOutLinearInEasing))

                // Rebound 3 (Subtle settle hop to -7f)
                bounceOffsetY.animateTo(-7f, tween(90, easing = LinearOutSlowInEasing))
                bounceOffsetY.animateTo(0f, tween(80, easing = FastOutLinearInEasing))

                // Stabilize scales to rest
                bounceScaleX.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                bounceScaleY.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            }
        } else {
            launch {
                bounceAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 180, easing = LinearOutSlowInEasing)
                )
            }
            launch {
                bounceOffsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
            launch {
                bounceScaleX.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
                bounceScaleY.snapTo(1f)
            }
        }
    }

    // Pulse animation when selected
    val selectedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "selectedPinScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .graphicsLayer {
                translationY = bounceOffsetY.value
                scaleX = bounceScaleX.value * selectedScale
                scaleY = bounceScaleY.value * selectedScale
                alpha = bounceAlpha.value
            }
            .testTag("google_map_pin_${service.id}")
    ) {
        // Prominent Availability & Distance Status Badge
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isDepanneur) Color(0xFFDC2626) else if (service.status == ServiceStatus.OPEN) ZurikaStatusOpen else DarkSurface,
            contentColor = Color.White,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isDepanneur) Color(0xFFFECACA) else if (service.status == ServiceStatus.OPEN) Color.White.copy(alpha = 0.85f) else statusColor
            ),
            shadowElevation = if (isDepanneur) 8.dp else 5.dp,
            modifier = Modifier
                .padding(bottom = 2.dp)
                .testTag("availability_badge_${service.id}")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                if (isDepanneur) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Dépanneur 24/7",
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = if (formattedDistance != null) "DÉPANNAGE • $formattedDistance" else "DÉPANNAGE 24/7",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 0.4.sp
                    )
                } else {
                    // Availability glowing dot
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                if (service.status == ServiceStatus.OPEN) Color.White else statusColor,
                                CircleShape
                            )
                    )
                    Text(
                        text = if (formattedDistance != null) "$statusText • $formattedDistance" else statusText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (service.status == ServiceStatus.OPEN) Color.White else statusColor,
                        letterSpacing = 0.4.sp
                    )
                }
            }
        }

        // Pin Head with Category Icon & Halo (with Pulsing Attention Beacon for Dépanneurs)
        Box(contentAlignment = Alignment.Center) {
            if (isDepanneur) {
                // Expanding attention radar beacon to draw eye to emergency tow & breakdown services
                val infinitePulse = rememberInfiniteTransition(label = "depanneurPulse")
                val pulseRingScale by infinitePulse.animateFloat(
                    initialValue = 1.0f,
                    targetValue = 2.2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 1500, easing = LinearOutSlowInEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "pulseRingScale"
                )
                val pulseRingAlpha by infinitePulse.animateFloat(
                    initialValue = 0.7f,
                    targetValue = 0.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 1500, easing = LinearOutSlowInEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "pulseRingAlpha"
                )

                // Outer radar glow
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 46.dp else 38.dp)
                        .graphicsLayer {
                            scaleX = pulseRingScale
                            scaleY = pulseRingScale
                            alpha = pulseRingAlpha
                        }
                        .background(Color(0xFFEF4444).copy(alpha = 0.35f), CircleShape)
                        .border(1.5.dp, Color(0xFFEF4444), CircleShape)
                )
            }

            Surface(
                shape = CircleShape,
                color = if (isSelected) categoryColor else if (isDepanneur) Color(0xFFDC2626) else DarkSurface,
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isSelected) 3.dp else if (isDepanneur) 2.5.dp else 2.dp,
                    color = if (isSelected) Color.White else if (isDepanneur) Color(0xFFFCA5A5) else statusColor
                ),
                shadowElevation = if (isSelected) 14.dp else if (isDepanneur) 10.dp else 6.dp,
                modifier = Modifier.size(if (isSelected) 46.dp else 38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = service.name,
                        tint = if (isSelected) Color.White else if (isDepanneur) Color.White else categoryColor,
                        modifier = Modifier.size(if (isSelected) 24.dp else 20.dp)
                    )

                    // Real-time Status indicator dot
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(statusColor, CircleShape)
                            .border(1.5.dp, Color.White, CircleShape)
                            .align(Alignment.TopEnd)
                    )
                }
            }
        }

        // Pointer Stem
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(6.dp)
                .background(if (isSelected) Color.White else if (isDepanneur) Color(0xFFDC2626) else statusColor)
        )

        // Attached Distance Badge Pill on every pin
        if (formattedDistance != null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) ZurikaPrimary else DarkSurface.copy(alpha = 0.94f),
                contentColor = Color.White,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSelected) Color.White else ZurikaAmber.copy(alpha = 0.75f)
                ),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .padding(top = 1.dp)
                    .testTag("pin_distance_badge_${service.id}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NearMe,
                        contentDescription = "Distance",
                        tint = if (isSelected) Color.White else ZurikaAmber,
                        modifier = Modifier.size(9.dp)
                    )
                    Text(
                        text = formattedDistance,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }

        // Mini Label if selected
        if (isSelected) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = DarkSurface.copy(alpha = 0.95f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                shadowElevation = 6.dp,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = if (formattedDistance != null) "${service.name} ($formattedDistance)" else service.name,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

fun ServiceEntity.isDepanneurService(): Boolean {
    return category == ServiceCategory.TOWING ||
        category == ServiceCategory.MECHANIC ||
        servicesOffered.contains("remorquage", ignoreCase = true) ||
        servicesOffered.contains("dépannage", ignoreCase = true) ||
        name.contains("dépannage", ignoreCase = true) ||
        name.contains("sos", ignoreCase = true)
}

/**
 * Floating on-map visual indicator HUD displaying the estimated travel time,
 * route breakdown, and transport mode estimates between the user and the selected service/dépanneur.
 */
@Composable
fun MapRouteEtaFloatingIndicator(
    service: ServiceEntity,
    routeEstimate: RouteCalculator.RouteEstimate,
    onFitRoute: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDepanneur = service.isDepanneurService()
    val accentColor = if (isDepanneur) ZurikaAmber else ZurikaPrimary

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = DarkSurface.copy(alpha = 0.96f),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, accentColor.copy(alpha = 0.85f)),
        shadowElevation = 12.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("map_route_eta_indicator")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Header Row: Type badge + Action button to view entire route
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = accentColor.copy(alpha = 0.2f),
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isDepanneur) Icons.Default.Warning else Icons.Default.Schedule,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isDepanneur) "TEMPS DE TRAJET ESTIMÉ (DÉPANNEUR)" else "TEMPS DE TRAJET ESTIMÉ",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor,
                        letterSpacing = 0.4.sp,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    onClick = onFitRoute,
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.15f),
                    modifier = Modifier.testTag("btn_fit_route_bounds")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NearMe,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Aperçu trajet",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Origin -> Destination Trajectory Line
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Votre position",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = service.name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Time and Distance Estimates
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Car / Tow truck estimate
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("eta_time_car")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = ZurikaAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "${routeEstimate.carTimeMinutes} min",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = if (isDepanneur) "Camion/Auto" else "Voiture",
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Moto express / Zémidjan estimate
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("eta_time_moto")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.TwoWheeler,
                            contentDescription = null,
                            tint = ZurikaStatusOpen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "${routeEstimate.motoTimeMinutes} min",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ZurikaStatusOpen
                            )
                            Text(
                                text = "Moto SOS",
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Total Distance
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier
                        .weight(0.9f)
                        .testTag("eta_distance")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "%.1f km".format(routeEstimate.distanceKm),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = accentColor
                        )
                        Text(
                            text = "Distance",
                            fontSize = 9.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RouteEtaMarkerPill(
    carMinutes: Int,
    motoMinutes: Int,
    distanceKm: Double,
    isDepanneur: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isDepanneur) Color(0xFF1E1B18) else DarkSurface.copy(alpha = 0.95f),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isDepanneur) ZurikaAmber else ZurikaPrimary
        ),
        shadowElevation = 8.dp,
        modifier = Modifier.testTag("route_eta_marker_pill")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (isDepanneur) Icons.Default.Warning else Icons.Default.Schedule,
                    contentDescription = null,
                    tint = if (isDepanneur) ZurikaAmber else ZurikaPrimary,
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = if (isDepanneur) "DÉPANNEUR • %.1f km".format(distanceKm) else "TRAJET • %.1f km".format(distanceKm),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDepanneur) ZurikaAmber else Color.White,
                    letterSpacing = 0.3.sp
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Car / Truck ETA
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = ZurikaAmber,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "$carMinutes min",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Text(text = "•", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)

                // Moto ETA
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TwoWheeler,
                        contentDescription = null,
                        tint = ZurikaStatusOpen,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "$motoMinutes min",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ZurikaStatusOpen
                    )
                }
            }
        }
    }
}

@Composable
fun LomeStylizedVectorCanvas(
    services: List<ServiceEntity>,
    selectedServiceId: String?,
    userLat: Double,
    userLng: Double,
    onSelectService: (String?) -> Unit,
    recenterTrigger: Int = 0,
    zoomInTrigger: Int = 0,
    zoomOutTrigger: Int = 0,
    isTrafficEnabled: Boolean = false,
    routeEstimate: RouteCalculator.RouteEstimate? = null
) {
    var scale by remember { mutableFloatStateOf(1.2f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // Respond to Zoom In (+) button
    LaunchedEffect(zoomInTrigger) {
        if (zoomInTrigger > 0) {
            scale = (scale * 1.35f).coerceAtMost(5.0f)
        }
    }

    // Respond to Zoom Out (-) button
    LaunchedEffect(zoomOutTrigger) {
        if (zoomOutTrigger > 0) {
            scale = (scale / 1.35f).coerceAtLeast(0.45f)
        }
    }

    // Reset offset and zoom when recenter trigger fires
    LaunchedEffect(recenterTrigger) {
        if (recenterTrigger > 0) {
            scale = 1.2f
            offsetX = 0f
            offsetY = 0f
        }
    }

    // Automatically recenter vector map camera when a service is selected or search result is targeted
    LaunchedEffect(selectedServiceId) {
        if (selectedServiceId != null) {
            val sel = services.find { it.id == selectedServiceId }
            if (sel != null) {
                val dLat = (sel.latitude - userLat) * 14000.0 * scale
                val dLng = (sel.longitude - userLng) * 14000.0 * scale
                offsetX = -dLng.toFloat()
                offsetY = dLat.toFloat()
            }
        }
    }

    val density = LocalDensity.current
    val isDark = isSystemInDarkTheme()
    val mapBgColor = if (isDark) Color(0xFF0B132B) else Color(0xFFF1F5F9)
    val mapRoadColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
    val mapHighwayColor = if (isDark) Color(0xFF0284C7).copy(alpha = 0.5f) else Color(0xFF93C5FD)
    val mapOceanColor = if (isDark) Color(0xFF0A2540) else Color(0xFF38BDF8).copy(alpha = 0.35f)
    val mapSandColor = if (isDark) Color(0xFF4A4031) else Color(0xFFFDE68A).copy(alpha = 0.6f)
    val mapLagoonColor = if (isDark) Color(0xFF083344) else Color(0xFF06B6D4).copy(alpha = 0.35f)
    val mapAirportColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    val mapGridColor = if (isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.04f)

    // Animated pulse for user location marker
    val infiniteTransition = rememberInfiniteTransition(label = "vector_map_anim")
    val userPulseRadius by infiniteTransition.animateFloat(
        initialValue = 16f,
        targetValue = 36f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "user_pulse_radius"
    )
    val userPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "user_pulse_alpha"
    )

    // Helper for projecting GPS coordinates to screen pixel coordinates
    val cx = canvasSize.width / 2f + offsetX
    val cy = canvasSize.height / 2f + offsetY

    fun toScreen(lat: Double, lng: Double): Offset {
        val px = cx + ((lng - userLng) * 14000.0 * scale).toFloat()
        val py = cy - ((lat - userLat) * 14000.0 * scale).toFloat()
        return Offset(px, py)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(mapBgColor)
            .onSizeChanged { canvasSize = it }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val oldScale = scale
                    val newScale = (scale * zoom).coerceIn(0.45f, 5.0f)
                    val w = canvasSize.width.toFloat()
                    val h = canvasSize.height.toFloat()
                    offsetX = (offsetX - (centroid.x - (w / 2f))) * (newScale / oldScale) + (centroid.x - (w / 2f)) + pan.x
                    offsetY = (offsetY - (centroid.y - (h / 2f))) * (newScale / oldScale) + (centroid.y - (h / 2f)) + pan.y
                    scale = newScale
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        onSelectService(null)
                    },
                    onDoubleTap = { centroid ->
                        val oldScale = scale
                        val newScale = (scale * 1.4f).coerceAtMost(5.0f)
                        val w = canvasSize.width.toFloat()
                        val h = canvasSize.height.toFloat()
                        offsetX = (offsetX - (centroid.x - (w / 2f))) * (newScale / oldScale) + (centroid.x - (w / 2f))
                        offsetY = (offsetY - (centroid.y - (h / 2f))) * (newScale / oldScale) + (centroid.y - (h / 2f))
                        scale = newScale
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            drawRect(color = mapBgColor)

            // 1. Street grid mesh
            val step = (70f * scale).coerceIn(30f, 250f)
            var gx = (offsetX % step)
            while (gx < width) {
                drawLine(color = mapGridColor, start = Offset(gx, 0f), end = Offset(gx, height), strokeWidth = 1f)
                gx += step
            }
            var gy = (offsetY % step)
            while (gy < height) {
                drawLine(color = mapGridColor, start = Offset(0f, gy), end = Offset(width, gy), strokeWidth = 1f)
                gy += step
            }

            // 2. Golfe de Guinée / Plage de Lomé (Coastline ~ lat 6.1215)
            val coastPt = toScreen(6.1215, userLng)
            val coastY = coastPt.y
            if (coastY < height + 100f) {
                // Beach sand strip
                drawRect(
                    color = mapSandColor,
                    topLeft = Offset(0f, (coastY - 18f * scale).coerceAtLeast(0f)),
                    size = androidx.compose.ui.geometry.Size(width, (height - coastY + 30f).coerceAtLeast(0f))
                )
                // Ocean
                drawRect(
                    color = mapOceanColor,
                    topLeft = Offset(0f, coastY.coerceAtLeast(0f)),
                    size = androidx.compose.ui.geometry.Size(width, (height - coastY).coerceAtLeast(0f))
                )
                // Coastline wave accent
                drawLine(
                    color = Color(0xFF38BDF8),
                    start = Offset(0f, coastY),
                    end = Offset(width, coastY),
                    strokeWidth = 2.5f * scale
                )
            }

            // 3. Lagune de Bè (~ 6.1360, 1.2520)
            val lagoonPt = toScreen(6.1360, 1.2520)
            drawOval(
                color = mapLagoonColor,
                topLeft = Offset(lagoonPt.x - 70f * scale, lagoonPt.y - 30f * scale),
                size = androidx.compose.ui.geometry.Size(140f * scale, 60f * scale)
            )

            // 4. Port Autonome de Lomé pier (~ 6.1340, 1.2820)
            val portPt = toScreen(6.1340, 1.2820)
            drawLine(
                color = Color(0xFF64748B),
                start = portPt,
                end = Offset(portPt.x, portPt.y + 65f * scale),
                strokeWidth = 14f * scale
            )

            // 5. Aéroport Gnassingbé Eyadéma (AIGE) (~ 6.1660, 1.2500)
            val aéroPt = toScreen(6.1660, 1.2500)
            drawRoundRect(
                color = mapAirportColor,
                topLeft = Offset(aéroPt.x - 55f * scale, aéroPt.y - 15f * scale),
                size = androidx.compose.ui.geometry.Size(110f * scale, 30f * scale),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f * scale, 6f * scale)
            )
            // Runway centerline
            drawLine(
                color = Color.White.copy(alpha = 0.6f),
                start = Offset(aéroPt.x - 45f * scale, aéroPt.y),
                end = Offset(aéroPt.x + 45f * scale, aéroPt.y),
                strokeWidth = 3f * scale
            )

            // 6. Major Arteries & Boulevards of Lomé:
            // Boulevard du Mono along beach at lat 6.1235
            val monoPt = toScreen(6.1235, userLng)
            drawLine(
                color = mapHighwayColor,
                start = Offset(0f, monoPt.y),
                end = Offset(width, monoPt.y),
                strokeWidth = 14f * scale
            )

            // Boulevard du 13 Janvier (circulaire)
            val p1 = toScreen(6.1235, 1.2100)
            val p2 = toScreen(6.1360, 1.2100)
            val p3 = toScreen(6.1430, 1.2220)
            val p4 = toScreen(6.1350, 1.2350)
            val p5 = toScreen(6.1235, 1.2400)
            val ringPath = Path().apply {
                moveTo(p1.x, p1.y)
                quadraticBezierTo(p2.x, p2.y, p3.x, p3.y)
                quadraticBezierTo(p4.x, p4.y, p5.x, p5.y)
            }
            drawPath(
                path = ringPath,
                color = mapHighwayColor,
                style = Stroke(width = 13f * scale)
            )

            // Route Nationale N1 (Axe Tokoin -> Agoè)
            val n1Start = toScreen(6.1430, 1.2220)
            val n1End = toScreen(6.2300, 1.2050)
            drawLine(
                color = ZurikaPrimary.copy(alpha = 0.45f),
                start = n1Start,
                end = n1End,
                strokeWidth = 16f * scale
            )

            // Avenue de la Libération & Boulevard de la Paix
            val libStart = toScreen(6.1340, 1.2820)
            val libEnd = toScreen(6.1360, 1.2220)
            drawLine(
                color = mapRoadColor,
                start = libStart,
                end = libEnd,
                strokeWidth = 11f * scale
            )

            val paixStart = toScreen(6.1480, 1.2380)
            val paixEnd = toScreen(6.1360, 1.2220)
            drawLine(
                color = mapRoadColor,
                start = paixStart,
                end = paixEnd,
                strokeWidth = 10f * scale
            )

            // 7. Live Traffic Layer (when enabled)
            if (isTrafficEnabled) {
                // Fluide on Boulevard du Mono (Green)
                drawLine(
                    color = ZurikaStatusOpen,
                    start = Offset(0f, monoPt.y),
                    end = Offset(width * 0.7f, monoPt.y),
                    strokeWidth = 4f * scale
                )
                // Ralenti on Boulevard du 13 Janvier (Amber/Orange)
                drawPath(
                    path = ringPath,
                    color = ZurikaAmber,
                    style = Stroke(width = 4f * scale)
                )
                // Dense on N1 Tokoin (Red)
                drawLine(
                    color = ZurikaStatusClosed,
                    start = n1Start,
                    end = Offset(n1Start.x + (n1End.x - n1Start.x) * 0.4f, n1Start.y + (n1End.y - n1Start.y) * 0.4f),
                    strokeWidth = 5f * scale
                )
            }

            // 8. User Position Dot with GPS Radar Pulse
            val userScreen = toScreen(userLat, userLng)
            drawCircle(
                color = ZurikaPrimary.copy(alpha = userPulseAlpha),
                radius = userPulseRadius * scale,
                center = userScreen
            )
            drawCircle(
                color = Color.White,
                radius = 9f * scale,
                center = userScreen
            )
            drawCircle(
                color = ZurikaPrimary,
                radius = 6f * scale,
                center = userScreen
            )

            // 9. Polyline Route to Selected Service
            val sel = services.find { it.id == selectedServiceId }
            if (sel != null) {
                val isDep = sel.isDepanneurService()
                val destScreen = toScreen(sel.latitude, sel.longitude)

                val midX = destScreen.x
                val midY = userScreen.y

                val routePath = Path().apply {
                    moveTo(userScreen.x, userScreen.y)
                    lineTo(midX, midY)
                    lineTo(destScreen.x, destScreen.y)
                }

                // Route shadow outline
                drawPath(
                    path = routePath,
                    color = Color.Black.copy(alpha = 0.5f),
                    style = Stroke(width = 9f * scale, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
                )
                // Route foreground line
                drawPath(
                    path = routePath,
                    color = if (isDep) ZurikaAmber else ZurikaPrimary,
                    style = Stroke(width = 5.5f * scale, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
                )
            }
        }

        // Overlay: Interactive Pins and ETA Badge
        if (canvasSize.width > 0 && canvasSize.height > 0) {
            // Mid-route ETA Pill floating on vector route
            val sel = services.find { it.id == selectedServiceId }
            if (sel != null && routeEstimate != null) {
                val isDep = sel.isDepanneurService()
                val userScreen = toScreen(userLat, userLng)
                val destScreen = toScreen(sel.latitude, sel.longitude)
                val midScreen = Offset((userScreen.x + destScreen.x) / 2f, (userScreen.y + destScreen.y) / 2f)
                val midXDp = with(density) { midScreen.x.toDp() }
                val midYDp = with(density) { midScreen.y.toDp() }

                Box(
                    modifier = Modifier
                        .offset(x = midXDp - 65.dp, y = midYDp - 24.dp)
                        .testTag("vector_route_eta_pill")
                ) {
                    RouteEtaMarkerPill(
                        carMinutes = routeEstimate.carTimeMinutes,
                        motoMinutes = routeEstimate.motoTimeMinutes,
                        distanceKm = routeEstimate.distanceKm,
                        isDepanneur = isDep
                    )
                }
            }

            // Interactive Pins placed at exact geographic projections
            services.forEachIndexed { index, service ->
                val isSelected = service.id == selectedServiceId
                val pinScreen = toScreen(service.latitude, service.longitude)
                val pinXDp = with(density) { pinScreen.x.toDp() }
                val pinYDp = with(density) { pinScreen.y.toDp() }
                val distanceKm = ZurikaRepository.calculateDistanceKm(userLat, userLng, service.latitude, service.longitude)

                Box(
                    modifier = Modifier
                        .offset(x = pinXDp - 24.dp, y = pinYDp - 48.dp)
                        .clickable { onSelectService(service.id) }
                        .testTag("vector_pin_${service.id}")
                ) {
                    GoogleMapServicePin(
                        service = service,
                        isSelected = isSelected,
                        distanceKm = distanceKm,
                        animationDelayMs = (index * 35).coerceAtMost(250),
                        filterTriggerKey = services.map { it.id }.hashCode()
                    )
                }
            }
        }
    }
}

@Composable
fun MiniServicePopupCard(
    service: ServiceEntity,
    distanceKm: Double,
    routeEstimate: RouteCalculator.RouteEstimate? = null,
    onOpenDetail: () -> Unit,
    onCall: () -> Unit,
    onNavigate: () -> Unit = {},
    onRequestIntervention: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val statusColor = when (service.status) {
        ServiceStatus.OPEN -> ZurikaStatusOpen
        ServiceStatus.BUSY -> ZurikaAmber
        ServiceStatus.CLOSED -> ZurikaStatusClosed
        ServiceStatus.TEMP_UNAVAILABLE -> ZurikaAmber
        ServiceStatus.UNKNOWN -> ZurikaStatusUnknown
    }

    val statusText = when (service.status) {
        ServiceStatus.OPEN -> "OUVERT"
        ServiceStatus.BUSY -> "OCCUPÉ"
        ServiceStatus.CLOSED -> "FERMÉ"
        ServiceStatus.TEMP_UNAVAILABLE -> "INDISPONIBLE"
        ServiceStatus.UNKNOWN -> "INCONNU"
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 14.dp,
        modifier = Modifier
            .fillMaxWidth()
            .sharedServiceCard(service.id, RoundedCornerShape(24.dp))
            .testTag("mini_popup_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Category Icon Badge
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = statusColor.copy(alpha = 0.12f),
                    modifier = Modifier
                        .size(52.dp)
                        .sharedServiceHero(service.id, RoundedCornerShape(16.dp))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when (service.category) {
                                ServiceCategory.STATION -> Icons.Default.LocalGasStation
                                ServiceCategory.MECHANIC -> Icons.Default.Build
                                ServiceCategory.VULCANIZER -> Icons.Default.TwoWheeler
                                ServiceCategory.TOWING -> Icons.Default.Warning
                            },
                            contentDescription = service.name,
                            tint = statusColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = service.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .weight(1f)
                                .sharedServiceTitle(service.id)
                        )

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = statusColor.copy(alpha = 0.12f),
                            modifier = Modifier.sharedServiceStatus(service.id)
                        ) {
                            Text(
                                text = statusText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${service.address} • %.1f km".format(distanceKm),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Estimated Travel Times Breakdown
            if (routeEstimate != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("travel_time_estimates_row")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        // Car Time
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = "Voiture",
                                tint = ZurikaPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = "${routeEstimate.carTimeMinutes} min",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Voiture",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Divider(
                            modifier = Modifier
                                .height(20.dp)
                                .width(1.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )

                        // Moto-taxi (Zémidjan)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TwoWheeler,
                                contentDescription = "Moto",
                                tint = ZurikaAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = "${routeEstimate.motoTimeMinutes} min",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Zémidjan",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Divider(
                            modifier = Modifier
                                .height(20.dp)
                                .width(1.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )

                        // Walking Time
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DirectionsWalk,
                                contentDescription = "À pied",
                                tint = ZurikaStatusOpen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = "${routeEstimate.walkingTimeMinutes} min",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "À pied",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Rating & Fuel / Services snippet
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .sharedServiceRating(service.id)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Star",
                        tint = ZurikaAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${service.rating}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp
                    )
                    Text(
                        text = " (${service.reviewCount} avis)",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }

                if (service.priceInfo.isNotBlank()) {
                    Text(
                        text = service.priceInfo,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ZurikaPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Appeler, Y aller & Intervention / Détails
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .sharedServiceActions(service.id),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCall,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("call_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ZurikaStatusOpen)
                ) {
                    Icon(imageVector = Icons.Default.Call, contentDescription = "Call", modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Appeler", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onNavigate,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("go_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ZurikaPrimary)
                ) {
                    Icon(imageVector = Icons.Default.Directions, contentDescription = "Itinéraire", modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Itinéraire", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onOpenDetail,
                    modifier = Modifier
                        .weight(1.1f)
                        .height(44.dp)
                        .testTag("open_detail_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ZurikaAmber,
                        contentColor = DarkBackground
                    )
                ) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = "Intervention", modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Détails", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MarkerVisibilityFilterMenuItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tintColor: Color,
    count: Int,
    isChecked: Boolean,
    testTag: String,
    onToggle: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = tintColor.copy(alpha = 0.2f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = tintColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = tintColor.copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = count.toString(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = tintColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = subtitle,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 1
                    )
                }
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = ZurikaPrimary,
                        uncheckedColor = Color.White.copy(alpha = 0.4f),
                        checkmarkColor = Color.White
                    )
                )
            }
        },
        onClick = onToggle,
        modifier = Modifier.testTag(testTag)
    )
}

@Composable
fun MapQuickFilterChip(
    label: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    activeColor: Color,
    isVisible: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isVisible) DarkSurface.copy(alpha = 0.95f) else DarkSurface.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isVisible) activeColor else Color.White.copy(alpha = 0.15f)
        ),
        shadowElevation = if (isVisible) 4.dp else 0.dp,
        modifier = Modifier.testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(if (isVisible) activeColor else Color.Gray.copy(alpha = 0.5f), CircleShape)
            )
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isVisible) activeColor else Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isVisible) FontWeight.Bold else FontWeight.Normal,
                color = if (isVisible) Color.White else Color.White.copy(alpha = 0.5f)
            )
            Surface(
                shape = CircleShape,
                color = if (isVisible) activeColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.1f)
            ) {
                Text(
                    text = count.toString(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isVisible) activeColor else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }
        }
    }
}

private suspend fun safeFitBounds(
    cameraPositionState: CameraPositionState,
    pos1: LatLng,
    pos2: LatLng,
    paddingPx: Int = 180
) {
    try {
        val bounds = LatLngBounds.builder()
            .include(pos1)
            .include(pos2)
            .build()
        cameraPositionState.animate(
            update = CameraUpdateFactory.newLatLngBounds(bounds, paddingPx),
            durationMs = 600
        )
    } catch (e: Throwable) {
        val mid = LatLng((pos1.latitude + pos2.latitude) / 2.0, (pos1.longitude + pos2.longitude) / 2.0)
        safeAnimateCamera(cameraPositionState, mid, 14f, 600)
    }
}

private suspend fun safeAnimateCamera(
    cameraPositionState: CameraPositionState,
    target: LatLng,
    zoom: Float = 16f,
    durationMs: Int = 600
) {
    try {
        cameraPositionState.animate(
            update = CameraUpdateFactory.newLatLngZoom(target, zoom),
            durationMs = durationMs
        )
    } catch (e: Throwable) {
        try {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(target, zoom)
        } catch (_: Throwable) {}
    }
}

private suspend fun safeZoomIn(cameraPositionState: CameraPositionState) {
    try {
        cameraPositionState.animate(CameraUpdateFactory.zoomIn())
    } catch (e: Throwable) {
        try {
            val curr = cameraPositionState.position
            cameraPositionState.position = CameraPosition(curr.target, (curr.zoom + 1f).coerceAtMost(21f), curr.tilt, curr.bearing)
        } catch (_: Throwable) {}
    }
}

private suspend fun safeZoomOut(cameraPositionState: CameraPositionState) {
    try {
        cameraPositionState.animate(CameraUpdateFactory.zoomOut())
    } catch (e: Throwable) {
        try {
            val curr = cameraPositionState.position
            cameraPositionState.position = CameraPosition(curr.target, (curr.zoom - 1f).coerceAtLeast(2f), curr.tilt, curr.bearing)
        } catch (_: Throwable) {}
    }
}

