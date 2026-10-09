package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.*
import com.example.ui.animation.*
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ServiceDetailSheet(
    service: ServiceEntity,
    reviews: List<ReviewEntity>,
    userLat: Double,
    userLng: Double,
    onToggleFavorite: (Boolean) -> Unit,
    onRequestIntervention: () -> Unit,
    onAddReview: (Float, String) -> Unit,
    onSubmitReport: (String, String, (Boolean) -> Unit) -> Unit = { _, _, cb -> cb(true) },
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Real-time Firestore Service Document Rating Fetch
    val firestoreRatingData by remember(service.id) {
        FirebaseManager.listenToServiceRating(service.id)
    }.collectAsState(initial = null)

    val effectiveRating = firestoreRatingData?.rating ?: service.rating
    val effectiveReviewCount = firestoreRatingData?.reviewCount ?: reviews.size.coerceAtLeast(service.reviewCount)
    val isLiveFromFirestore = firestoreRatingData != null

    val distanceKm = remember(service, userLat, userLng) {
        ZurikaRepository.calculateDistanceKm(userLat, userLng, service.latitude, service.longitude)
    }

    val statusColor = when (service.status) {
        ServiceStatus.OPEN -> ZurikaStatusOpen
        ServiceStatus.BUSY -> ZurikaStatusBusy
        ServiceStatus.CLOSED -> ZurikaStatusClosed
        ServiceStatus.TEMP_UNAVAILABLE -> ZurikaStatusBusy
        ServiceStatus.UNKNOWN -> ZurikaStatusUnknown
    }

    val statusText = when (service.status) {
        ServiceStatus.OPEN -> "Ouvert"
        ServiceStatus.BUSY -> "Occupé"
        ServiceStatus.CLOSED -> "Fermé"
        ServiceStatus.TEMP_UNAVAILABLE -> "Temporairement indisponible"
        ServiceStatus.UNKNOWN -> "Statut inconnu"
    }

    var showReviewInput by remember { mutableStateOf(false) }
    var reviewRating by remember { mutableStateOf(5f) }
    var reviewComment by remember { mutableStateOf("") }

    // Report Error Dialog State
    var showReportDialog by remember { mutableStateOf(false) }
    var reportErrorType by remember { mutableStateOf("Numéro de téléphone erroné / injoignable") }
    var reportDescription by remember { mutableStateOf("") }
    var isSubmittingReport by remember { mutableStateOf(false) }

    // Drawable banner resource resolver
    val bannerDrawable = when (service.category) {
        ServiceCategory.STATION -> R.drawable.img_station_banner_1786356166469
        ServiceCategory.MECHANIC -> R.drawable.img_mechanic_banner_1786356179978
        ServiceCategory.VULCANIZER -> R.drawable.img_mechanic_banner_1786356179978
        ServiceCategory.TOWING -> R.drawable.img_towing_banner_1786356193954
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .sharedServiceCard(service.id, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .testTag("service_detail_sheet")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Hero Banner Image Header
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .sharedServiceHero(service.id, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                ) {
                    Image(
                        painter = painterResource(id = bannerDrawable),
                        contentDescription = service.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.5f),
                                        Color.Transparent,
                                        DarkBackground
                                    )
                                )
                            )
                    )

                    // Drag Handle Indicator
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 10.dp)
                            .size(width = 38.dp, height = 4.dp)
                            .background(Color.White.copy(alpha = 0.45f), CircleShape)
                    )

                    // Top Bar overlay inside header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = DarkBackground.copy(alpha = 0.7f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Report Error Top Bar Action
                            Surface(
                                shape = CircleShape,
                                color = DarkBackground.copy(alpha = 0.7f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                IconButton(
                                    onClick = { showReportDialog = true },
                                    modifier = Modifier.testTag("top_report_error_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ReportProblem,
                                        contentDescription = "Signaler une erreur",
                                        tint = ZurikaAmber
                                    )
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = DarkBackground.copy(alpha = 0.7f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                IconButton(onClick = { onToggleFavorite(service.isFavorite) }) {
                                    Icon(
                                        imageVector = if (service.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Favorite",
                                        tint = if (service.isFavorite) ZurikaStatusClosed else Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Main Details
            item {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = service.name,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .weight(1f)
                                .sharedServiceTitle(service.id)
                        )

                        if (service.isVerifiedPro) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = ZurikaPrimary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ZurikaPrimary)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.Verified, contentDescription = "Verified", tint = ZurikaPrimary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Pro Vérifié", fontSize = 11.sp, color = ZurikaPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Status & Distance Row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = statusColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
                            modifier = Modifier.sharedServiceStatus(service.id)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(statusColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = statusText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Live Firestore Sync Indicator Pill
                        Surface(
                            color = ZurikaPrimary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ZurikaPrimary.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = "Firestore Live",
                                    tint = ZurikaPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Firestore Live",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ZurikaPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Icon(imageVector = Icons.Default.NearMe, contentDescription = "Distance", tint = ZurikaPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "À %.1f km".format(distanceKm), fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.SemiBold)

                        Spacer(modifier = Modifier.width(12.dp))

                        // Live Star-Rating Pill in Header
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = ZurikaAmber.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ZurikaAmber.copy(alpha = 0.35f)),
                            modifier = Modifier.sharedServiceRating(service.id)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Star, contentDescription = "Rating", tint = ZurikaAmber, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "%.1f (%d)".format(effectiveRating, effectiveReviewCount),
                                    fontSize = 13.sp,
                                    color = ZurikaAmber,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Primary Actions Grid: Appeler, Y aller / Itinéraire, Demander Intervention
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .sharedServiceActions(service.id),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${service.phoneNumber}"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("detail_call_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ZurikaStatusOpen)
                        ) {
                            Icon(imageVector = Icons.Default.Call, contentDescription = "Call")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Appeler", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                try {
                                    val gmmIntentUri = Uri.parse("google.navigation:q=${service.latitude},${service.longitude}&mode=d")
                                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                                        setPackage("com.google.android.apps.maps")
                                    }
                                    if (mapIntent.resolveActivity(context.packageManager) != null) {
                                        context.startActivity(mapIntent)
                                    } else {
                                        val geoIntent = Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("geo:${service.latitude},${service.longitude}?q=${service.latitude},${service.longitude}(${Uri.encode(service.name)})")
                                        )
                                        if (geoIntent.resolveActivity(context.packageManager) != null) {
                                            context.startActivity(geoIntent)
                                        } else {
                                            val webIntent = Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${service.latitude},${service.longitude}")
                                            )
                                            context.startActivity(webIntent)
                                        }
                                    }
                                } catch (e: Exception) {
                                    val webIntent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${service.latitude},${service.longitude}")
                                    )
                                    context.startActivity(webIntent)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("detail_route_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ZurikaPrimary)
                        ) {
                            Icon(imageVector = Icons.Default.Directions, contentDescription = "Itinéraire")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Itinéraire", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Intervention Request button for pro services
                    Button(
                        onClick = onRequestIntervention,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("detail_intervention_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ZurikaAmber, contentColor = DarkBackground)
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = "Intervention")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Demander une intervention", fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Information Cards Block
                    Text(text = "Informations pratiques", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)

                    Spacer(modifier = Modifier.height(12.dp))

                    InfoRowItem(icon = Icons.Default.Place, label = "Adresse", value = service.address)
                    InfoRowItem(icon = Icons.Default.Phone, label = "Téléphone", value = service.phoneNumber)
                    InfoRowItem(icon = Icons.Default.Schedule, label = "Horaires d'ouverture", value = service.openingHours)
                    InfoRowItem(icon = Icons.Default.Sell, label = "Tarification / Prix", value = service.priceInfo)

                    Spacer(modifier = Modifier.height(24.dp))

                    // Services Offered Tags
                    Text(text = "Services & Équipements", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)

                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        service.servicesOffered.split(",").forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = DarkSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "✓ ${tag.trim()}",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Reviews & Ratings Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Avis & Évaluations", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = "Firestore Live",
                                    tint = ZurikaPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isLiveFromFirestore) "Calculé en direct via le document Firestore" else "Synchronisation Firestore...",
                                    fontSize = 11.sp,
                                    color = if (isLiveFromFirestore) ZurikaPrimary else Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        TextButton(
                            onClick = { showReviewInput = !showReviewInput },
                            modifier = Modifier.testTag("toggle_review_input_button")
                        ) {
                            Icon(
                                imageVector = if (showReviewInput) Icons.Default.Close else Icons.Default.RateReview,
                                contentDescription = "Review",
                                tint = ZurikaPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (showReviewInput) "Annuler" else "Laisser un avis",
                                color = ZurikaPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Star-Rating Summary Card with Big Score & Detailed Star Distribution Bars
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("star_rating_summary_card")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Overall Score Big Text & Star Rating Row
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .padding(end = 18.dp)
                                    .testTag("firestore_rating_score_column")
                            ) {
                                Text(
                                    text = "%.1f".format(effectiveRating),
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                StarRatingBar(
                                    rating = effectiveRating,
                                    starSize = 15.dp,
                                    starSpacing = 2.dp,
                                    activeColor = ZurikaAmber
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "$effectiveReviewCount avis vérifiés",
                                    fontSize = 11.sp,
                                    color = Color.LightGray,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Star Rating Distribution Progress Bars (5★ to 1★)
                            Column(modifier = Modifier.weight(1f)) {
                                val total = if (reviews.isNotEmpty()) reviews.size else 1
                                val counts = (5 downTo 1).map { level ->
                                    val count = if (reviews.isNotEmpty()) {
                                        reviews.count { it.rating.toInt() == level }
                                    } else {
                                        // Normalized representative distribution matching initial rating
                                        if (level == effectiveRating.toInt()) 1 else 0
                                    }
                                    level to count
                                }
                                counts.forEach { (level, count) ->
                                    val fraction = (count.toFloat() / total).coerceIn(0f, 1f)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 1.5.dp)
                                    ) {
                                        Text(
                                            text = "${level}★",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.Gray,
                                            modifier = Modifier.width(22.dp)
                                        )
                                        LinearProgressIndicator(
                                            progress = { fraction },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(7.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            color = ZurikaAmber,
                                            trackColor = DarkSurfaceVariant,
                                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "$count",
                                            fontSize = 10.sp,
                                            color = Color.Gray,
                                            modifier = Modifier.width(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (showReviewInput) {
                        var selectedReviewTags = remember { mutableStateListOf<String>() }

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = DarkSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, ZurikaPrimary),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Votre avis sur ce professionnel :",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Interactive 1-5 Star Selector
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    InteractiveStarRatingSelector(
                                        rating = reviewRating,
                                        onRatingChange = { reviewRating = it },
                                        starSize = 34.dp
                                    )

                                    RatingAppraisalBadge(rating = reviewRating)
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Quick feedback tags
                                QuickFeedbackTagsSelector(
                                    selectedTags = selectedReviewTags.toSet(),
                                    onTagToggle = { tag ->
                                        if (selectedReviewTags.contains(tag)) {
                                            selectedReviewTags.remove(tag)
                                        } else {
                                            selectedReviewTags.add(tag)
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = reviewComment,
                                    onValueChange = { reviewComment = it },
                                    placeholder = { Text("Laissez un commentaire sur l'intervention ou la prestation...", color = Color.Gray, fontSize = 13.sp) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("review_comment_input"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = DarkSurface,
                                        unfocusedContainerColor = DarkSurface,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                FirestoreSubDocumentIndicator(serviceId = service.id)

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = { showReviewInput = false }
                                    ) {
                                        Text(text = "Annuler", color = Color.Gray)
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Button(
                                        onClick = {
                                            if (reviewComment.isNotBlank() || selectedReviewTags.isNotEmpty()) {
                                                val fullComment = if (selectedReviewTags.isNotEmpty() && reviewComment.isNotBlank()) {
                                                    "${selectedReviewTags.joinToString(" • ")}\n$reviewComment"
                                                } else if (selectedReviewTags.isNotEmpty()) {
                                                    selectedReviewTags.joinToString(" • ")
                                                } else {
                                                    reviewComment
                                                }
                                                onAddReview(reviewRating, fullComment)
                                                reviewComment = ""
                                                selectedReviewTags.clear()
                                                showReviewInput = false
                                                Toast.makeText(context, "Avis 1-5 étoiles enregistré sur Firestore !", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ZurikaPrimary),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("submit_review_button")
                                    ) {
                                        Icon(imageVector = Icons.Default.Send, contentDescription = "Submit", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = "Publier l'avis", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Reviews List
                    if (reviews.isEmpty()) {
                        Text(text = "Aucun avis pour le moment. Soyez le premier à donner votre avis !", color = Color.Gray, fontSize = 13.sp)
                    } else {
                        reviews.forEach { rev ->
                            ReviewItemRow(review = rev)
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Report Inaccurate Information Button Card
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = DarkSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, ZurikaAmber.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("report_error_section_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReportProblem,
                                    contentDescription = "Signaler",
                                    tint = ZurikaAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Une information vous semble incorrecte ?",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Numéro non attribué, adresse imprécise, tarifs ou horaires modifiés... Aidez la communauté en nous le signalant.",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { showReportDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ZurikaAmber),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ZurikaAmber),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("open_report_error_button")
                            ) {
                                Icon(imageVector = Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Signaler une erreur", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // Report Error Dialog to Firestore
        if (showReportDialog) {
            val errorOptions = listOf(
                "Numéro de téléphone erroné / injoignable",
                "Adresse ou position GPS incorrecte",
                "Établissement fermé définitivement",
                "Horaires d'ouverture inexacts",
                "Tarifs / Services non conformes",
                "Autre problème"
            )

            AlertDialog(
                onDismissRequest = {
                    if (!isSubmittingReport) showReportDialog = false
                },
                shape = RoundedCornerShape(20.dp),
                containerColor = DarkSurface,
                icon = {
                    Surface(
                        shape = CircleShape,
                        color = ZurikaAmber.copy(alpha = 0.2f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ReportProblem,
                                contentDescription = "Signaler",
                                tint = ZurikaAmber,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                },
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Signaler une erreur",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = service.name,
                            fontSize = 12.sp,
                            color = ZurikaPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Sélectionnez le type d'erreur constatée :",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )

                        // Error Types Quick Select
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            errorOptions.take(4).forEach { option ->
                                val isSelected = reportErrorType == option
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) ZurikaPrimary.copy(alpha = 0.2f) else DarkSurfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) ZurikaPrimary else Color.White.copy(alpha = 0.1f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { reportErrorType = option }
                                        .testTag("report_option_${option.take(10)}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { reportErrorType = option },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = ZurikaPrimary,
                                                unselectedColor = Color.Gray
                                            ),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = option,
                                            fontSize = 12.sp,
                                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.75f),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Précisions ou informations correctes :",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )

                        OutlinedTextField(
                            value = reportDescription,
                            onValueChange = { reportDescription = it },
                            placeholder = {
                                Text(
                                    "Ex: Le bon numéro est le +228 91 23 45 67, fermé le dimanche...",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            },
                            minLines = 3,
                            maxLines = 5,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("report_description_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkSurfaceVariant,
                                unfocusedContainerColor = DarkSurfaceVariant,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = ZurikaPrimary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                            )
                        )

                        // Firestore Live Sync Badge in Dialog
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ZurikaPrimary.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = ZurikaPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Le signalement sera transmis directement sur Firestore (collection service_reports)",
                                    fontSize = 10.sp,
                                    color = ZurikaPrimary
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isSubmittingReport = true
                            onSubmitReport(reportErrorType, reportDescription) { success ->
                                isSubmittingReport = false
                                showReportDialog = false
                                reportDescription = ""
                                Toast.makeText(
                                    context,
                                    if (success) "Merci ! Votre signalement a été transmis à l'équipe." else "Signalement enregistré.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        enabled = !isSubmittingReport,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ZurikaAmber,
                            contentColor = DarkBackground
                        ),
                        modifier = Modifier.testTag("confirm_submit_report_button")
                    ) {
                        if (isSubmittingReport) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = DarkBackground
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        } else {
                            Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = if (isSubmittingReport) "Envoi..." else "Envoyer le signalement",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showReportDialog = false },
                        enabled = !isSubmittingReport,
                        modifier = Modifier.testTag("cancel_report_button")
                    ) {
                        Text("Annuler", color = Color.Gray)
                    }
                }
            )
        }
    }
}

/**
 * Reusable StarRatingBar supporting fractional ratings with full, half, and outline stars.
 */
@Composable
fun StarRatingBar(
    rating: Float,
    maxStars: Int = 5,
    starSize: androidx.compose.ui.unit.Dp = 16.dp,
    starSpacing: androidx.compose.ui.unit.Dp = 3.dp,
    activeColor: Color = ZurikaAmber,
    inactiveColor: Color = Color.Gray.copy(alpha = 0.4f),
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(starSpacing),
        modifier = modifier
    ) {
        for (i in 1..maxStars) {
            val fillAmount = (rating - (i - 1)).coerceIn(0f, 1f)
            when {
                fillAmount >= 0.75f -> {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Star $i",
                        tint = activeColor,
                        modifier = Modifier.size(starSize)
                    )
                }
                fillAmount >= 0.25f -> {
                    Icon(
                        imageVector = Icons.Default.StarHalf,
                        contentDescription = "Half star $i",
                        tint = activeColor,
                        modifier = Modifier.size(starSize)
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.StarBorder,
                        contentDescription = "Empty star $i",
                        tint = inactiveColor,
                        modifier = Modifier.size(starSize)
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = ZurikaPrimary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(text = label, fontSize = 11.sp, color = Color.Gray)
                Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White)
            }
        }
    }
}

@Composable
private fun ReviewItemRow(review: ReviewEntity) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = review.authorName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StarRatingBar(
                        rating = review.rating,
                        maxStars = 5,
                        starSize = 13.dp,
                        starSpacing = 1.5.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "%.1f".format(review.rating),
                        fontWeight = FontWeight.Bold,
                        color = ZurikaAmber,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(text = review.comment, fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f), lineHeight = 18.sp)

            Spacer(modifier = Modifier.height(6.dp))

            Text(text = review.date, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@Composable
fun ServiceDetailsSheet(
    service: ServiceEntity,
    reviews: List<ReviewEntity> = emptyList(),
    userLat: Double = 6.1375,
    userLng: Double = 1.2125,
    onToggleFavorite: (Boolean) -> Unit = {},
    onRequestIntervention: () -> Unit = {},
    onAddReview: (Float, String) -> Unit = { _, _ -> },
    onSubmitReport: (String, String, (Boolean) -> Unit) -> Unit = { _, _, cb -> cb(true) },
    onDismiss: () -> Unit = {}
) {
    ServiceDetailSheet(
        service = service,
        reviews = reviews,
        userLat = userLat,
        userLng = userLng,
        onToggleFavorite = onToggleFavorite,
        onRequestIntervention = onRequestIntervention,
        onAddReview = onAddReview,
        onSubmitReport = onSubmitReport,
        onDismiss = onDismiss
    )
}
