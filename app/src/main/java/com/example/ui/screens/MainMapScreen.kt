package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale
import com.example.data.*
import com.example.ui.MapState
import com.example.ui.animation.*
import com.example.ui.theme.*
import com.example.util.LocationSearchHelper
import com.example.util.SearchResultLocation
import com.example.util.LocationType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MainMapScreen(
    userName: String,
    services: List<ServiceEntity>,
    mapState: MapState,
    isProMode: Boolean,
    isAdminMode: Boolean,
    recentSearches: List<com.example.data.SearchHistoryEntity> = emptyList(),
    onSearchQueryChange: (String) -> Unit,
    onCommitSearchQuery: (String) -> Unit = {},
    onSelectCategoryFilter: (ServiceCategory?) -> Unit,
    onToggleCategoryVisibility: (ServiceCategory) -> Unit = {},
    onSetAllCategoriesVisible: (Boolean) -> Unit = {},
    onFindNearest: () -> Unit,
    onSelectService: (String?) -> Unit,
    onToggleListView: () -> Unit,
    onOpenServiceDetail: (ServiceEntity) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenInterventionDispatch: () -> Unit,
    onOpenProSpace: () -> Unit,
    onOpenAdminDashboard: () -> Unit,
    onRecenterOnLocation: (Double, Double, String, Float) -> Unit = { _, _, _, _ -> },
    onResetToUserLocation: () -> Unit = {},
    onRefreshDepanneurs: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val permissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    val isFineGranted = permissionState.permissions.find {
        it.permission == Manifest.permission.ACCESS_FINE_LOCATION
    }?.status?.isGranted == true

    val isCoarseGranted = permissionState.permissions.find {
        it.permission == Manifest.permission.ACCESS_COARSE_LOCATION
    }?.status?.isGranted == true

    val isAnyLocationGranted = isFineGranted || isCoarseGranted

    var showPermissionRationaleDialog by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchFeedbackMessage by remember { mutableStateOf<String?>(null) }
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    // Auto-dismiss search feedback message after 3 seconds
    LaunchedEffect(searchFeedbackMessage) {
        if (searchFeedbackMessage != null) {
            kotlinx.coroutines.delay(3000)
            searchFeedbackMessage = null
        }
    }

    // Start background GPS tracking service once permission is granted
    LaunchedEffect(isAnyLocationGranted) {
        if (isAnyLocationGranted) {
            try {
                com.example.service.LocationTrackingService.start(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val filteredServices = remember(services, mapState.visibleCategories, mapState.selectedCategory, mapState.searchQuery) {
        services.filter { service ->
            val matchVisibility = service.category in mapState.visibleCategories
            val matchCategory = mapState.selectedCategory == null || service.category == mapState.selectedCategory
            val matchSearch = mapState.searchQuery.isBlank() ||
                    service.name.contains(mapState.searchQuery, ignoreCase = true) ||
                    service.address.contains(mapState.searchQuery, ignoreCase = true) ||
                    service.servicesOffered.contains(mapState.searchQuery, ignoreCase = true)
            matchVisibility && matchCategory && matchSearch
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Header Greeting & User Profile Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "BIENVENUE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                // GPS Accuracy Badge
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isFineGranted) ZurikaStatusOpen.copy(alpha = 0.15f)
                                    else if (isCoarseGranted) ZurikaAmber.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.clickable {
                                        if (!isFineGranted) {
                                            showPermissionRationaleDialog = true
                                        }
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(
                                                    if (isFineGranted) ZurikaStatusOpen
                                                    else if (isCoarseGranted) ZurikaAmber
                                                    else Color.Gray,
                                                    CircleShape
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isFineGranted) "GPS Précis" else if (isCoarseGranted) "GPS Coarse" else "GPS Inactif",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isFineGranted) ZurikaStatusOpen else if (isCoarseGranted) ZurikaAmber else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Bonjour, ${userName.ifBlank { "Jean" }} 👋",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Action Badges (Pro / Admin / Profile)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                onClick = onOpenProSpace,
                                shape = CircleShape,
                                color = if (isProMode) ZurikaAmber else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Work,
                                        contentDescription = "Espace Pro",
                                        tint = if (isProMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Surface(
                                onClick = onOpenAdminDashboard,
                                shape = CircleShape,
                                color = if (isAdminMode) ZurikaPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AdminPanelSettings,
                                        contentDescription = "Admin",
                                        tint = if (isAdminMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Surface(
                                onClick = onOpenProfile,
                                shape = CircleShape,
                                color = ZurikaPrimaryContainer,
                                border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .size(42.dp)
                                    .testTag("profile_avatar_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = userName.take(2).uppercase().ifBlank { "JD" },
                                        fontWeight = FontWeight.Bold,
                                        color = ZurikaPrimary,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Sleek Search Input Field with Realtime Autocomplete & Map Recentering
                    OutlinedTextField(
                        value = mapState.searchQuery,
                        onValueChange = {
                            onSearchQueryChange(it)
                            isSearchActive = it.isNotBlank()
                        },
                        placeholder = {
                            Text(
                                "Saisir une ville ou une adresse (ex: Lomé, Kpalimé, Déckon...)",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 13.sp
                            )
                        },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = androidx.compose.ui.text.input.ImeAction.Search
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onSearch = {
                                isSearchActive = false
                                keyboardController?.hide()
                                val query = mapState.searchQuery.trim()
                                if (query.isNotBlank()) {
                                    onCommitSearchQuery(query)
                                    coroutineScope.launch {
                                        val resolvedLoc = LocationSearchHelper.resolveLocation(query, context)
                                        if (resolvedLoc != null) {
                                            onRecenterOnLocation(
                                                resolvedLoc.latitude,
                                                resolvedLoc.longitude,
                                                resolvedLoc.name,
                                                resolvedLoc.zoom
                                            )
                                            searchFeedbackMessage = "🎯 Carte recentrée sur : ${resolvedLoc.name}"
                                        } else {
                                            val match = filteredServices.firstOrNull()
                                            if (match != null) {
                                                onSelectService(match.id)
                                                onRecenterOnLocation(match.latitude, match.longitude, match.name, 16f)
                                                searchFeedbackMessage = "🎯 Recentré sur : ${match.name} (${match.address})"
                                            } else {
                                                searchFeedbackMessage = "Aucun lieu ou adresse trouvé pour \"$query\""
                                            }
                                        }
                                    }
                                }
                            }
                        ),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = ZurikaPrimary
                            )
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (mapState.searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            val query = mapState.searchQuery.trim()
                                            isSearchActive = false
                                            keyboardController?.hide()
                                            coroutineScope.launch {
                                                val resolvedLoc = LocationSearchHelper.resolveLocation(query, context)
                                                if (resolvedLoc != null) {
                                                    onRecenterOnLocation(resolvedLoc.latitude, resolvedLoc.longitude, resolvedLoc.name, resolvedLoc.zoom)
                                                    onCommitSearchQuery(resolvedLoc.name)
                                                    searchFeedbackMessage = "🎯 Carte recentrée sur : ${resolvedLoc.name}"
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NearMe,
                                            contentDescription = "Recentrer",
                                            tint = ZurikaAmber
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            onSearchQueryChange("")
                                            isSearchActive = false
                                            onSelectService(null)
                                            searchFeedbackMessage = null
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else if (mapState.mapCenterLat != null) {
                                    IconButton(
                                        onClick = onResetToUserLocation
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MyLocation,
                                            contentDescription = "Revenir à ma position",
                                            tint = ZurikaPrimary
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_bar"),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedBorderColor = ZurikaPrimary,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    // Realtime Location & Service Suggestions Dropdown
                    val locationSuggestions = remember(mapState.searchQuery) {
                        if (mapState.searchQuery.isBlank()) emptyList()
                        else LocationSearchHelper.searchPresets(mapState.searchQuery).take(4)
                    }

                    val searchSuggestions = remember(services, mapState.searchQuery) {
                        if (mapState.searchQuery.isBlank()) emptyList()
                        else {
                            services.filter {
                                it.name.contains(mapState.searchQuery, ignoreCase = true) ||
                                it.address.contains(mapState.searchQuery, ignoreCase = true) ||
                                it.servicesOffered.contains(mapState.searchQuery, ignoreCase = true)
                            }.take(4)
                        }
                    }

                    if (isSearchActive && (locationSuggestions.isNotEmpty() || searchSuggestions.isNotEmpty())) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp,
                            shadowElevation = 8.dp,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_suggestions_dropdown")
                        ) {
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                // Section: VILLES & ADRESSES (Recenter Target)
                                if (locationSuggestions.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "VILLES & ADRESSES (${locationSuggestions.size})",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ZurikaAmber,
                                            letterSpacing = 0.5.sp
                                        )
                                        Text(
                                            text = "Toucher pour recentrer la carte",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    locationSuggestions.forEach { loc ->
                                        Surface(
                                            onClick = {
                                                onRecenterOnLocation(loc.latitude, loc.longitude, loc.name, loc.zoom)
                                                onCommitSearchQuery(loc.name)
                                                isSearchActive = false
                                                keyboardController?.hide()
                                                searchFeedbackMessage = "🎯 Carte recentrée sur : ${loc.name}"
                                            },
                                            color = Color.Transparent,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("location_suggestion_${loc.name}")
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = ZurikaAmber.copy(alpha = 0.18f),
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = if (loc.type == LocationType.CITY) Icons.Default.LocationCity else Icons.Default.Place,
                                                            contentDescription = null,
                                                            tint = ZurikaAmber,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            text = loc.name,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            maxLines = 1
                                                        )
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = ZurikaAmber.copy(alpha = 0.15f)
                                                        ) {
                                                            Text(
                                                                text = when (loc.type) {
                                                                    LocationType.CITY -> "VILLE"
                                                                    LocationType.DISTRICT -> "QUARTIER"
                                                                    LocationType.ADDRESS -> "ADRESSE"
                                                                    LocationType.LANDMARK -> "REPÈRE"
                                                                },
                                                                fontSize = 8.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = ZurikaAmber,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = loc.subtitle,
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = ZurikaPrimary.copy(alpha = 0.12f),
                                                    modifier = Modifier.padding(2.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.NearMe,
                                                            contentDescription = null,
                                                            tint = ZurikaPrimary,
                                                            modifier = Modifier.size(10.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(2.dp))
                                                        Text(
                                                            text = "Recentrer",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = ZurikaPrimary
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 12.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                            thickness = 0.5.dp
                                        )
                                    }
                                }

                                // Section: PROFESSIONNELS & SERVICES
                                if (searchSuggestions.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "PROFESSIONNELS & SERVICES (${searchSuggestions.size})",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ZurikaPrimary,
                                            letterSpacing = 0.5.sp
                                        )
                                        Text(
                                            text = "Toucher pour cibler",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    searchSuggestions.forEach { suggestion ->
                                        val catColor = when (suggestion.category) {
                                            ServiceCategory.STATION -> ZurikaPrimary
                                            ServiceCategory.MECHANIC -> ZurikaAmber
                                            ServiceCategory.VULCANIZER -> Color(0xFF8B5CF6)
                                            ServiceCategory.TOWING -> Color(0xFFEF4444)
                                        }
                                        val catIcon = when (suggestion.category) {
                                            ServiceCategory.STATION -> Icons.Default.LocalGasStation
                                            ServiceCategory.MECHANIC -> Icons.Default.Build
                                            ServiceCategory.VULCANIZER -> Icons.Default.TwoWheeler
                                            ServiceCategory.TOWING -> Icons.Default.Warning
                                        }
                                        val isOpen = suggestion.status == ServiceStatus.OPEN
                                        val distanceKm = ZurikaRepository.calculateDistanceKm(
                                            mapState.userLat,
                                            mapState.userLng,
                                            suggestion.latitude,
                                            suggestion.longitude
                                        )

                                        Surface(
                                            onClick = {
                                                onCommitSearchQuery(suggestion.name)
                                                onSelectService(suggestion.id)
                                                onRecenterOnLocation(suggestion.latitude, suggestion.longitude, suggestion.name, 16f)
                                                isSearchActive = false
                                                keyboardController?.hide()
                                                searchFeedbackMessage = "🎯 Recentré sur : ${suggestion.name} (${suggestion.address})"
                                            },
                                            color = Color.Transparent,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("search_suggestion_${suggestion.id}")
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = catColor.copy(alpha = 0.18f),
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = catIcon,
                                                            contentDescription = null,
                                                            tint = catColor,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            text = suggestion.name,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            maxLines = 1
                                                        )
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = if (isOpen) ZurikaStatusOpen.copy(alpha = 0.15f) else ZurikaStatusClosed.copy(alpha = 0.15f)
                                                        ) {
                                                            Text(
                                                                text = if (isOpen) "OUVERT" else "FERMÉ",
                                                                fontSize = 8.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isOpen) ZurikaStatusOpen else ZurikaStatusClosed,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.LocationOn,
                                                            contentDescription = null,
                                                            tint = ZurikaAmber,
                                                            modifier = Modifier.size(11.dp)
                                                        )
                                                        Text(
                                                            text = suggestion.address,
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        text = String.format("%.1f km", distanceKm),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = ZurikaPrimary
                                                    )
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Default.Star,
                                                            contentDescription = null,
                                                            tint = ZurikaAmber,
                                                            modifier = Modifier.size(10.dp)
                                                        )
                                                        Text(
                                                            text = String.format("%.1f", suggestion.rating),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        if (suggestion != searchSuggestions.last()) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 12.dp),
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                                thickness = 0.5.dp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Quick City & District Chips for Instant 1-Tap Recentering
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quick_cities_scroll_row")
                    ) {
                        val quickLocations = listOf(
                            Triple("Lomé", 6.1375 to 1.2125, 13.5f),
                            Triple("Kpalimé", 6.9070 to 0.6280, 13.5f),
                            Triple("Kara", 9.5510 to 1.1860, 13.5f),
                            Triple("Sokodé", 8.9830 to 1.1330, 13.5f),
                            Triple("Atakpamé", 7.5330 to 1.1330, 13.5f),
                            Triple("Déckon", 6.1310 to 1.2180, 15.5f),
                            Triple("Agoè-Nyivé", 6.2080 to 1.1950, 15.0f),
                            Triple("Tokoin", 6.1455 to 1.2188, 15.5f),
                            Triple("Bè", 6.1320 to 1.2420, 15.5f)
                        )
                        items(quickLocations) { (name, coords, zoom) ->
                            val isSelected = mapState.mapCenterLabel == name
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    onRecenterOnLocation(coords.first, coords.second, name, zoom)
                                    onCommitSearchQuery(name)
                                    searchFeedbackMessage = "🎯 Carte recentrée sur : $name"
                                },
                                label = {
                                    Text(
                                        text = name,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (name in listOf("Déckon", "Agoè-Nyivé", "Tokoin", "Bè")) Icons.Default.Place else Icons.Default.LocationCity,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = if (isSelected) Color.White else ZurikaPrimary
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ZurikaPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                    labelColor = MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("quick_city_chip_$name")
                            )
                        }
                    }

                    // Active Recenter Status Indicator Banner
                    AnimatedVisibility(
                        visible = mapState.mapCenterLat != null,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ZurikaAmber.copy(alpha = 0.16f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ZurikaAmber.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("active_recenter_indicator")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NearMe,
                                            contentDescription = null,
                                            tint = ZurikaAmber,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Carte centrée sur : ${mapState.mapCenterLabel ?: "Lieu personnalisé"}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                    }
                                    TextButton(
                                        onClick = onResetToUserLocation,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier
                                            .height(28.dp)
                                            .testTag("reset_to_gps_header_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MyLocation,
                                            contentDescription = null,
                                            tint = ZurikaPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Ma position",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ZurikaPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Recent Searches Chips (Cached via Room)
                    if (recentSearches.isNotEmpty() && mapState.searchQuery.isEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "History",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Dernières recherches (Cache Room) :",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(recentSearches, key = { it.id }) { searchItem ->
                                SuggestionChip(
                                    onClick = { onCommitSearchQuery(searchItem.query) },
                                    label = { Text(searchItem.query, fontSize = 11.sp) },
                                    icon = {
                                        Icon(
                                            Icons.Default.History,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Location Permission Prompt / Status Banner
                    if (!isFineGranted) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCoarseGranted) ZurikaAmber.copy(alpha = 0.15f) else ZurikaPrimary.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isCoarseGranted) ZurikaAmber else ZurikaPrimary.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPermissionRationaleDialog = true }
                                .testTag("location_permission_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isCoarseGranted) Icons.Default.GpsNotFixed else Icons.Default.LocationOff,
                                        contentDescription = "Permission status",
                                        tint = if (isCoarseGranted) ZurikaAmber else ZurikaPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isCoarseGranted)
                                            "Position approximative : activez le GPS précis pour un guidage exact."
                                        else
                                            "Activez la localisation fine pour géolocaliser les services au Togo.",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (permissionState.shouldShowRationale) {
                                            showPermissionRationaleDialog = true
                                        } else {
                                            permissionState.launchMultiplePermissionRequest()
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isCoarseGranted) ZurikaAmber else ZurikaPrimary,
                                        contentColor = if (isCoarseGranted) DarkBackground else Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(text = if (isCoarseGranted) "Préciser" else "Activer", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Sleek Horizontal Category Navigation Bar with icons & labels
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        contentPadding = PaddingValues(horizontal = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("category_navigation_bar")
                    ) {
                        item {
                            FilterCategoryChip(
                                label = "Tous",
                                icon = Icons.Default.Apps,
                                isSelected = mapState.selectedCategory == null,
                                testTag = "category_chip_all",
                                onClick = { onSelectCategoryFilter(null) }
                            )
                        }
                        item {
                            FilterCategoryChip(
                                label = "Stations",
                                icon = Icons.Default.LocalGasStation,
                                isSelected = mapState.selectedCategory == ServiceCategory.STATION,
                                testTag = "category_chip_station",
                                onClick = { onSelectCategoryFilter(if (mapState.selectedCategory == ServiceCategory.STATION) null else ServiceCategory.STATION) }
                            )
                        }
                        item {
                            FilterCategoryChip(
                                label = "Mécaniciens",
                                icon = Icons.Default.Build,
                                isSelected = mapState.selectedCategory == ServiceCategory.MECHANIC,
                                testTag = "category_chip_mechanic",
                                onClick = { onSelectCategoryFilter(if (mapState.selectedCategory == ServiceCategory.MECHANIC) null else ServiceCategory.MECHANIC) }
                            )
                        }
                        item {
                            FilterCategoryChip(
                                label = "Vulcanisateurs",
                                icon = Icons.Default.TwoWheeler,
                                isSelected = mapState.selectedCategory == ServiceCategory.VULCANIZER,
                                testTag = "category_chip_vulcanizer",
                                onClick = { onSelectCategoryFilter(if (mapState.selectedCategory == ServiceCategory.VULCANIZER) null else ServiceCategory.VULCANIZER) }
                            )
                        }
                        item {
                            FilterCategoryChip(
                                label = "Dépannage",
                                icon = Icons.Default.Warning,
                                isSelected = mapState.selectedCategory == ServiceCategory.TOWING,
                                testTag = "category_chip_towing",
                                onClick = { onSelectCategoryFilter(if (mapState.selectedCategory == ServiceCategory.TOWING) null else ServiceCategory.TOWING) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action Bar: "📍 TROUVER LE PLUS PROCHE" + View Switcher
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onFindNearest,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("find_nearest_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ZurikaPrimary,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(imageVector = Icons.Default.NearMe, contentDescription = "Nearest", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TROUVER LE PLUS PROCHE",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        // Map vs List Toggle Button
                        IconButton(
                            onClick = onToggleListView,
                            modifier = Modifier
                                .size(46.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                                .testTag("toggle_list_view_button")
                        ) {
                            Icon(
                                imageVector = if (mapState.isListView) Icons.Default.Map else Icons.Default.FormatListBulleted,
                                contentDescription = "Toggle view",
                                tint = ZurikaPrimary
                            )
                        }
                    }
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            // Emergency Assistance Dispatch Floating Button (shown when no service detail popup is active)
            if (mapState.selectedServiceId == null) {
                ExtendedFloatingActionButton(
                    onClick = onOpenInterventionDispatch,
                    containerColor = ZurikaStatusClosed,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                    modifier = Modifier.testTag("emergency_dispatch_fab")
                ) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = "SOS")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "DEMANDER INTERVENTION", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (mapState.isListView) {
                // List View mode
                ServiceListView(
                    services = filteredServices,
                    userLat = mapState.userLat,
                    userLng = mapState.userLng,
                    onOpenServiceDetail = onOpenServiceDetail
                )
            } else {
                // Interactive Map Mode
                ZurikaMapCanvas(
                    services = filteredServices,
                    allServices = services,
                    selectedServiceId = mapState.selectedServiceId,
                    userLat = mapState.userLat,
                    userLng = mapState.userLng,
                    onSelectService = onSelectService,
                    onOpenDetail = onOpenServiceDetail,
                    visibleCategories = mapState.visibleCategories,
                    onToggleCategoryVisibility = onToggleCategoryVisibility,
                    onSetAllCategoriesVisible = onSetAllCategoriesVisible,
                    isMyLocationGranted = isAnyLocationGranted,
                    mapCenterLat = mapState.mapCenterLat,
                    mapCenterLng = mapState.mapCenterLng,
                    mapCenterLabel = mapState.mapCenterLabel,
                    mapCenterZoom = mapState.mapCenterZoom,
                    recenterEventId = mapState.recenterEventId,
                    onRecenterOnLocation = onRecenterOnLocation,
                    onResetToUserLocation = onResetToUserLocation,
                    isLoading = mapState.isLoading,
                    onRefresh = onRefreshDepanneurs,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Floating Active Search Result Navigation Bar on top of map
            if (mapState.searchQuery.isNotBlank() && !mapState.isListView) {
                val currentMatchIndex = filteredServices.indexOfFirst { it.id == mapState.selectedServiceId }
                val displayIndex = if (currentMatchIndex >= 0) currentMatchIndex else 0

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = DarkSurface.copy(alpha = 0.94f),
                    contentColor = Color.White,
                    shadowElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, ZurikaPrimary.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp, start = 16.dp, end = 16.dp)
                        .testTag("search_results_navigator_bar")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = ZurikaPrimary.copy(alpha = 0.25f),
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = ZurikaPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = if (filteredServices.isEmpty()) "Aucun résultat" else "${filteredServices.size} pro(s) trouvé(s)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "\"${mapState.searchQuery}\"",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.6f),
                                maxLines = 1
                            )
                        }

                        if (filteredServices.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        val prevIndex = if (displayIndex > 0) displayIndex - 1 else filteredServices.lastIndex
                                        val target = filteredServices[prevIndex]
                                        onSelectService(target.id)
                                        searchFeedbackMessage = "📍 ${prevIndex + 1}/${filteredServices.size} : ${target.name} (${target.address})"
                                    },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .testTag("search_prev_result_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronLeft,
                                        contentDescription = "Précédent",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "${displayIndex + 1}/${filteredServices.size}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ZurikaAmber,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val nextIndex = (displayIndex + 1) % filteredServices.size
                                        val target = filteredServices[nextIndex]
                                        onSelectService(target.id)
                                        searchFeedbackMessage = "📍 ${nextIndex + 1}/${filteredServices.size} : ${target.name} (${target.address})"
                                    },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .testTag("search_next_result_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Suivant",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = {
                                onSearchQueryChange("")
                                onSelectService(null)
                                searchFeedbackMessage = null
                            },
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("clear_search_bar_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Effacer",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Animated Camera Recenter Feedback Notification Toast
            AnimatedVisibility(
                visible = searchFeedbackMessage != null,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = if (mapState.searchQuery.isNotBlank() && !mapState.isListView) 64.dp else 12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ZurikaPrimary,
                    contentColor = Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .testTag("search_recenter_toast")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = searchFeedbackMessage.orEmpty(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Location Permission Rationale / Settings Dialog
        if (showPermissionRationaleDialog) {
            AlertDialog(
                onDismissRequest = { showPermissionRationaleDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "GPS",
                        tint = ZurikaAmber,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        text = "Localisation GPS Haute Précision",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "ZURIKA a besoin de la localisation précise (Fine Location) pour :",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = ZurikaStatusOpen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Calculer la distance exacte aux stations et garages", fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = ZurikaStatusOpen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Transmettre votre position GPS aux dépanneurs SOS", fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = ZurikaStatusOpen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Positionner votre véhicule en temps réel sur la carte", fontSize = 12.sp)
                        }

                        if (!permissionState.shouldShowRationale && !isAnyLocationGranted) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Si la demande n'apparaît pas, veuillez autoriser la localisation dans les Paramètres système de l'application.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showPermissionRationaleDialog = false
                            if (!permissionState.shouldShowRationale && !isAnyLocationGranted) {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            } else {
                                permissionState.launchMultiplePermissionRequest()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ZurikaAmber, contentColor = DarkBackground),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (!permissionState.shouldShowRationale && !isAnyLocationGranted) "Ouvrir les Paramètres" else "Autoriser le GPS",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPermissionRationaleDialog = false }) {
                        Text("Fermer", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.surface
            )
        }
    }
}

@Composable
private fun FilterCategoryChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    isSelected: Boolean,
    testTag: String = "",
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) ZurikaPrimary else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) ZurikaPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        shadowElevation = if (isSelected) 3.dp else 0.dp,
        modifier = Modifier
            .height(40.dp)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isSelected) Color.White else ZurikaPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ServiceListView(
    services: List<ServiceEntity>,
    userLat: Double,
    userLng: Double,
    onOpenServiceDetail: (ServiceEntity) -> Unit
) {
    if (services.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.SearchOff,
                    contentDescription = "Empty",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Aucun service trouvé",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    } else {
        val sortedList = remember(services, userLat, userLng) {
            services.sortedBy {
                ZurikaRepository.calculateDistanceKm(userLat, userLng, it.latitude, it.longitude)
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(sortedList, key = { it.id }) { service ->
                val distanceKm = ZurikaRepository.calculateDistanceKm(
                    userLat, userLng, service.latitude, service.longitude
                )

                ServiceCardItem(
                    service = service,
                    distanceKm = distanceKm,
                    onClick = { onOpenServiceDetail(service) }
                )
            }
        }
    }
}

@Composable
fun ServiceCardItem(
    service: ServiceEntity,
    distanceKm: Double,
    onClick: () -> Unit
) {
    val statusColor = when (service.status) {
        ServiceStatus.OPEN -> ZurikaStatusOpen
        ServiceStatus.BUSY -> ZurikaStatusBusy
        ServiceStatus.CLOSED -> ZurikaStatusClosed
        ServiceStatus.TEMP_UNAVAILABLE -> ZurikaStatusBusy
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
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .sharedServiceCard(service.id, RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon Avatar
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = statusColor.copy(alpha = 0.12f),
                modifier = Modifier
                    .size(56.dp)
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

            Spacer(modifier = Modifier.width(14.dp))

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

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.sharedServiceRating(service.id)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = ZurikaAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${service.rating}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = " (${service.reviewCount} avis)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
