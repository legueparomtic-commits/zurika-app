package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
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
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ProfileTab {
    FAVORITES,
    INTERVENTIONS,
    SEARCHES
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ProfileSettingsScreen(
    user: UserEntity?,
    favoriteServices: List<ServiceEntity>,
    interventions: List<InterventionEntity> = emptyList(),
    recentSearches: List<SearchHistoryEntity> = emptyList(),
    currentLanguage: com.example.util.AppLanguage = com.example.util.AppLanguage.FRENCH,
    onLanguageSelected: (com.example.util.AppLanguage) -> Unit = {},
    onReplayOnboarding: () -> Unit = {},
    onOpenServiceDetail: (ServiceEntity) -> Unit,
    onSelectSearchQuery: (String) -> Unit = {},
    onDeleteSearchQuery: (String) -> Unit = {},
    onClearSearchHistory: () -> Unit = {},
    onDeleteIntervention: (String) -> Unit = {},
    onClearInterventions: () -> Unit = {},
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
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

    var selectedTab by remember { mutableStateOf(ProfileTab.INTERVENTIONS) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var selectedInterventionForDetail by remember { mutableStateOf<InterventionEntity?>(null) }
    val strings = remember(currentLanguage) { com.example.util.getAppStrings(currentLanguage) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("profile_settings_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Top Bar Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.profileTitle,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // User Profile Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, ZurikaPrimary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = ZurikaPrimary.copy(alpha = 0.2f),
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = "Avatar", tint = ZurikaPrimary, modifier = Modifier.size(32.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user?.fullName?.ifBlank { "Utilisateur ZURIKA" } ?: "Conducteur ZURIKA",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = user?.phoneNumber?.ifBlank { "+228 90 00 11 22" } ?: "+228 90 XX XX XX",
                            fontSize = 13.sp,
                            color = ZurikaAmber,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SdCard, contentDescription = null, tint = ZurikaStatusOpen, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = strings.offlineCacheActive,
                                fontSize = 11.sp,
                                color = ZurikaStatusOpen
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Multilingual Language Selector Card (Français / English)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Language",
                                tint = ZurikaPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.languageSectionTitle,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ZurikaAmber.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = currentLanguage.flag + " " + currentLanguage.displayName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ZurikaAmber,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = strings.languageSectionSubtitle,
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        com.example.util.AppLanguage.values().forEach { lang ->
                            val isSelected = lang == currentLanguage
                            Surface(
                                onClick = { onLanguageSelected(lang) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) ZurikaPrimary.copy(alpha = 0.25f) else DarkSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) ZurikaPrimary else Color.Transparent
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag(if (lang == com.example.util.AppLanguage.FRENCH) "language_french_button" else "language_english_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = lang.flag,
                                        fontSize = 18.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = lang.displayName,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else Color.LightGray
                                        )
                                        Text(
                                            text = if (lang == com.example.util.AppLanguage.FRENCH) "Togo 🇹🇬" else "Global 🌍",
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Location & GPS Settings Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_gps_settings_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "GPS",
                                tint = ZurikaAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "GÉOLOCALISATION & GPS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ZurikaAmber,
                                letterSpacing = 1.sp
                            )
                        }

                        // Status Badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isFineGranted) ZurikaStatusOpen.copy(alpha = 0.2f)
                            else if (isCoarseGranted) ZurikaAmber.copy(alpha = 0.2f)
                            else Color.Red.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = if (isFineGranted) "Haute Précision" else if (isCoarseGranted) "Approximatif" else "Désactivé",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isFineGranted) ZurikaStatusOpen else if (isCoarseGranted) ZurikaAmber else Color.Red,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isFineGranted)
                            "La localisation fine (Fine Location) est active. Vos calculs d'itinéraires et demandes de dépannage à Lomé bénéficient d'une précision maximale."
                        else if (isCoarseGranted)
                            "Seule la localisation approximative est accordée. Autorisez le GPS précis pour un positionnement exact de votre véhicule."
                        else
                            "La géolocalisation est désactivée. Vos recherches utilisent les coordonnées par défaut de Lomé.",
                        fontSize = 12.sp,
                        color = Color.LightGray,
                        lineHeight = 16.sp
                    )

                    if (!isFineGranted) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (permissionState.shouldShowRationale) {
                                    permissionState.launchMultiplePermissionRequest()
                                } else {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ZurikaAmber,
                                contentColor = DarkBackground
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("enable_fine_location_button")
                        ) {
                            Icon(imageVector = Icons.Default.GpsFixed, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isCoarseGranted) "Passer au GPS Haute Précision" else "Autoriser la Localisation Fine",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Navigation Tabs (Historique dépannage, Dernières recherches, Favoris)
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = DarkSurface,
                contentColor = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == ProfileTab.INTERVENTIONS,
                    onClick = { selectedTab = ProfileTab.INTERVENTIONS },
                    text = {
                        Text(
                            "${strings.tabInterventions} (${interventions.size})",
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == ProfileTab.INTERVENTIONS) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    icon = { Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = selectedTab == ProfileTab.SEARCHES,
                    onClick = { selectedTab = ProfileTab.SEARCHES },
                    text = {
                        Text(
                            "${strings.tabSearches} (${recentSearches.size})",
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == ProfileTab.SEARCHES) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = selectedTab == ProfileTab.FAVORITES,
                    onClick = { selectedTab = ProfileTab.FAVORITES },
                    text = {
                        Text(
                            "${strings.tabFavorites} (${favoriteServices.size})",
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == ProfileTab.FAVORITES) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content Area based on Selected Tab
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    ProfileTab.INTERVENTIONS -> {
                        InterventionsHistorySection(
                            interventions = interventions,
                            onSelectIntervention = { selectedInterventionForDetail = it },
                            onDeleteIntervention = onDeleteIntervention,
                            onClearAll = onClearInterventions,
                            strings = strings
                        )
                    }
                    ProfileTab.SEARCHES -> {
                        RecentSearchesSection(
                            searches = recentSearches,
                            onSelectSearch = onSelectSearchQuery,
                            onDeleteSearch = onDeleteSearchQuery,
                            onClearAll = onClearSearchHistory,
                            strings = strings
                        )
                    }
                    ProfileTab.FAVORITES -> {
                        FavoritesSection(
                            favoriteServices = favoriteServices,
                            onOpenServiceDetail = onOpenServiceDetail,
                            strings = strings
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Logout & Delete Account Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onLogout,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("logout_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant)
                ) {
                    Icon(imageVector = Icons.Default.Logout, contentDescription = "Logout", tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = strings.logout, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                }

                Button(
                    onClick = { showDeleteConfirmation = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("delete_account_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ZurikaStatusClosed.copy(alpha = 0.2f))
                ) {
                    Icon(imageVector = Icons.Default.DeleteForever, contentDescription = "Delete", tint = ZurikaStatusClosed, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = strings.deleteAccount, fontWeight = FontWeight.Bold, color = ZurikaStatusClosed, fontSize = 13.sp)
                }
            }
        }

        // Intervention Detail Modal Dialog
        if (selectedInterventionForDetail != null) {
            val intervention = selectedInterventionForDetail!!
            AlertDialog(
                onDismissRequest = { selectedInterventionForDetail = null },
                containerColor = DarkSurface,
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Détail de l'intervention",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ZurikaStatusOpen.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Cache Room",
                                fontSize = 10.sp,
                                color = ZurikaStatusOpen,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DarkSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = intervention.serviceName ?: "Station / Dépannage ZURIKA",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Panne : ${intervention.problemType.name.replace("_", " ")}",
                                    fontSize = 13.sp,
                                    color = ZurikaAmber,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Text(
                            text = "Description :",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.LightGray
                        )
                        Text(
                            text = intervention.description.ifBlank { "Aucune description complémentaire." },
                            fontSize = 13.sp,
                            color = Color.White
                        )

                        Divider(color = Color.White.copy(alpha = 0.1f))

                        val sdf = SimpleDateFormat("dd MMMM yyyy à HH:mm", Locale.FRENCH)
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Date :", fontSize = 12.sp, color = Color.Gray)
                            Text(text = sdf.format(Date(intervention.timestamp)), fontSize = 12.sp, color = Color.White)
                        }

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Statut :", fontSize = 12.sp, color = Color.Gray)
                            Text(
                                text = intervention.status.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (intervention.status) {
                                    RequestStatus.PENDING -> ZurikaAmber
                                    RequestStatus.ACCEPTED -> ZurikaPrimary
                                    RequestStatus.EN_ROUTE -> ZurikaStatusBusy
                                    RequestStatus.COMPLETED -> ZurikaStatusOpen
                                    RequestStatus.CANCELLED -> ZurikaStatusClosed
                                }
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Coordonnées GPS :", fontSize = 12.sp, color = Color.Gray)
                            Text(
                                text = String.format(Locale.US, "%.5f, %.5f", intervention.latitude, intervention.longitude),
                                fontSize = 12.sp,
                                color = ZurikaPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                onDeleteIntervention(intervention.id)
                                selectedInterventionForDetail = null
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = ZurikaStatusClosed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Supprimer", color = ZurikaStatusClosed)
                        }

                        Button(
                            onClick = { selectedInterventionForDetail = null },
                            colors = ButtonDefaults.buttonColors(containerColor = ZurikaPrimary)
                        ) {
                            Text("Fermer", color = DarkBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }

        // Delete confirmation modal dialog
        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text(strings.confirmDeleteTitle, fontWeight = FontWeight.Bold, color = ZurikaStatusClosed) },
                text = {
                    Text(
                        text = strings.confirmDeleteMessage,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteConfirmation = false
                            onDeleteAccount()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ZurikaStatusClosed)
                    ) {
                        Text(strings.confirmButton, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text(strings.cancelButton)
                    }
                }
            )
        }
    }
}

@Composable
private fun InterventionsHistorySection(
    interventions: List<InterventionEntity>,
    onSelectIntervention: (InterventionEntity) -> Unit,
    onDeleteIntervention: (String) -> Unit,
    onClearAll: () -> Unit,
    strings: com.example.util.AppStrings
) {
    if (interventions.isEmpty()) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = DarkSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Engineering,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = strings.noInterventions,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Vos demandes de dépannage sont automatiquement sauvegardées dans la base de données Room et restent consultables sans réseau.",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ZurikaPrimary.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ZurikaPrimary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = ZurikaPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Base Room SQLite : ${interventions.size} intervention(s) hors-ligne",
                                fontSize = 11.sp,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        TextButton(
                            onClick = onClearAll,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(strings.clearAll, fontSize = 11.sp, color = ZurikaStatusClosed)
                        }
                    }
                }
            }

            items(interventions, key = { it.id }) { item ->
                Surface(
                    onClick = { onSelectIntervention(item) },
                    shape = RoundedCornerShape(14.dp),
                    color = DarkSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = ZurikaPrimary.copy(alpha = 0.2f),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = when (item.problemType) {
                                                ProblemType.PANNE_SECHE -> Icons.Default.LocalGasStation
                                                ProblemType.CREVAISON -> Icons.Default.DirectionsCar
                                                ProblemType.MOTEUR -> Icons.Default.Build
                                                ProblemType.BATTERIE -> Icons.Default.BatteryAlert
                                                ProblemType.REMORQUAGE -> Icons.Default.Agriculture
                                                ProblemType.AUTRE -> Icons.Default.Warning
                                            },
                                            contentDescription = null,
                                            tint = ZurikaPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = item.serviceName ?: "Station / Dépannage Général",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Panne : ${item.problemType.name.replace("_", " ")}",
                                        fontSize = 12.sp,
                                        color = ZurikaAmber
                                    )
                                }
                            }

                            val (statusText, statusBg, statusFg) = when (item.status) {
                                RequestStatus.PENDING -> Triple(strings.statusPending, ZurikaAmber.copy(alpha = 0.2f), ZurikaAmber)
                                RequestStatus.ACCEPTED -> Triple(strings.statusAccepted, ZurikaPrimary.copy(alpha = 0.2f), ZurikaPrimary)
                                RequestStatus.EN_ROUTE -> Triple(strings.statusEnRoute, ZurikaStatusBusy.copy(alpha = 0.2f), ZurikaStatusBusy)
                                RequestStatus.COMPLETED -> Triple(strings.statusCompleted, ZurikaStatusOpen.copy(alpha = 0.2f), ZurikaStatusOpen)
                                RequestStatus.CANCELLED -> Triple(strings.statusCancelled, ZurikaStatusClosed.copy(alpha = 0.2f), ZurikaStatusClosed)
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = statusBg
                            ) {
                                Text(
                                    text = statusText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusFg,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = item.description,
                            fontSize = 12.sp,
                            color = Color.LightGray
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                            Text(
                                text = "Date : ${sdf.format(Date(item.timestamp))}",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "GPS : ${String.format(Locale.US, "%.4f, %.4f", item.latitude, item.longitude)}",
                                    fontSize = 10.sp,
                                    color = ZurikaPrimary
                                )
                                IconButton(
                                    onClick = { onDeleteIntervention(item.id) },
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(14.dp)
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

@Composable
private fun RecentSearchesSection(
    searches: List<SearchHistoryEntity>,
    onSelectSearch: (String) -> Unit,
    onDeleteSearch: (String) -> Unit,
    onClearAll: () -> Unit,
    strings: com.example.util.AppStrings
) {
    if (searches.isEmpty()) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = DarkSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.SearchOff,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = strings.noSearches,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Vos recherches récentes sont enregistrées localement avec Room pour y accéder instantanément, même sans connexion.",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ZurikaAmber.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, ZurikaAmber.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = ZurikaAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Cache Room : ${searches.size} recherche(s) récentes",
                            fontSize = 11.sp,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    TextButton(
                        onClick = onClearAll,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(strings.clearAll, fontSize = 11.sp, color = ZurikaStatusClosed)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(searches, key = { it.id }) { item ->
                    Surface(
                        onClick = { onSelectSearch(item.query) },
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.History, contentDescription = null, tint = ZurikaPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = item.query, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                    val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                                    Text(text = sdf.format(Date(item.timestamp)), fontSize = 10.sp, color = Color.Gray)
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = ZurikaPrimary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "Rechercher",
                                        color = ZurikaPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { onDeleteSearch(item.query) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoritesSection(
    favoriteServices: List<ServiceEntity>,
    onOpenServiceDetail: (ServiceEntity) -> Unit,
    strings: com.example.util.AppStrings
) {
    if (favoriteServices.isEmpty()) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = DarkSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = strings.noFavorites,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(favoriteServices, key = { it.id }) { service ->
                ServiceCardItem(
                    service = service,
                    distanceKm = 1.2,
                    onClick = { onOpenServiceDetail(service) }
                )
            }
        }
    }
}
